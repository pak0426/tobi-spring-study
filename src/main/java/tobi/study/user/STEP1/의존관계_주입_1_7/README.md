# 1.7 의존관계 주입(DI)

## 1.7.1 제어의 역전(IoC)과 의존관계 주입

IoC는 소프트웨어에서 일반적인 개념이다. DaoFactory처럼 객체를 생성하고 관계를 맺어주는 등의 작업을 담당하는 기능을 일반화한 것이 스프링의 IoC 컨테이너다.

IoC라는 용어는 매우 폭넓게 사용되는 용어이다. 때문에 스프링을 IoC 컨테이너라고만 해서는 스프링이 제공하는 기능의 특징을 명확하게 설명하지 못한다.

- 스프링이 서블릿 컨테이너처럼 서버에서 동작하는 서비스 컨테이너라는 듯인지,
- 아니면 단순히 IoC 개념이 적용된 템플릿 메서드 패턴을 이용해 만들어진 프레임워크인지,
- 아니면 또 다른 IoC 특징을 지닌 기술이라는 것인지 파악하기 힘들다.

그래서 새로운 용어로 IoC 방식을 핵심을 짚어주는 **의존 관계 주입 (Dependency Injection)** 이라는, 좀 더 의도가 명확히 드러나는 이름을 사용하기 시작했다. 스프링의 IoC 기능의 대표적인
동작원리는 주로 **의존관계 주입**이라고 불린다.

## 1.7.2 런타임 의존관계 설정

### 의존관계

먼저 의존관계란?

두 개의 클래스 또는 모듈이 의존관계에 있다고 말할 때는 항상 방향성을 부여해줘야 한다. 즉 누가 누구에게 의존하는 관계에 있다는 식이어야 한다. UML 모델에서는 두 클래스의 의존관계를 점선으로 된 화살표로
표현한다.

아래 그림은 A가 B에 의존하고 있음을 나타낸다.

<img width="357" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/258b272e-4990-4fae-829f-a2a01dcca371">

의존하고 있다는건 무슨 의미일까? 의존한다는 건 의존대상, 위 그림에서는 B가 변하면 그것이 A에 영향을 미친다는 듯이다. B의 기능이 추가되거나 변경되거나, 형식이 바뀌거나 하면 그 영향이 A로 전달된다는 것이다.

예를 들어 A에서 B에 정의된 메서드를 호출해서 사용하는 경우이다. 이럴 땐 `사용에 대한 의존관계`가 있다고 말할 수 있다.

- 만약 B에 새로운 메서드가 추가되거나 기존의 메서드의 형식이 바뀌면 A도 그에 따라 수정되거나 추가돼야 할 것이다.
- 또는 B의 형식은 그대로지만 기능이 내부적으로 변경되면, 결과적으로 A의 기능이 수행되는 데도 영향을 미칠 수 있다.

다시 말하면 의존성에는 방향성이 있다. A가 B에 의존하고 있지만, 반대로 B는 A에 의존하지 않는다. 의존하지 않는다는 말은 B는 A의 변화에 영향을 받지 않는다는 뜻이다.

### UserDao의 의존관계

UserDao는 ConnectionMaker에 의존하고 있는 형태다. 그림 1-11에서 UserDao는 ConnectionMaker 인터페이스에 의존하고 있다. 따라서 ConnectionMaker 인터페이스가
변한다면 그 영향을 UserDao가 직접적으로 받게 된다. 하지만 ConnectionMaker 인터페이스를 구현한 클래스, 즉 DConnectionMaker 등이 다른 것으로 바뀌거나 그 내부에서 사용하는 메서드에
변화가 생겨도 UserDao에 영향을 주지 않는다.

이렇게 인터페이스에 대해서만 의존관계를 만들어두면 인터페이스 구현 클래스와의 관계는 느슨해지면서 변화에 영향을 덜 받는 상태가 된다. 결합도가 낮다고 설명할 수 있다.

<img width="325" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/600e982b-f4f3-474c-b209-edc540eccd7a">

위 그림에서 UserDao는 DconnectionMaker라는 클래스의 존재도 알지 못한다. UML에서 말하는 의존관계란 이렇게 설계 모델의 관점에서 말하는 것이다. 그런데 모델이나 코드에서 클래스와 인터페이스를
통해 드러나는 의존관계 말고, 런타임 시에 오브젝트 사이에서 만들어지는 의존관계도 있다. 런타임 의존관계 또는 오브젝트 의존관계라 하는데, 설계 시점의 의존관계가 실체화된 것이라고 볼 수 있다.

