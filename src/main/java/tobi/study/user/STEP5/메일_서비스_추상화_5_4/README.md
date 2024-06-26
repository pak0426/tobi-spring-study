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

### 5.4.3 테스트를 위한 서비스 추상화

#### JavaMail을 이용한 테스트의 문제점

JavaMail의 핵심 API에는 DataSource처럼 인터페이스로 만들어져서 구현을 바꿀 수 있는게 없다.

메일 발송을 위해 가장 먼저 생성해야 하는 javax.mail.Session 클래스의 사용방법을 살펴보자.

```java
Session s = Session.getInstance(props, null);
```

하지만 이 Session은 인터페이스가 아니고 클래스다. 생성자 모두 private 으로 되어 있어 직접 생성도 불가능하다. JavaMail 처럼 테스트하기 힘든 구조인 API를 테스트하기 좋게 만드는 방법이 있다. 트랜잭션을 적용하면서 살펴봤던 서비스 추상화를 적용하면 된다. 스프링은 JavaMail을 사용해 만든 코드는 손쉽게 테스트하기 힘들다는 문제를 해결하기 위해서 JavaMail에 대한 추상화 기능을 제공하고 있다. 아래는 스프링이 제공하는 메일 서비스 추상화의 핵심 인터페이스다.

```java
public interface MailSender {
    void send(SimpleMailMessage simpleMessae) throws MailException;
    void send(SimpleMailMessage[] simpleMessages) throws MailException;
} 
```

이 인터페이스는 SimpleMailMessage라는 인터페이스를 구현한 클래스에 담긴 메일 메시지를 전송하는 메서드로만 구성되어 있다. 기본적으로 JavaMail을 사용해 메일 발송 기능을 제공하는 JavaMailSenderImpl 클래스를 이용하면 된다. 아래는 스프링이 제공하는 JavaMailSender 구현 클래스를 사용해서 만든 메일 발송용 코드다.

```java
    private void sendUpgradeEmail(User user) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("mail.server.com");

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("useradmin@ksug.org");
        mailMessage.setSubject("upgrade 안내");
        mailMessage.setText("사용자님의 등급이 " + user.getLevel().name());

        mailSender.send(mailMessage);
    }
```

- 지저분한 try/catch 블록이 사라졌다.
- JavaMailSender 인터페이스를 구현한 JavaMailSenderImpl의 오브젝트를 만들어서 사용했다.

하지만 이 코드는 테스트용 오브젝트를 대체할 수 없다. JavaMailSenderImpl 클래스를 코드에서 직접 사용하기 때문이다. 따라서 스프링의 DI를 적용해보자.

```java
public class UserService {
    // ...
    
    private MailSender mailSender;
    
    // ...

    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    // ...

    private void sendUpgradeEmail(User user) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("mail.server.com");

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("useradmin@ksug.org");
        mailMessage.setSubject("upgrade 안내");
        mailMessage.setText("사용자님의 등급이 " + user.getLevel().name());

        this.mailSender.send(mailMessage);
    }
}
```

```java
@Bean
public JavaMailSenderImpl mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("mail.server.com");
    return mailSender;
}

@Bean
public UserService userService() {
    UserService userService = new UserService();
    userService.setUserDao(userDao());
    userService.setTransactionManager(new DataSourceTransactionManager(dataSource()));
    userService.setMailSender(mailSender());
    return userService;
}
```

이제 테스트를 실행하면 JavaMail API를 직접 사용했을 때와 동일하게 지정된 메일 서버로 메일이 발송된다. 우리가 원하는 건 JavaMail을 사용하지 않고, 메일 발송 기능이 포함된 코드를 테스트하는 것이다. 이를 위해 메일 전송 기능을 추상화해서 인터페이스를 적용하고 DI를 통해 빈으로 분리해놨으니 모든 준비가 끝났다. 스프링이 제공한 메일 전송 기능에 대한 인터페이스가 있으니 메일 전송 클래스를 만들어 보자. 

```java
public class DummyMailSender implements MailSender {
    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        
    }
}
```

DummyMailSender 는 MailSender 인터페이스를 구현했을 뿐, 하는 일이 없다. 다음은 테스트 설정파일의 mailSender 빈 클래스를 다음과 같이 DummyMailSender로 변경한다.

```java
    @Bean
    public DummyMailSender mailSender() {
        DummyMailSender mailSender = new DummyMailSender();
        return mailSender;
    }
```

```java
@SpringBootTest
class UserServiceTest {
    @Autowired
    private MailSender mailSender;
    
    // ...

    @Test
    public void upgradeAllOrNoting() {
        // ...
        testUserService.setMailSender(mailSender);
        
        // ...
    }
}
```

이렇게 하고 UserServiceTest 테스트는 모두 성공으로 끝난다.


#### 테스트와 서비스 추상화

아래 그림은 스프링이 제공하는 MailSender 인터페이스를 핵심으로 하는 메인 전송 서비스 추상화의 구조다.

<img width="507" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/68b7e26d-bf18-48dc-af91-0e1ed441abd3">

