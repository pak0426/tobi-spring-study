# 1.6 싱글톤 레지스트리와 오브젝트 스코프

DaoFactory를 직접 사용하는 것과 @Configuration 어노테이션을 추가해서 스프링 애플리케이션 컨텍스트를 통해 사용하는 것은 테스트 결과만 보면 동일한 것 같다. 하지만 그 둘은 중요한 차이점이 있다. 먼저 DaoFactory의 userDao() 메서드를 두 번 호출해서 리턴되는 UserDao 오브젝트를 비교해보자.

이 두 개는 같은 오브젝트일까?

```java
DaoFactory daoFactory = new DaoFactory();
UserDao userDao1 = daoFactory.getUserDao();
UserDao userDao2 = daoFactory.getUserDao();

System.out.println("userDao1 = " + userDao1);
System.out.println("userDao2 = " + userDao2);

//userDao1 = tobi.study.user.싱글톤_레지스트리와_오브젝트_스코프_1_6.UserDao@6a03bcb1
//userDao2 = tobi.study.user.싱글톤_레지스트리와_오브젝트_스코프_1_6.UserDao@21b2e768
```

출력 결과에서 알 수 있듯이, 두 개는 각기 다른 값을 가진 동일하지 않은 오브젝트다. 즉 오브젝트가 2개가 생긴걸 알 수 있다.

하지만 getBean()을 통해 가져온 오브젝트를 보면 이전 결과와 다르다.

```java
UserDao userDao3 = context.getBean("userDao", UserDao.class);
UserDao userDao4 = context.getBean("userDao", UserDao.class);

System.out.println("userDao3 = " + userDao3);
System.out.println("userDao4 = " + userDao4);

//userDao3 = tobi.study.user.싱글톤_레지스트리와_오브젝트_스코프_1_6.UserDao@57250572
//userDao4 = tobi.study.user.싱글톤_레지스트리와_오브젝트_스코프_1_6.UserDao@57250572
```

두 오브젝트의 출력 값이 같으므로, 두 오브젝트가 동일하다는 걸 알 수 있다. <br> 
확실히 하려면 `dao3 == dao4`를 출력해보면 된다.

우리가 만들었던 오브젝트 팩토리와 스프링의 애플리케이션 컨텍스트에는 동작방식에서 무엇인가 차이가 있다. 스프링은 여러 번에 걸쳐 같은 빈을 요청하더라도 매번 동일한 오브젝트를 돌려준다.

## 1.6.1 싱글톤 레지스트리로서의 애플리케이션 컨텍스트

애플리케이션 컨텍스트는 오브젝트 팩토리와 비슷한 방식으로 동작하는 IoC 컨테이너이다. 그러면서 동시에 싱글톤을 저장하고 관리하는 싱글톤 레지스트리이기도 하다.


### 서버 애플리케이션과 싱글톤

왜 스프링은 싱글톤으로 빈을 만드는 것일까? 이는 스프링이 주료 적용되는 대상이 자바 엔터프라이즈 기술을 사용하는 서버환경이기 때문이다. 

스프링이 처음 설계됐던 대규모 엔터프라이즈 서버환경은 서버 하나당 최대로 초당 수십에서 수백 번씩 브라우저나 여타 시스템으로부터의 요청을 받아 처리할 수 있는 높은 성능이 요구되는 환경이었다. 

그런데 매번 클라이언트에서 요청이 올 때마다 각 로직을 담당하는 오브젝트를 새로 만들어서 사용한다고 생각해보자. 요청 한 번에 5개의 오브젝트가 만들어지고 초당 500개의 요청이 들어오면, 초당 2500개의 오브젝트가 생성된다. 이렇게 부하가 걸리면 서버가 감당하기 힘들다.

그래서 엔터프라이즈 분야에서는 서비스 오브젝트라는 개념을 일찍부터 사용해왔다. 서블릿은 자바 엔터프라이즈 기술의 가장 기본이 되는 서비스 오브젝트라고 할 수 있다. 서블릿은 댑분 멀티스레드 환경에서 싱글톤으로 동작한다.

서블릿 클래스당 하나의 오브젝트만 만들어주고, 사용자의 요청을 담당하는 여러 스레드에서 하나의 오브젝트를 공유해 동시에 사용한다.

### 싱글톤 패턴의 한계

자바에서 싱글톤을 구현하는 방법은 보통 이렇다.