인터페이스를 통해 설계 싲머에 느슨한 의존관계를 갖는 경우에는 UserDao의 오브젝트가 런타임 시에 사용할 오브젝트가 어떤 클래스로 만든 것인지 미리 알 수 없다.

프로그램이 시작되고 UserDao 오브젝트가 만들어지고 나서 런타임 시에 의존관계를 맺는 대상, 즉 실제 사용대상인 오브젝트를 **의존 오브젝트**라고 말한다.

**의존관계 주입은 이렇게 구체적인 의존 오브젝트와 그것을 사용할 주체, 보통 클라이언트라고 부르는 오브젝트를 런타임 시에 연결해주는 작업을 말한다.**

정리하면 의존관계 주입이란 다음 세 가지 조건을 충족해야 한다.

1. 클래스 모델이나 코드에는 런타임 시점의 의존관계가 드러나지 않는다. 그러기 위해서는 인터페이스에만 의존하고 있어야 한다.
2. 런타임 시점의 의존관계는 컨테이너나 팩토리 같은 제 3의 존재가 결정한다.
3. 의존관계는 사용할 오브젝트에 대한 레퍼런스를 외부에서 제공(주입)해줌으로써 만들어진다.

의존관계 주입의 핵심은 설계 시점에는 알지 못했던 두 오브젝트의 관계를 맺도록 도와주는 제 3의 존재가 있다는 것이다. DI에서 말하는 제3의 존재는 바로 관계설정 책임을 가진 코드를 분리해서 만들어진 오브젝트라고
볼 수 있다. 전략패턴에서 등장한 클라이언트나 앞에서 만들었던 DaoFactory, 또 DaoFactory와 같은 작업을 일반화해서 만들어졌다는 스프링의 애플리케이션 컨텍스트, 빈 팩토리, IoC 컨테이너 등이 모두
외부에서 오브젝트 사이의 런타임 관계를 맺어주는 책임을 지닌 제 3의 존재라고 볼 수 있다.

### UserDao의 의존관계 주입

UserDao에 적용된 의존관계 주입 기술을 다시 살펴보자.

UserDao엔 마지막으로 남은 문제가 있었는데 UserDao가 사용할 구체적인 클래스를 알고 있어야 한다는 점이었다. 관계 설정의 채임을 분리하기 전에 UserDao 클래스의 생성자는 아래와 같았다.

```java
public UserDao() {
    connectionMaker = new DConnectionMaker();
}
```

이 코드의 문제는 이미 런타임 시의 의존관계가 코드 속에 미리 결정되어 있다는 점이다. 그래서 IoC 방식을 써서 UserDao로부터 런타임 의존관계를 드러내는 코드를 제거하고, 제3의 존재에 런타임 의존관계 결정
권한을 위임한다. 그래서 최종적으로 만들어진 것이 DaoFactory이다.

DaoFactory는 런타임 시점에 UserDao가 사용할 ConnectionMaker 타입의 오브젝트를 결정하고
이를 생성한 후에 UserDao의 생성자 파라미터로 주입해서 UserDao가 DConnectionMaker의 오브젝트와 런타임 의존관계를 맺게 해준다. 따라서 의존관계 주입의 세 가지 조건을 모두 충족한다고 볼 수
있고, 이미 DaoFactory를 만든 시점에서 의존관계 주입을 이용한 셈이다.

아래 그림과 같이 UserDao의 의존관계는 ConnectionMaker 인터페이스 뿐이다. 이것은 클래스 모델의 의존관계이므로 코드에 반영되고, 런타임 시점에서도 변경되지 않는다.

<img width="353" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/ef7f26a1-1d35-4f25-a70f-5b6d944e99e3">

<img width="621" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/dce1abd6-fa06-48b6-b758-f96e7db4a28f">

위 코드와 같이 해서 두 개의 오브젝트 간에 런타임 의존관계가 만들어졌다. 이렇게 DI 컨테이너에 의해 런타임 시에 의존 오브젝트를 사용할 수 있도록 그 레퍼런스를 전달받는 과정이 마치 메서드를 통해 DI 컨테이너가
UserDao에게 주입해주는 것과 가타고 해서 이를 의존관계 주입이라고 부른다.