스프링이 직접 제공하는 MailSender를 구현한 추상화 클래스는 JavaMailServiceImpl 하나 뿐이다.

추상화를 이용하니까 UserService와 같은 애플리케이션 계층의 코드는 아래 계층에서는 어떤 일이 일어나는지 상관 없이 메일 발송을 요청한다는 기본 기능에 충실하게 작성하면 된다. 메일 서버가 바뀌고 메일 발송 방식이 바뀌는 등의 변화가 있어도 메일을 발송한다는 비즈니스 로직이 바뀌지 않는 한 UserService는 수정할 필요가 없다.

하지만 이 코드에서 부족한 한 가지가 있다. 바로 메일 발송 작업에 트랜잭션 개념이 빠져있다는 사실이다. 레벨 업그레이드 작업 중간에 예외가 발생해서 DB에 반영했던 레벨 업그레이드는 모두 롤백 됐다고 하자. 하지만 메일은 사용자별로 업그레이드 할때 발송한다. 어떻게 취소할 것인가?

이런 문제를 해결하려면 2가지 방법이 있다.

1. 메일을 업그레이드할 사용자를 발견했을 때마다 발송하지 않고 발송 대상을 별도의 목록에 저장해두는 것이다. 그리고 업그레이드 작업이 모두 성공적으로 끝났을 때 한번에 전송하면 된다.
2. MailSender를 확장해 메일 전송에 트랜잭션 개념을 적용하는 것이다.

JavaMail처럼 확장이 불가능하게 설계해놓은 API를 사용해야 하는 경우라면 추상화 계층의 도입을 적극 고려해볼 필요가 있다. 특별히 외부의 리소스와 연동하는 대부분 작업은 추상화의 대상이 될 수 있다.

### 5.4.4 테스트 대역

DummyMailSender 클래스는 아무것도 하는 일이 없다. 하지만 이 클래스를 이용해 JavaMail로 메일을 직접 발송하는 클래스를 대치하지 않았으면 테스트는 매우 불편해지고 자주 실행하기 힘들었을 것이다.

스프링의 설정파일을 테스트용으로 만든 이유는 개발자 환경에서 쉽게 이용할 수 있는 테스트용 DB를 사용하도록 만들기 위해서다. 이처럼 테스트 환경에서 유용하게 사용하는 기법이 있다. 대부분 테스트할 대상이 의존하고 있는 오브젝트를 DI를 통해 바꿔치기 하는 것이다.

#### 의존 오브젝트의 변경을 통한 테스트 방법

아래 그림은 테스트가 진행될 때의 상황을 나타낸다.

<img width="452" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/ff3fc502-ea92-41ba-97d2-9ff0608b58ac">

아래 그림은 테스트 중에 메일 전송 기능을 이용하는 구조를 나타낸 것이다.

<img width="459" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/da9e96d6-d5cc-4680-894c-17da0589d5c8">

위 두 가지 경우에서 볼 수 있듯, 테스트 대상이 되는 오브젝트가 또 다른 오브젝트에 의존하는 일은 매우 흔하다.

스프링의 DI는 실전에서 사용할 오브젝트를 고체하지 않더라도, 단지 테스트만을 위해서도 유용하다. 운영 중인 시스템에서 DataSource 외에는 절대 다른 것을 사용하지 않는다고 100% 확신하더라도, 테스트 때는 바꿀 수 밖에 없기 때문이다. 그래서 DataSource 라는 인터페이스를 사용하고, 어떤 클래스의 오브젝트를 사용할지 외부에서 주입하도록 스프링의 DI를 적용해야 한다.

#### 테스트 대역의 종류와 특징

테스트용으로 사용되는 특별한 오브젝트들이 있다. 대부분 테스트 대상인 오브젝트의 의존 오브젝트가 되는 것들이다.  
이렇게 테스트 환경을 만들어주기 위해, 테스트 대상이 되는 오브젝트의 기능에만 충실하게 수행하면서 빠르게, 자주 테스트를 실행할 수 있도록 사용하는 이런 오브젝트를 통틀어서 **테스트 대역(Test Double)**이라고 부른다.

대표저인 테스트 대역은 **테스트 스텁(Test Stub)**이다. 테스트 스텁은 테스트 대상 오브젝트의 의존객체로서 존재하면서 테스트 동안에 코드가 정상적으로 수행할 수 있도록 돕는 것을 말한다. 일반적으로 테스트 스텁은 코드 내부에서 간접적으로 사용된다. 따라서 DI 등을 통해 미리 의존 오브젝트를 테스트 스텁으로 변경해야 한다.  
어떤 경우는 스텁이 결과를 돌려줘야 할 때도 있다. 이럴 댄 스텁에 미리 테스트 중에 필요한 정보를 리턴해주도록 만들 수 있다. 또는 어떤 스텁은 메서드를 호출하면 강제로 예외를 발생시키게 해서 테스트 대상 오브젝트가 예외상황에서 어떻게 반응할지를 테스트할 때 적용할 수 있다.

테스트 대상 오브젝트의 메서드가 돌려주는 결과뿐 아니라 테스트 오브젝트가 간접적으로 의존 오브젝트에 넘기는 값과 그 행위 자체에 대해서도 검증하고 싶다면 어떻게 해야할까?