- 클래스 밖에서는 오브젝트를 생성하지 못하도록 생성자를 private으로 만든다.
- 생성된 싱글톤 오브젝트를 저장할 수 있는 자신과 같은 타입의 스태틱 필드를 정의한다.
- 스태틱 팩토리 메소드인 getInstance()를 만들고 이 메소드가 최초로 호출되는 시점에서 한 번만 오브젝트가 만들어지게 한다. 생성된 오브젝트는 스태틱 필드에 저장된다. 또는 스태틱 필드의 초기값으로 오브젝트를 미리 만들어둘 수도 있다.
- 한번 오브젝트(싱글톤)가 만들어지고 난 후에는 getInstance() 메소드를 통해 이미 만들어져 스태틱 필드에 저장해둔 오브젝트를 넘겨준다.

UserDao를 싱글톤 패턴을 이용해 만든다면 다음과 같다.

```java
private static UserDao instance;
    
private ConnectionMaker connectionMaker;

public UserDao(ConnectionMaker connectionMaker) {
    this.connectionMaker = connectionMaker;
}

public static synchronized UserDao getInstance() {
    if (instance == null) instance = new UserDao(???);
    return instance;
}
```

이제 생성자는 private 이기 때문에 외부에서 호출할 수 없고 따라서 DaoFactory에서 UserDao를 생성하며 ConnectionMaker를 넣어주는 것이 불가능해졌다.

일반적으로 싱글톤 패턴 구현 방식에는 다음과 같은 문제가 있다.

#### private 생성자를 갖고 있기 때문에 상속할 수 없다.

private 생성자를 가진 클래스는 다른 생성자가 없다면 상속이 불가능하다. 객체지향의 장점인 상속과 이를 이용한 다형성을 적용할 수 없다.

기술적인 서비스만 제공하는 경우라면 상관없지만, 애플리케이션의 로직을 담고 있는 일반 오브젝트의 경우 싱글톤으로 만들었을 때 객체지향적인 설계의 장점을 적용하기 어렵다는 점은 심각한 문제다.

또한 상속, 다형성 같은 객체지향 특징이 적용되지 않는 static 필드와, 메서드를 사용하는 것도 역시 동일한 문제를 발생시킨다.

#### 싱글톤은 테스트하기 힘들다.
싱글톤은 테스트가 아예 불가능하다. 싱글톤은 만들어지는 방식이 제한적이기 때문에 테스트에서 사용될 때 목 오브젝트 등으로 대체하기가 힘들다. 

싱글톤은 초기화 과정에서 생성자 등을 통해 사용할 오브젝트를 동적으로 주입하기도 힘들기 때문에 필요한 오브젝트는 직접 만들어 사용할 수 밖에 없다. 이런 경우 테스트용 오브젝트로 대체하기가 힘들다.

#### 서버환경에서는 싱글톤이 하나만 만들어지는 것을 보장하지 못한다.
서버에서 클래스 로더를 어떻게 구성하고 있느냐에 따라 싱글톤 클래스임에도 하나 이상의 오브젝트가 만들어질 수 있다. 따라서 자바 언어를 이용한 싱글톤 패턴 기법은 서버환경에서 싱글톤이 꼭 보장된다고 할 수 없다.

여러 개의 JVM에 분산돼서 설치가 되는 경우에도 각각 독립적으로 오브젝트가 생기기 때문에 싱글톤으로서의 가치가 떨어진다.

#### 싱글톤 사용은 전역 상태를 만들 수 있기 때문에 바람직하지 못한다.

싱글톤은 사용하는 클라이언트가 정해져 있지 않다. static 메서드를 이용해 언제든지 접근할 수 있고, 그러다 보면 자연스럽게 전역 상태로 사용되기 쉽다.

아무 객체나 자유롭게 접근하고 수정하고 공유할 수 있는 전역 상태를 갖는 것은 객체지향 프로그래밍에서는 권장되지 않는 프로그래밍 모델이다.

---

### 싱글톤 레지스트리 <br>

스프링은 서버환경에서 싱글톤이 만들어져서 서비스 오브젝트 방식으로 사용되는 것은 지지한다. 하지만 자바의 기본적인 싱글톤 패턴의 구현 방식은 여러 가지 단점이 있기 때문에, 스프링은 직접 싱글톤 형태의 오브젝트를 만들고 관리하는 기능을 제공한다.

그것이 바로 **싱글톤 레지스트리**다.