<img width="453" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/b4a2d057-2ee0-456e-822b-8c773ae468b4">

DI는 자신이 사용할 오브젝트에 대한 선택과 생성 제어권을 외부로 넘기고 자신은 수동적으로 주입받은 오브젝트를 사용한다는 점에서 IoC의 개념에 잘 들어맞는다. 스프링 컨테이너의 IoC는 주로 의존관계 주입 또는
DI라는 데 초점이 맞춰져 있다.

## 1.7.3 의존관계 검색과 주입

스프링이 제공하는 IoC 방법에는 의존관계 주입만 있는 것이 아니다. 코드에서는 구체적인 클래스에 의존하지 않고, 런타임 시에 의존관계를 결정한다는 점에서 의존관계 주입과 비슷하지만, 의존관계를 맺는 방법이
외부로부터의 주입이 아니라 스스로 검색을 이용하기 때문에 `의존관계 검색`이라고 불리는 것도 있다.

```java
public UserDao() {
    DaoFactory daoFactory = new DaoFactory();
    this.connectionMaker = daoFactory.connectionMaker();
}
```

이렇게 해도 UserDao는 여전히 자신이 어떤 ConnectionMaker 오브젝트를 사용할지 미리 알지 못한다. 여전히 코드의 의존대상은 ConnectionMaker 인터페이스뿐이다. 런타임 시에
DaoFactory가 만들어서 돌려주는 오브젝트와 다이내믹하게 런타임 의존관계를 맺는다.

하지만 적용 방법은 외부로부터의 주입이 아니라 스스로 IoC 컨테이너인 DaoFactory에게 요청하는 것이다. DaoFactory의 경우라면 미리 준비된 메서드를 호출하면 되니까 단순 요청으로 보이겠지만, 이런
작업을 일반화한 스프링의 애플리케이션 컨텍스트라면 미리 정해놓은 이름을 전달해서 그 이름에 해당하는 오브젝트를 찾게 된다. 따라서 이를 일종의 검색이라고 볼 수 있다. 또한 그 대상이 런타임 의존관계를 가질
오브젝트이므로 의존관계 검색이라고 부르는 것이다.

스프링 IoC 컨테이너인 애플리케이션 컨텍스트는 getBean()이라는 메서드를 제공한다. 바로 이 메서드가 의존관계 검색에 사용되는 것이다.

```java
public UserDao() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    this.connectionMaker = context.getBean("connectionMaker", ConnectionMaker.class);
}
```

의존관계 검색은 기존 의존관계 주입의 거의 모든 장점을 갖고 있다. IoC 원칙에도 잘 들어맞는다. 단 방법만 조금 다를 뿐이다.

## 1.7.4 의존관계 주입의 응용

런타임 시에 사용 의존관계를 맺을 오브젝트를 주입해준다는 DI 기술의 장점이 무엇일까?

### 기능 구현의 교환

실제 운영에 사용할 데이터베이스는 매우 중요한 자원이다. 평상시에도 항상 부하를 많이 받고 있어서 개발 중에는 절대 사용하지 말아야 한다. 대신 개발 중에는 개발자 PC에 설치한 로컬 DB로 사용해야 한다고 해보자.
그리고 개발이 진행되다가 어느 시점이 되면 지금까지 개발한 것을 그대로 운영서버로 배치해서 사용할 것이다.

그런데 만약 DI 방식을 적용하지 않았다고 해보자. 개발 중에는 로컬 DB를 사용하도록 해야 하니 로컬 DB에 대한 연결 기능이 있는 LocalDBConnectionMaker라는 클래스를 만들고, 모든 DAO에서 이
클래스의 오브젝트를 매번 생성해서 사용하게 했을 것이다.

반면 DI 방식을 적용해서 만들었다고 해보자. 모든 DAO는 생성 시점에 ConnectionMaker 타입의 오브젝트를 컨테이너로부터 제공받는다. 구체적인 사용 클래스 이름은 컨테이너가 사용할 설정정보에 들어있다.
@Configuration이 붙은 DaoFactory를 사용한다고 하면 개발자 PC에서는 DaoFactory의 아래와 같이 만들어서 사용하면 된다.

```java

@Bean
public ConnectionMaker connectionMaker() {
    return new LocalDBConnectionMaker();
} 
```

### 부가기능 추가