이런 경우에는 테스트의 간접적인 출력 결과를 검증하고, 테스트 대상 오브젝트와 의존 오브젝트 사이에서 일어나는 일을 검증할 수 있도록 설계된 **목 오브젝트(Mock Object)**를 사용해야 한다.

<img width="446" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/acbcc4bd-bd28-4719-a002-3d915358ff7b">

위 그림에서 (5)번을 제외하면 스텁이라고 봐도 된다.

테스트 대상 오브젝트는 테스트로부터만 입력을 받는 것이 아니라는 점이다. 테스트가 수행되는 동안 실행되는 코드는 테스트 대상이 의존하고 있는 다른 의존 오브젝트와도 커뮤니케이션하기도 한다. 테스트 대상은 의존 오브젝트에게 값을 출력하기도 하고 값을 입력받기도 한다. 이를 위해 별도로 준비해둔 스텁 오브젝트가 메소드 호출 시 특정 값을 리턴하도록 만들어두면 된다.  
때론 테스트 대상 오브젝트가 의존 오브젝트에게 출력한 값에 관심이 있을 경우가 있다. 또는 의존 오브젝트를 얼마나 사용했는가 하는 커뮤니케이션 행위 자체에 관심이 있을 수가 있다. 문제는 이 정보는 테스트에서는 직접 알 수가 없다는 점이다. 이때는 테스트 대상과 의존 오브젝트 사이에 주고받는 정보를 보존해두는 기능을 가진 의존 오브젝트인 목 오브젝트를 만들어서 사용해야 한다. 테스트 대상 오브젝트의 메서드 호출이 끝나고 나면 테스트는 목 오브젝트에게 테스트 대상과 목 오브젝트 사이에서 일어났던 일에 대해 확인을 요청해서, 그것을 테스트 검증 자료로 삼을 수 있다.

#### 목 오브젝트를 이용한 테스트

DummyMailSender 대신에 새로운 MailSender 를 대체할 클래스를 하나 만들자. 물론 메일을 발송하는 기능은 없다. UserServiceTest의 스태틱 멤버 클래스로 정의하겠다.

```java
    static class MockMailSender implements MailSender {
        // UserService 로부터 전송 요청을 받은 메일 주소를 저장해두고 이를 읽을 수 있게 한다.
        private List<String> requests = new ArrayList<String>();
    
        public List<String> getRequests() {
            return requests;
        }
    
        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage simpleMessage : simpleMessages) {
                requests.add(simpleMessage.getTo()[0]);
            }
        }
    }
```

실제 메일을 발송하는 기능은 없다. 대신 이 클래스는 테스트 대상인 UserService가 send() 메서드를 통해 자신을 불러서 메일 전송 요청을 보냈을 때 관련 정보를 저장해두는 기능이 있다.

이제 MockMailSender를 이용해 아래와 같이 upgradeLevels() 테스트 코드를 수정해서 목 오브젝트를 통해 메일 발송 여부를 검증하도록 만들 수 있다.

```java
    @Test
    @DirtiesContext // 컨텍스트의 DI 설정을 변경하라는 테스트라는 것을 알려준다.
    public void upgradeLevels() throws SQLException {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        // 메일 발송 결과를 테스트할 수 있도록 목 오브젝트를 만들어 userService 의 의존 오브젝트로 주입해준다.
        MockMailSender mockMailSender = new MockMailSender();
        userService.setMailSender(mockMailSender);


        // 업그레이드 테스트. 메일 발송이 일어나면 MockMailSender 오브젝트의 리스트에 그 결과가 저장된다.
        userService.upgradeLevels();
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);

        
        // 목 오브젝트에 저장된 메일 수신자 목록을 가져와 업그레이드 대상과 일치하는지 확인 
        List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }
```

테스트 대상인 UserService의 메서드를 호출하기 앞서 스프링 설정을 통해 DI된 DummyMailSender를 대신해서 사용할 목 오브젝트를 수정자를 이용해 수동 DI 해준다. 그러면 UserService에서 사용하는 mailSender 는 목 오브젝트가 주입되어 우리가 설계한 대로 메일 수신자 정보를 저장할 것이다.

테스트를 수행하고 결과를 확인하면 모두 성공일 것이다. 목 오브젝트 테스트라는 게, 작성하기는 간단하면서도 기능은 상당히 막강하다는 것을 알 수 있다. 보통의 테스트 방법으로는 검증하기가 매우 까다로운 테스트 대상 오브젝트의 내부에서 일어나는 일이나 다른 오브젝트 사이에서 주고받는 정보까지 검증하는 일이 손쉽기 때문이다.

테스트가 수행될 수 있도록 의존 오브젝트에 간접적으로 입력 값을 제공해주는 스텁 오브젝트와 간접적이 출력 값까지 확인이 가능한 목 오브젝트, 이 두 가지는 테스트 대역의 가장 대표적인 방법이며 효과적인 테스트 코드를 작성하는 데 빠질 수 없는 중요한 도구이다.