스프링 컨테이너는 싱글톤을 생성하고, 관리하고, 공급하는 싱글톤 관리 컨테이너이기도 하다. 싱글톤 레지스트리의 장점은 스태틱 메서드와 private 생성자를 사용해야 하는 비정상적인 클래스가 아닐 ㅏ평범한 자바 클래스를 싱글톤으로 활용하게 해준다는 점이다. 평범한 자바 클래스라도 IoC 방식의 컨테이너를 사용해서 생성과 관계설정, 사용 등에 대한 제어권을 컨테이너에게 넘기면 손쉽게 싱글톤 방식으로 만들어져 관리되게 할 수 있다. 오브젝트 생성에 관한 모든 권한은 IoC 기능을 제공하는 애플리케이션 컨텍스트에게 있기 때문이다.

가장 중요한 것은 싱글톤 패턴과 달리 스프링이 지지하는 객체지향적인 설계 방식과 원칙, 디자인 패턴 등을 적용하는 데 아무런 제약이 없다는 점이다.

## 1.6.2 싱글톤과 오브젝트의 상태

싱글톤은 멀티스레드 환경이라면 여러 스레드가 동시에 접근해서 사용할 수 있다. 따라서 상태 관리에 주의를 기울여야 한다. 기본적으로 싱글톤이 멀티스레드 환경에서 서비스 형태의 오브젝트로 사용되는 경우에는 상태정보를 내부에 갖고 있지 안은 무상태 방식으로 만들어져야 한다. 다중 사용자의 요청을 한꺼번에 처리하는 스레드들이 동시에 싱글톤 오브젝트의 인스턴스 변수를 수정하는 것은 매우 위험하다. 저장할 공간이 하나뿐이니 서로 값을 덮어쓰고 자신이 저장하지 않은 값을 읽어올 수 있기 때문이다. 따라서 싱글톤은 기본적으로 인스턴스 필드의 값을 변경하고 유지하는 상태유지 방식으로 만들지 않는다.

상태가 없는 방식으로 클래스를 만드는 경우에 각 요청에 대한 정보나, DB나 서버의 리소스로부터 생성한 정보는 어떻게 다뤄야 할까? 이때는 파라미터와 로컬 변수, 리턴 값 등을 이용하면 된다. 메서드 파라미터나, 메서드 안에서 생성되는 로컬 변수는 매번 새로운 값을 저장할 독립적인 공간이 만들어지기 때문에 싱글톤이라고 해도 여러 쓰레드가 변수의 값을 덮어쓸 일은 없다.

UserDao를 수정한 코드를 살펴보자.

```java
class UserDao {
    private ConnectionMaker connectionMaker;
    private Connection c;
    private User user;

    public void add(User user) throws SQLException, ClassNotFoundException {
        this.c = connectionMaker.makeNewConnection();

        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) values (?, ?, ?)"
        );
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public User get(String id) throws SQLException, ClassNotFoundException {
        this.c = connectionMaker.makeNewConnection();

        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?"
        );
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        this.user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }
}
```

기존에 만들었던 UserDao와 다른 점은 기존에 로컬 변수로 선언하고 사용했던 Connection과 User를 클래스의 인스턴스 필드로 선언했다는 것이다. 따라서 싱글톤으로 만들어져서 멀티스레드 환경에서 사용하면 위에서 설명한 대로 심각한 문제가 발생한다. 따라서 스프링의 싱글톤 빈으로 사용되는 클래스를 만들 때는 기존의 UserDao 처럼 개별적으로 바뀌는 정보는 로컬 변수로 정의하거나, 파라미터로 주고받으면서 사용하게 해야한다.

그런데 기존의 UserDao에서도 인스턴스 변수로 정의해서 사용한 것이 있다. 바로 ConnectionMaker 인터페이스 타입의 connectionMaker다. 이것은 인스턴스 변수를 사용해도 상관 없다. 왜냐하면 connectionMaker는 읽기전용의 정보이기 때문이다.

## 1.6.3 스프링의 빈 스코프

스프링이 관리하는 오브젝트, 즉 빈이 생성되고, 존재하고, 적용되는 범위에 대해 알아보자. 스프링에서는 이것을 빈의 스코프라고 한다. 스프링 빈의 기본 스코프는 싱글톤이다. 싱글톤 스코프는 스프링 컨테이너가 존재하는 동안 계속 유지된다.

경우에 따라서는 싱글톤 외의 스코프를 가질 수 있다. 대표적으로 프로토타입 스코프가 있다. 프로토타입은 싱글톤과 달리 컨테이너에 빈을 요청할 때마다 매번 새로운 오브젝트를 만들어준다. 그 외에도 웹을 통해 새로운 HTTP 요청이 생길 때마다 생성되는 요청 스코프가 있고, 웹의 세션과 스코프가 유사한 세션 스코프도 있다. 스프링에서 만들어지는 빈의 스코프는 싱글톤 외에도 다양한 스코프를 사용할 수 있다.