만약 DAO가 DB를 얼마나 많이 연결해서 사용하는지 파악하고 싶다고 해보자. DB 연결횟수를 카운팅하기 위해 무식한 방법으로, 모든 DAO의 makeConnection() 메서드를 호출하는 부분에 소루 추가한
카운터를 증가하는 코드를 넣어야 할까?

DI 컨테이너에서라면 아주 간단한 방법으로 가능하다. DAO와 DB커넥션을 만드는 오브젝트 사이에 연결횟수를 카운팅하는 오브젝트를 하나 더 추가하는 것이다. 먼저 CountingConnectionMaker라는
클래스를 만든다. 중요한 것은 ConnectionMaker 인터페이스를 구현해서 만든다는 점이다. DAO가 의존할 대상이 될 것이기 때문이다.

```java
public class CountingConnectionMaker implements ConnectionMaker {
    int counter = 0;
    private ConnectionMaker realConnectionMaker;

    public CountingConnectionMaker(ConnectionMaker realConnectionMaker) {
        this.realConnectionMaker = realConnectionMaker;
    }

    @Override
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        this.counter++;
        return realConnectionMaker.makeNewConnection();
    }
}
```

CountingConnectionMaker 클래스는 ConnectionMaker 인터페이스를 구현했지만 내부에서 직접 DB 커넥션을 만들지 않는다. 대신 DAO가 DB 커넥션을 가져올 때마다 호출하는
makeConnection()에서 DB 연결횟수 카운터를 증가시킨다. CountingConnectionMaker는 자신의 관심사인 DB 연결횟수 카운팅 작업을 마치면 실제 DB 커넥션을 만들어주는
realConnectionMaker에 저장된 ConnectionMaker 타입 오브젝트의 makeConnection()을 호출해서 그 결과를 DAO에게 돌려준다. 그래야만 DAO가 DB 커넥션을 사용해서 정상적으로
동작할 수 있다.

아래 그림은 CountingConnectionMaker를 사용하기 전의 런타임 의존관계이다.

<img width="523" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/57b128fa-e82b-4ba9-b784-70a961a03b19">

UserDao는 ConnectionMaker의 인터페이스에만 의존하고 있기 때문에, ConnectionMaker 인터페이스를 구현하고 있다면 어떤 것이든 DI가 가능하다. 그래서 UserDao 오브젝트가 DI 받는
대상의 설정을 조정해서 DConnection 오브젝트 대신 CountingConnectionMaker 오브젝트로 바꿔치기 하는 것이다.

그렇다고 해서 DB 커넥션을 제공해주지 않으면 DAO가 동작하지 않을 테니 CountingConnectionMaker가 다시 실제 사용할 DB 커넥션을 제공해주는 DConnectionMaker를 호출하도록 만들어야
한다. 역시 DI를 사용하면 된다.

<img width="552" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/96602bac-6a39-43ca-a829-0a3d8db9fe3b">

새로운 의존관계를 컨테이너가 사용할 설정정보를 이용해 만들어보자. CountingDaoFactory라는 이름의 설정용 클래스를 만든다.
기존 DaoFactory와 달리,

1. connectionMaker() 메서드에서 CountingConnectionMaker 타입 오브젝트를 생성하도록 만든다.
2. 그리고 실제 DB 커넥션을 만들어주는 DConnectionMaker는 이름이 realConnectionMaker()인 메서드에서 생성하게 한다.
3. 그리고 realConnectionMaker() 메서드가 만들어주는 오브젝트는 connectionMaker()에서 만드는 오브젝트 생성자를 통해 DI 해준다.

코드를 살펴보자.

```java

@Configuration
public class CountingDaoFactory {

    @Bean
    public UserDao userDao() {
        return new UserDao(connectionMaker());
    }

    private ConnectionMaker connectionMaker() {
        return new CountingConnectionMaker(realConnectionMaker());
    }

    private ConnectionMaker realConnectionMaker() {
        return new DConnectionMaker();
    }
}
```

이제 커넥션 카운팅을 위한 실행 코드를 만든다. 기본적으로 UserDaoTest와 같지만 설정용 클래스를 CountingDaoFactory로 변경해줘야 한다. DAO를 DL 방식으로 가져와 어떤 작업이든 여러 번
실행시킨다. 그리고 CountingConnectionMaker 빈을 가져온다. 설정정보에 지정된 이름과 타입만 알면 특정 빈을 가져올 수 있으니 CountingConnectionMaker 오브젝트를 가져오는 건
간단하다.

