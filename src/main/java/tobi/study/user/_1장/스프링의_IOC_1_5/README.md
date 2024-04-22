# 1.5 스프링의 IoC

스프링의 핵심을 담당하는 건, 바로 빈 팩토리 또는 애플리케이션 컨텍스트라고 불리는 것이다. 이 두가지는 우리가 만든 DaoFactory가 하는일을 좀 더 일반화한 것이라고 설명할 수 있다.

## 1.5.1 오브젝트 팩토리를 이용한 스프링 IoC

### 애플리케이션 컨텍스트와 설정정보

이제 DaoFactory를 스프링에서 사용이 가능하도록 변신시켜보자. 스프링에서는 스프링이 제어권을 가지고 직접 만들고 관계를 부여하는 오브젝트를 **빈**이라고 부른다.

자바빈 또는 엔터프라이즈 자바빈에서 말하는 빈과 비슷한 오브젝트 단위의 애플리케이션 컴포넌트를 말한다. 동시에 스프링 빈은 스프링 컨테이너가 생성과 관계설정, 사용 등을 제어해주는 제어의 역전이 적용된 오브젝트를 가리키는 말이다.

스프링에서는 빈의 생성과 관계 설정 같은 제어를 담당하는 IoC 오브젝트를 **빈 팩토리**라고 부른다. 

애플리케이션 컨텍스트는 IoC 방식을 따라 만들어진 일종의 빈 팩토리라고 보면 된다. 그리고 별도의 정보를 참고해서 빈의 생성, 관계설정 등의 제어 작업을 총괄한다.

### DaoFactory를 사용하는 애플리케이션 컨텍스트

DaoFactory를 스프링의 빈 팩토리가 사용할 수 있는 본격적인 설정정보로 만들어보자. 먼저 스프링이 빈 팩토리를 위해 오브젝트 설정을 담당하는 클래스라고 인식할 수 있도록 @Configuration 이라는 어노테이션을 추가해준다.

그리고 오브젝트를 만둘어주는 메소드에는 @Bean 이라는 어노테이션을 붙여준다. userDao() 메서드는 userDao 타입 오브젝트를 생성하고 초기화해서 돌려주는 것이니 당연히 @Bean 을 붙여준다. 또한 ConnectionMaker 타입 오브젝트를 생성해주는 connectionMaker() 메서드에도 @Bean 을 붙여준다.

```java
@Configuration
class DaoFactory {
    @Bean
    public UserDao userDao() {
        return new UserDao(connectionMaker());
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }
}
```
이제 DaoFactory 를 설정정보로 사용하는 애플리케이션 컨텍스트를 만들어보자. 애플리케이션 컨텍스트는 ApplicationContext 타입의 오브젝트다. ApplicationContext를 구현한 클래스는 여러 가지가 있는데 DaoFactory처럼 @Configuration이 붙은 자바 코드를 설정정보로 사용하려면 AnnotationConfigApplicationContext 를 이용하면 된다. 이제 이렇게 준비된 ApplicationContext 의 getBean()이라는 메서드를 이용해서 UserDao의 오브젝트를 가져올 수 있다.

```java
class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);
        
        // ...
    }
}
```

getBean() 메서드는 ApplicationContext 가 관리하는 오브젝트를 요청하는 메서드다. getBean()의 파라미터인 "userDao"는 ApplicationContext 에 등록된 빈의 이름이다. DaoFactory에서 @Bean 이라고 애노테이션을 붙였는데, 이 메서드 이름이 바로 빈의 이름이 된다.

메서드 이름을 myPreciousUserDao() 라고 했다면 getBean("myPreciousUserDao", UserDao.class)로 가져올 수 있다.

## 1.5.2 애플리케이션 컨텍스트의 동작 방식

기존에 오브젝트 팩토리를 이용했던 방식과 스프링의 애플리케이션 컨텍스트를 사용한 방식을 비교해보자.

오브젝트 팩토리에 대응되는 것이 스프링의 애플리케이션 컨텍스트다.
스프링에서는 애플리케이션 컨텍스트를 IoC 컨테이너, 스프링 컨테이너, 빈 컨테이너라고 부를 수도 있다.

애플리케이션 컨텍스트는 <br> 
ApplicationContext 인터페이스를 구현 -> ApplicationContext는 BeanFactory를 상속 <br>
했으므로 빈 팩토리인 셈이다.

DaoFactory는 UserDao를 비롯한 DAO 오브젝ㅌ를 생성, DB 생성 오브젝트와 관계를 맺어주는 제한적인 역할을 하는 데 반해, 

애플리케이션 컨텍스트는 애플리케이션에서 IoC를 적용해 관리할 모든 오브젝트에 대한 생성과 관계 설정을 담당한다.

@Configuration이 붙은 DaoFactory는 이 애플리케이션 컨텍스트가 활용하는 IoC 설정정보이다. 내부적으로는 이 애플리케이션 컨텍스트가 DaoFactory의 userDao() 메서드를 호출해서 오브젝트를 가져온 것을 클라이언트가 getBean()으로 요청할 때 전달해준다.

<img width="515" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/73cbdbfd-75cc-4600-b0ca-b918d7a34d07">

1. 애플리케이션 컨텍스트는 DaoFactory 클래스를 설정정보로 등록해두고 @Bean이 붙은 메소드의 이름을 가져와 Bean 목록을 만들어 둔다.
2. 클라이언트가 getBean()을 호출하면 Bean 목록에서 있는지 찾고, 있으면 Bean을 생성하는 메서드를 호출해 오브젝트를 생성 후 클라이언트에게 돌려준다.

애플리케이션 컨텍스트를 사용하는 이유는 범용적이고 유연한 방법으로 IoC 기능을 확장하기 위해서이다.
애플리케이션 컨텍스트을 사용했을때 얻을 수 있는 장점은 다음과 같다.

### 클라이언트는 구체적인 팩토리 클래스를 알 필요가 없다.

애플리케이션이 발전하면 DaoFactory 처럼 IoC를 적용한 오브젝트들이 추가될 것이다. 클라이언트가 필요한 오브젝트를 가져오려면 **어떤 팩토리 클래스를 사용해야 할지 알아야 하고, 필요할 때마다 팩토리 오브젝트를 생성해야 한다.**

애플리케이션 컨텍스트를 사용하면 오브젝트 팩토리가 아무리 많아져도 이를 알거나 직접 사용할 필요가 없다.

### 애플리케이션 컨텍스트는 종합 IoC 서비스를 제공해준다.

애플리케이션 컨텍스트의 역할은 단지 오브젝트 생성과 다른 오브젝트와의 관계 설정만이 전부가 아니다.

오브젝트가 만들어지는 방식, 시점, 전략을 다르게 가져갈 수 있고 자동생성, 오브젝트에 대한 후처리, 정보의 조합 설정 방식의 다변화, 인터셉팅 등 오브젝트를 효과적으로 활용할 수 있는 다양한 기능을 제공한다.
또 빈이 사용할 수 있는 기반기술 서비스나 외부 시스템과의 연동 등을 컨테이너 차원에서 제공해준다.

### 애플리케이션 컨텍스트는 빈을 검색하는 다양한 방법을 제공한다.

애플리케이션 컨텍스트의 getBean() 메서드는 빈의 이름을 이용해 빈을 찾아준다. 타입만으로 빈을 검색하거나 특별한 어노테이션 설정이 되어 있는 빈을 찾을 수도 있다. 

