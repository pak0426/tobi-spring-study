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