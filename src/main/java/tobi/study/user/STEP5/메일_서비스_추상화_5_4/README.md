## 5.4 메일 서비스 추상화

새로운 요청 사항
> 레벨이 업그레이드 되는 사용자에게는 안내 메일을 발송

이를 위해 해야 할 일 두 가지가 있다.

1. 사용자의 이메일 정보 관리 - email 필드 추가
2. 업그레이드 작업은 담은 로직에 메일 발송 기능 추가

### 5.4.1 JavaMail을 이용한 메일 발송 기능

사용자 정보에 이메일을 추가하는 일은 레벨을 추가했을 때와 동일하게 진행하면 된다.

- User 테이블에 email 필드 추가
- UserDao 에 email 필드 처리 코드 추가
- User 에 email 필드 및 생성자 코드 추가

테스트가 모두 성공적이면 다음 단계로 넘어가자.

#### JavaMail 메일 발송

자바에서 메일을 발송할 때는 표준 기술인 JavaMail 을 사용하면 된다. 코드를 작성해보자.

```yaml
implementation 'javax.mail:javax.mail-api:1.6.2'
implementation 'com.sun.mail:javax.mail:1.6.2'
```

```java
    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeEmail(user);
    }
    
    private void sendUpgradeEmail(User user) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.ksug.org");
        Session s = Session.getInstance(props);

        MimeMessage message = new MimeMessage(s);
        try {
            message.setFrom(new InternetAddress("useradmin@ksug.org"));
            message.addRecipients(Message.RecipientType.TO, String.valueOf(new InternetAddress(user.getEmail())));
            message.setSubject("Upgrade 안내");
            message.setText("사용자님의 등급이 " + user.getLevel().name() + "로 업그레이드되었습니다.");

            Transport.send(message);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
```

전형적인 코드이고 SMTP 프로토콜을 지원하는 메일 전송 서버가 준비되어 있다면, 이 코드는 정상적으로 동작할 것이고 안내 메일이 발송될 것이다.

### 5.4.2 JavaMail이 포함된 코드의 테스트

그런데 메일 서버가 준비되어 있지 않다면 어떻게 될 것인가? 테스트를 실행했는데 메일 서버가 없다면 예외가 발생하면서 테스트가 실패할 것이다.

<img width="553" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/297eb227-6dd5-4429-a462-9b110a760656">

테스트 실패의 원인은 분명하다. 그런데 과연 테스트를 하면서 매번 매일이 발송되는 것이 바람직할까? 메일 발송이란 매우 부하가 큰 작업이다. 그것도 실제 운영 중인 메일 서버를 통해 테스트를 실행할 때마다 매일을 보내면 메일 서버에 상당한 부담을 줄 수 있다.

게다가 실제 메일이 발송된다는 문제도 있다. 메일 발송 기능은 사용자 레벨 업그레이드 작업의 보조적인 기능에 불과하다.

실제 업그레이드가 일어나거나, DB에 잘 반영되는 것보다 중요하지 않다. 메일 발송 테스트는 엄밀히 말해서 불가능하다. 메일이 정말 잘 도착했는지 확인하지 못한다면 의미가 없다.

메일 서버는 충분히 테스트된 시스템이다.  SMTP로 메일 전송 요청을 받으면 별문제 없이 메일이 잘 전송됐다고 믿어도 충분하다. 따라서 JavaMail을 통해 메일 서버까지만 메일이 잘 전달됐다면, 결국 사용자도에게도 메일이 잘 보내졌을 것이라고 생각할 수 있다.

아래 그림은 실제 메일 서버를 사용하지 않고 테스트 메일을 이용해 테스트 하는 방법을 보여준다.

<img width="539" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/1b1e8b51-8c81-47a4-ba12-395e3e15fd98">

SMTP 라는 표준 메일 발송 프로토콜로 메일 서버에 요청이 전달되기만 하면 메일이 발송될 것이라고 믿고, 실제 메일 서버가 아닌 테스트용으로 따로 준비한 메일 서버를 사용해 테스트를 수행해도 좋다면, 그 똑같은 원리를 UserService 와 JavaMail 사이에도 적용할 수 있지 않을까?

JavaMail은 자바의 표준 기술이고 이미 수많은 시스템에 사용돼서  검증된 안정적인 모듈이다. 따라서 JavaMail API를 통해 요청이 들어간다는 보장만 있다면 굳이 테스트를 할 때마다 JavaMail 을 직접 구동시킬 필요가 없다. 게다가 JavaMail이 동작하면 외부의 메일 서버와 네트워크로 연동하고 전송하는 부하가 큰 작업이 일어나기 때문에 이를 생략할 수 있다면 좋다.

<img width="479" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/fc748827-c49d-4b18-8444-569c59391d56">