```java
public class UserDaoConnectionCountingTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(CountingDaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);

        DaoFactory daoFactory = new DaoFactory();
        UserDao userDao1 = daoFactory.getUserDao();
        UserDao userDao2 = daoFactory.getUserDao();

        System.out.println("userDao1 = " + userDao1);
        System.out.println("userDao2 = " + userDao2);

        UserDao userDao3 = context.getBean("userDao", UserDao.class);
        UserDao userDao4 = context.getBean("userDao", UserDao.class);

        System.out.println("userDao3 = " + userDao3);
        System.out.println("userDao4 = " + userDao4);

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        userDao.add(user);
        System.out.println(user.getId() + " 등록 성공");

        User user2 = userDao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");

        CountingConnectionMaker ccm = context.getBean(CountingConnectionMaker.class);
        System.out.println("Connection counter : = " + ccm.getCounter());
    }
}
```

지금은 DAO가 하나뿐이지만 DAO가 수십, 수백 개여도 상관 없다. DI의 장점은 관심사의 분리를 통해 얻어지는 높은 응집도에서 나온다. 또한 CountingConnectionMaker를 이용한 분석 작업이 모두
끝나면, 다시 CountingDaoFactory 설정 클래스를 DaoFactory로 변경하거나 connectionMaker() 메서드를 수정하는 것만을heh DAO의 런타임 의존관계는 이전 상태로 복구된다.

## 1.7.5 메서드를 이용한 의존관계 주입

지금까지 UserDao의 의존관계 주입을 위해 상성자를 사용했다. 생성자에 파라미터를 만들어두고 이를 통해 DI 컨테이너가 의존할 오브젝트 레퍼런스를 넘겨주도록 만들었다. 그런데 의존관계 주입 시 반드시 생성자를
사용해야 하는 것은 아니다. 생성자가 아닌 일반 메서드를 사용할 수도 있을 뿐만 아니라, 생성자를 사용하는 방법보다 더 자주 사용된다.

### 수정자 메서드를 이용한 주입

수정자 (setter) 메서드는 외부에서 오브젝트 내부의 속성 값을 변경하려는 용도로 주로 사용된다. 메서드는 항상 set으로 시작된다. 간단히 수정자라고 불린다.

수정자 메서드의 핵심기능은 파라미터로 전달된 값을 보통 내부의 인스턴스 변수에 저장하는 것이다. 부가적으로, 입력 값에 대한 검증이나 그 밖의 작업을 수행할 수도 있다. 수정자 메서드는 외부로부터 제공받은 오브젝트
레퍼런스를 저장해뒀다가 내부의 메서드에서 사용하게 하는 DI 방식에서 활용하기에 적당하다.

### 일반 메서드를 이용한 주입

수정자 메서드처럼 set으로 시작해야하고 한 번에 한 개의 파라미터만 가질 수 있다는 제약이 싫다면 여러 개의 파라미터를 갖는 일반 메서드를 DI용으로 사용할 수도 있다. 생성자가 수정자 메서드보다 나은 점은 한번에
여러 개의 파라미터를 받을 수 있다는 점이다. 하지만 파라미터의 개수가 많아지고 비슷한 타입이 여러 개라면 실수하기 쉽다. 임의의 초기화 메서드를 이용하는 DI는 적절한 개수의 파라미터를 가진 여러 개의 초기화
메서드를 만들 수도 있기 때문에 한 번에 모든 필요한 파라미터를 다 받아야 하는 생성자보다 낫다.

UserDao를 수정자 메서드를 이용해 DI 하도록 만들어 보자.

```java
class UserDao {
    private ConnectionMaker connectionMaker;

    public void setConnectionMaker(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
    }

    // ...
}
```

UserDao의 수정자 메서드 DI 방식이 가능하도록 변경했으니 DI를 적용하는 DaoFactory의 코드도 수정해보자.

```java

@Bean
public UserDao userDao() {
    UserDao userDao = new UserDao();
    userDao.setConnectionMaker(connectionMaker());
    return userDao;
}
```
단지 의존관계를 주입하는 시점과 방법이 달라졌을 뿐 결과는 동일하다. 실제로 스프링은 생성자, 수정자 메서드, 초기화 메서드를 이용한 방법 외에도 다양한 의존관계 주입 방법을 지원한다.