## 6.5 스프링 AOP

지금까지 해왔던 작업의 목표는 비즈니스 로직에 **반복적으로 등장해야만 했던 트랜잭션 코드를 깔끔하고 효과적으로 분리해내는 것**이다.  이렇게 분리해낸 트랜잭션 코드는 부가기능 형태로 제공돼야 한다. 즉, **부가기능 적용 후에도 기존 설계와 코드에 영향을 주지 않는다는 뜻**이다.

### 6.5.1 자동 프록시 생성

프록시 팩토리 빈 방식의 접근 방법의 한계라고 생각했던 2가지 문제가 있었다. 그 중에서 부가기능이 타깃 오브젝트마다 새로 만들어지는 문제는 스프링의 `ProxyFactoryBean` 의 어드바이스를 통해 해결됐다.

남은 것은 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 `ProxyFactoryBean` 빈 설정정보를 추가해주는 부분이다. 새로운 타깃이 등장했다고 해서 코드를 손댈 필요는 없지만, 설정은 매번 수정해줘야 한다.

이런 문제를 제거할 방법은 없을까?

#### 중복 문제의 접근 방법

지금까지 다뤘던 기계적인 코드에 대한 해결책을 생각해보자.

먼저 JDBC API를 사용하는 DAO 코드가 있었다. 메서드마다 JDBC try/catch/finally 블록으로 구성된 비슷한 코드가 반복해서 나타났다. 이 코드는 바뀌지 않는 부분과 바뀌는 부분을 구분해서 분리하고, 템플릿과 콜백, 클라이언트로 나누는 방법을 통해 해결했다. 전략 패턴과 DI를 적용한 덕분이다.

그런데 이와 다른 방법으로 해결했던 것이 반복적인 위임 코드가 필요한 프록시 클래스 코드다. 이는 단순한 분리와 DI와는 다르게 동적 프록시라는 런타임 코드 자동생성 기법을 이용한 것이다. JDK의 동적 프록시는 특정 인터페이스를 구현한 오브젝트에 대해서 프록시 역할을 해주는 클래스를 **런타임 시** 내부적으로 만들어준다. (런타임 시에 만들어져 사용되기 때문에 클래스 소스가 따로 남지 않을 뿐이지 타깃 인터페이스의 모든 메서드를 구현하는 클래스가 분명히 만들어진다.)

변하지 않는 타깃으로의 위임과 부가기능 적용 여부 판단이라는 부분은 코드 생성 기법을 이용하는 동적 프록시 기술에 맡기고, 변하는 부가기능 코드는 별도로 만들어서 동적 프록시 생성 팩토리에 DI로 제공하는 방법을 사용한 것이다.

#### 빈 후처리기를 이용한 자동 프록시 생성기

스프링은 OCP(개방폐쇄 원칙)의 가장 중요한 요소인 유연한 확장의 개념을 스프링 컨테이너 자신에게도 다양한 방법으로 적용하고 있다. 스프링 DI를 이용해 만들어지는 애플리케이션 코드가 OCP에 충실할 수 있다면 스프링 스스로도 그런 가치를 따르는 게 마땅하다. 그래서 스프링은 컨테이너로서 제공하는 기능 중에서 변하지 않는 핵심 부분 외에는 대부분 확장할 수 있도록 확장 포인트를 제공해준다.

그 중에서도 `BeanPostProcessor` 인터페이스를 구현해서 만드는 빈 후처리기다. 빈 후처리기는 스프링 빈 오브젝트로 만들어지고 난 후에, 빈 오브젝트를 다시 가공할 수 있게 해준다.

여기서는 스프링이 제공하는 빈 후처리기 중 하나인 `DefaultAdvisorAutoProxyCreator` 를 살펴보는데 이는 어드바이저를 이용한 자동 프록시 생성기다. 빈 후처리기를 스프링에 적용하는 방법은 빈 후처리기 자체를 빈으로 등록하는 것이다. 스프링은 빈 후처리기가 빈으로 등록되어 있으면 빈 오브젝트가 생성될 때마다 빈 후처리기에 보내서 후처리 작업을 요청한다. 빈 후처리기는 빈 오브젝트의 프로퍼티를 강제로 수정하거나 별도의 초기화 작업도 수행할 수 있다. 심지어는 만들어진 빈 오브젝트 자체를 바꿔치기할 수도 있다. 따라서 스프링이 설정을 참고해서 만든 오브젝트가 아닌 다른 오브젝트를 빈으로 등록시키는 것이 가능하다.

아래는 빈 후처리기를 이용한 자동 프록시 생성 방법을 설명한다.

- `DefaultAdvisorAutoProxyCreator` 빈 후처리가 등록되어 있으면 스프링은 빈 오브젝트를 만들 때마다 후처리기에게 빈을 보낸다. 
- `DefaultAdvisorAutoProxyCreator`는 빈으로 등록된 모든 어드바이저 내에 포인트컷을 이용해 빈이 프록시 적용 대상인지 확인한다.
- 프록시 적용 대상이면 그때는 내장된 프록시 생성기에게 현재 빈에 대한 프록시를 만들게 하고, 만들어진 프록시에 어드바이저를 연결해준다. 
- 빈 후처리기는 프록시가 생성되면 원래 컨테이너가 전달해준 빈 오브젝트 대신 프록시 오브젝트를 컨테이너에 돌려준다.
- 컨테이너는 최종적으로 빈 후처리기가 돌려준 오브젝트를 빈으로 등록하고 사용한다.


<img width="621" alt="image" src="https://github.com/user-attachments/assets/d0cf070b-f963-4a3e-a6a8-ca82e0100924">

적용할 빈을 선정하는 로직이 추가된 포인트컷이 담긴 어드바이저를 등록하고 빈 후처리기를 사용하면 일일이 `ProxyFactoryBean` 빈을 등록하지 않아도 타깃 오브젝트에 자동으로 프록시가 적용되게 할 수 있다. 마지막 남은 번거로운 `ProxyFactoryBean` 설정 문제를 말끔하게 해결해주는 방법이다.

#### 확장된 포인트컷

그런데 한 가지 이상한 점이 있다. 지금까지 포인트컷이란 타깃 오브젝트의 메서드 중에서 어떤 메서드에 부가기능을 적용할지를 선정해주는 역할을 한다고 했다. 그런데 여기서는 갑자기 포인트컷이 등록된 빈 중에서 어떤 빈에 프록시를 적용할지를 선택한다는 식으로 설명하고 있다. 어떻게 된 일일까? 포인트컷은 오브젝트 내의 메서드를 선택하는 것이 아니고 빈 오브젝트 자체를 선택하는 기능을 가졌다는 뜻일까?

포인트 컷은 두 가지 기능을 모두 가지고 있다. 아래의 `PointCut` 인터페이스를 잘 살펴보면 포인트 컷은 클래스 필터와 메서드 매처 두 가지를 돌려주는 메서드를 갖고 있다.

```java
public interface Pointcut {
    ClassFilter getClassFilter(); // 프록시를 적용할 클래스인지 확인
    MethodMatcher getMethodMatcher(); // 어드바이스를 적용할 메서드인지 확인
}
```

지금가지는 포인트컷이 제공하는 두 가지 기능 중에서 `MethodMatcher` 라는 메서드를 선별하는 기능만 사용한 것이다. 기존에 사용한 `NameMatchMethodPointcut` 은 메서드 선별 기능만 가진 특별한 포인트컷이다. 메서드만 선별한다는 건 클래스 필터는 모든 클래스를 다 받아주도록 만들어져 있다는 뜻이다. 따라서 클래스의 종류는 상관없이 메서드만 판별한다. 어차피 `ProxyFactoryBean` 에서 포인트컷을 사용할 때는 이미 타깃이 정해져 있기 때문에 포인트컷은 메서드 선별만 해주면 그만이었다.

만약 `Pointcut` 선정 기능을 모두 적용한다면 먼저 프록시를 적용할 클래스인지 판단하고, 적용 대상 클래스인 경우 어드바이스를 적용할 메서드인지 확인하는 식으로 동작한다.

`ProxyFactoryBean` 에서는 굳이 클래스 레벨의 필터는 필요 없었지만, 모든 빈에 대해 프록시 자동 적용 대상을 선별해야 하는 빈 후처리기인 `DefaultAdvisorAutoProxyCreator` 클래스와 메서드 선정 알고리즘을 갖고 있는 포인트컷이 필요하다. 정확히는 그런 포인트컷과 어드바이스가 결합되어 있는 어드바이저가 등록되어 있어야 한다.

#### 포인트컷 테스트
포인트컷의 기능을 간단한 학습 테스트로 확인해보자. 앞에서 사용한 `NameMatchMethodPointcut` 은 클래스 필터 기능이 아예 없다고 했다. 사실은 모든 클래스에 대해 무조건 승인하는 필터가 들어있다. 이번엔 이 클래스를 확장해서 클래스도 고를 수 있게 해보자. 그리고 프록시 적용 후보 클래스를 여러 개 만들고 이 포인트컷을 적용한 `ProxyFactoryBean` 으로 프록시를 만들도록 해서 과연 어드바이스가 적용되는지 확인해보자.

```java
// DynamicProxyTest.java

@Test
public void classNamePointcutAdvisor() {
    // 포인트컷 준비
    NameMatchMethodPointcut classMethodPointcut = new NameMatchMethodPointcut() {
        // 익명 내부 클래스 방식으로 클래스 정의
        public ClassFilter getClassFilter() {
            return new ClassFilter() {
                @Override
                public boolean matches(Class<?> clazz) {
                    // 클래스 이름이 HelloT로 시작하는 것만 선정
                    return clazz.getSimpleName().startsWith("HelloT");
                }
            };
        }
    };
    // sayH로 시작하는 메서드 일므을 가진 메서드만 선정
    classMethodPointcut.setMappedName("sayH*");


    // 테스트
    checkAdviced(new HelloTarget(), classMethodPointcut, true);

    class HelloWord extends HelloTarget {};
    checkAdviced(new HelloWord(), classMethodPointcut, false);

    class HelloTom extends HelloTarget {}
    checkAdviced(new HelloTom(), classMethodPointcut, true);
}

private void checkAdviced(Object target, Pointcut pointcut, boolean adviced) {
    ProxyFactoryBean pfBean = new ProxyFactoryBean();
    pfBean.setTarget(target);
    pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
    Hello proxiedHello = (Hello) pfBean.getObject();

    if (adviced) {
        assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
        assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
        assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("Thank you hm");
    }
    else {
        // 어드바이스 적용 대상 후보에서 탈락
        assertThat(proxiedHello.sayHello("hm")).isEqualTo("Hello hm");
        assertThat(proxiedHello.sayHi("hm")).isEqualTo("Hi hm");
        assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("Thank you hm");
    }
}
```

- 포인트컷은 `NameMatchMethodPointcut` 을 내부 익명 클래스 방식으로 확장해서 만들었다.
- 모든 클래스를 받아주는 클래스 필터를 리턴하던 `getClassFilter()` 를 오버라이드해서 이름이 HelloT로 시작하는 클래스만 선정하는 필터로 만들었다.
- 메서드 이름 선정기준은 기존에 사용하던 것을 그대로 유지했다.


##### 테스트는 3가지 클래스에 대해 진행한다.

- HelloTarget, HelloWorld, HelloTom
- 이 세 개의 클래스에 모두 동일한 포인트컷을 적용
- 메서드 선정기준으로는 `sayThankYou()` 메서드를 제외하고 어드바이스가 적용된다.
- `HelloWolrd` 클래스는 클래스 필터에서 탈락된다.

포인트컷이 클래스 필터까지 동작해서 클래스를 걸러버리면 아무리 프록시를 적용하더라도 부가기능은 제공되지 않는다.


### 6.5.2 DefaultAdvisorAutoProxyCreator 의 적용

프록시 자동생성 방식에서 사용할 포인트컷을 만드는 방법을 학습 테스트를 만들어가면서 살펴봤으니, 적용해보자.

#### 클래스 필터를 적용한 포인트컷 작성

만들어야 할 클래스는 하나 뿐이다. 메서드 이름만 비교하던 포인트컷인 `NameMatherMethodPointcut` 을 상속해서 프로퍼티로 주어진 이름 패턴을 가지고 클래스 이름을 비교하는 `ClassFilter` 를 추가하도록 만들 것이다. 학습 테스트에서 만들었던 포인트컷과 유사한 클래스다. 아래 코드는 클래스 필터 기능이 추가된 포인트컷이다.

```java
public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
    public void setMappedClassName(String mappedClassName) {
        this.setClassFilter(new SimpleClassFilter(mappedClassName));
    }



    static class SimpleClassFilter implements ClassFilter {
        String mappedName;

        public SimpleClassFilter(String mappedName) {
            this.mappedName = mappedName;
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
        }
    }
}
```

#### 어드바이저를 이용하는 자동 프록시 생성기 등록

적용할 자동 프록시 생성기인 `DefaultAdvisorAutoProxyCreator` 는 등록된 빈 중 Advisor 인터페이스를 구현한 것을 모두 찾는다. 그리고 생성되는 모든 빈에 대해 어드바이저 포인트컷을 적용해보면서 프록시 적용 대상을 선정한다. 프록시 선정 대상이라면 프록시를 만들어 원래 빈 오브젝트와 바꿔치기한다. 원래 빈 오브젝트는 프록시 뒤에 연결돼서 프록시를 통해서만 접근 가능하게 바뀌는 것이다. 따라서 타깃 빈에 의존한다고 정의한 다른 빈들은 프록시 오브젝트 대신 DI 받게 될 것이다.


#### 포인트컷 등록

아래와 같이 기존의 포인트컷 설정을 삭제하고 새롭게 등록하자.

```java
@Bean
public NameMatchClassMethodPointcut transactionPointcut() {
    NameMatchClassMethodPointcut pointcut = new NameMatchClassMethodPointcut();
    pointcut.setMappedClassName("*ServiceImpl");
    pointcut.setMappedName("upgrade*");
    return pointcut;
}
```

#### 어드바이스 어드바이저

어드바이스인 `transactionAdvice`과 아드바이저인 `transactionAdvisor` 빈의 설정은 수정할 게 없다.

하지만 어드바이저로서 사용되는 방법은 바뀌었다. 이제 `ProxyFactoryBean` 으로 등록한 빈에서처럼 `transactionAdvisor`를 명시적으로 DI하는 빈은 존재하지 않는다. 대신 어드바이저를 이용하는 자동 프록시 생성기인 `DefaultAdvisorProxyCreator` 에 의해 자동 수집되고, 프록시 대상 선정 과정에 참여하며, 자동생성된 프록시에 다이내믹하게 DI 돼서 동작하는 어드바이저가 된다.


#### ProxyFactoryBean 제거와 서비스 빈의 원상복구

프록시를 도입했던 때부터 아이디를 바꾸고 프록시에 DI 돼서 간접적으로 사용돼야 했던 `userServiceImpl` 빈의 아이디를 이제는 `userService`로 되돌릴 수 있다. 더 이상 명시적인 프록시 팩토리 빈을 등록하지 않기 때문이다. `ProxyFactoryBean` 타입의 빈도 삭제시키자.

```java
// 삭제
@Bean
public ProxyFactoryBean userService() {
    ProxyFactoryBean proxy = new ProxyFactoryBean();
    proxy.setTarget(userServiceImpl());
    proxy.addAdvisor(transactionAdvisor());
    return proxy;
}

// 내용 변경 (명시적인 클래스를 인터페이스로 변경)
@Bean
public UserService userService() {
    UserServiceImpl userService = new UserServiceImpl();
    userService.setUserDao(userDao());
    userService.setMailSender(mailSender());
    return userService;
}
```

#### 자동 프록시 생성기를 사용하는 테스트

`@Autowired`를 통해 컨텍스트에서 가져오는 `UserService` 타입 오브젝트는 `UserServiceImpl` 오브젝트가 아니라 트랜잭션이 적용된 프록시여야 한다. 이를 검증하려면 `upgradeAllOrNothing()` 테스트가 필요한데, 기존의 테스트 코드에서 사용한 방법으로는 한계가 있다. 

지금까지는 `ProxyFactoryBean` 이 빈으로 등록되어 있었으므로 이를 가져와 타깃을 테스트용 클래스로 바꿔치기하는 방법을 사용했다. 하지만 자동 프록시 생성기를 적용하여 더 이상 가져올 `ProxyFactoryBean` 빈이 존재하지 않는다. 자동 프록시 생성기가 알아서 프록시를 만들어줬기 때문에 프록시 오브젝트만 남아있을 뿐이다.

그럼 어떻게 해야할까? 자동 프록기 생성기는 스프링에 종속적이기 때문에 예외상황을 위한 테스트 대상도 빈으로 등록해줘야 한다. 기존에 만들어 사용하던 `TestUserService` 를 빈으로 등록해보자.   
그런데 두 가지 문제가 있다. 첫째는 `TestUserService`가 `UserServiceTest` 클래스 내부에 정의된 스태틱 클래스라는 점이고, 둘째는 포인트컷이 트랜잭션 어드바이스를 적용해주는 대상 클래스의 이름 패턴을 '*ServiceImpl' 로 정의해서 `TestUSerService` 클래스를 빈으로 등록해도 포인트컷이 프록시 적용 대상으로 선정해주지 않는다.

이 문제를 해결하기 위해 수정해보자. 스태틱 클래스 자체는 스프링의 빈으로 등록되는 데 문제 없다. 대신 빈으로 등록할 때 클래스 이름을 지정하는 방법을 알아야 한다. 대신 클래스 이름은 포인트컷이 선정해주도록 `ServiceImpl` 로 끝나야 한다.


```java
    @Bean
public UserService testUserService() {
    return new TestUserServiceImpl();
}

static class TestUserServiceImpl extends UserServiceImpl {
    private String id = "d";

    @Override
    protected void upgradeLevel(User user) {
        if (user.getId().equals(id)) throw new RuntimeException();
        super.upgradeLevel(user);
    }
}
```

```java
@SpringBootTest
class UserServiceTest {

    private List<User> users;

    @Autowired
    private UserService userService;


    // 같은 타입의 빈이 두 개 존재하기 때문에 필드 이름을 기준으로 주입될 빈이 결정됨.
    // 자동 프록시 생성기에 의해 트랜잭션 부가기능이 testUserService 빈에 적용됐는지 확인하는 것이 목적임
    @Autowired
    private UserService testUserService;
        
    // ...

    @Test
    @DirtiesContext
    public void upgradeAllOrNoting() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        try {
            testUserService.upgradeLevels();
            fail("RuntimeException expected");
        } catch (RuntimeException e) {

        }

        checkLevelUpgraded(userDao.get(users.get(1).getId()), false);
    }
}
```

#### 자동생성 프록시 확인

테스트는 성공했다. 몇 가지 특별한 빈 등록과 포인트컷 작성으로 프록시가 자동으로 만들어질 수 있다는걸 확인해보았다.

지금까지 트랜잭션 어드바이스를 적용한 프록시 자동생성기를 빈 후처리기 메커니즘을 통해 적용했다. 최소한 두 가지는 확인해야 한다.

1. 트랜잭션이 필요한 빈에 트랜잭션 부가기능이 적용됐는지
2. 아무 빈에나 트랜잭션이 부가기능이 적용된 것은 아닌지 확인

포인트컷의 클래스 필터를 이용해 정확히 원하는 빈에만 프록시를 생성했는지 확인해보자. 포인트컷 빈의 클래스 이름 패턴을 변경해서 이번엔 `testUserService` 빈에 트랜잭션이 적용되지 않게 해보자.

```java
@Bean
public NameMatchClassMethodPointcut transactionPointcut() {
    NameMatchClassMethodPointcut pointcut = new NameMatchClassMethodPointcut();
    pointcut.setMappedClassName("*NotServiceImpl");
    pointcut.setMappedName("upgrade*");
    return pointcut;
}
```

### 6.5.3 포인트컷 표현식을 이용한 포인트컷 

이번에는 좀 더 편리한 포인트컷 작성 방법을 알아보자. 지금까지 사용했던 포인트컷은 메서드의 이름과 클래스의 이름 패턴을 각각 클래스 필터와 메서드 매처 오브젝트로 비교해서 선정하는 방식이었다. 일일이 클래스 필터와 메서드 매처를 구현하거나, 스프링이 제공하는 필터나 매처 클래스를 가져와 프로퍼티를 설정하는 방식을 사용해야 했다.

지금까지는 단순한 이름을 비교하는 일이 전부였다. 이보다 더 복잡하고 세밀한 기준을 이용해 클래스나 메서드를 선정하게 하려면 어떻게 해야 할까? 필터나 매처에서 클래스와 메서드의 메타정보를 제공받으니 불가능할 것은 없다. 리플렉션 API를 통해 클래스와 메서드의 이름, 정의된 패키지, 파라미터, 리턴 값은 물론이고 부여된 애노테이션이나 구현 인터페이스, 상속 클래스 등의 정보까지 알 수 있다.  
하지만 리플렉션 API는 코드를 작성하기가 제법 번거롭다는 단점이 있다. 또한 리플렉션 API를 이용해 메타정보를 비교하는 방법은 조건이 달라질 때마다 포인트컷 구현 코드를 수정해야 하는 번거로움도 있다.

스프링은 아주 간단한 방법을 제공한다. 정규식이나 JSP의 EL과 비슷한 일종의 표현식 언어를 사용해서 포인트컷을 작성할 수 있게 한다. 이것을 **포인트컷 표현식** 이라고 부른다.

#### 포인트컷 표현식

포인트컷 표현식을 지원하는 포인트컷을 적용하려면 `AspectJExpressionPointcut` 클래스를 사용하면 된다. `Pointcut` 인터페이스를 구현해야 하는 스프링의 포인트컷은 클래스 선정을 위한 **클래스 필터**와 메서드 선정을 위한 **메서드 매처** 두 가지를 제공해야 한다.  
앞서 만들었던 `NameMatchClassMethodPointcut` 은 클래스와 메서드의 이름의 패턴을 독립적으로 비교하도록 만들어져 있다. 이를 위해 비교할 조건을 가진 두 가지 패턴을 프로퍼티로 넣어줬다. 하지만 `AspectJExpressionPointcut` 은 클래스와 메서드 선정 알고리즘을 포인트컷 표현식을 이용해 한 번에 지정할 수 있게 해준다. 포인트컷 표현식은 자바의 RegEx 클래스가 지원하는 정규식처럼 간단한 문자열로 복잡한 선정조건을 쉽게 만들어낼 수 있는 강력한 표현식을 지원한다. 사실 스프링이 사용하는 포인트컷 표현식은 `AspectJ` 라는 유명 프레임워크에서 제공하는 것을 가져와 일부 문법을 확장해서 사용하는 것이다. 그래서 이를 `AspectJ` 표현식이라고 한다.

학습 테스트를 만들어 살펴보자.

```java
public interface TargetInterface {
    void hello();
    void hello(String a);
    int minus(int a, int b);
    int plus(int a, int b);
    void method();
}

public class Target implements TargetInterface {
    @Override
    public void hello() {}

    @Override
    public void hello(String a) {}

    @Override
    public int minus(int a, int b) throws RuntimeException { return 0; }

    @Override
    public int plus(int a, int b) { return 0; }

    @Override
    public void method() {}
}

public class Bean {
    public void method() throws RuntimeException {

    }
}
```

이제 `Target`, `Bean` 클래스와 총 6개의 메서드를 대상으로 포인트컷 표현식을 적용해보자.

#### 포인트컷 표현식 문법

`AspectJ` 포인트컷 표현식은 포인트컷 지시자를 이용해 작성한다. 포인트컷 지시자 중에서 가장 대표적으로 사용되는 것은 `execution()` 이다. 문법 구조는 다음과 같다.

<img width="580" alt="image" src="https://github.com/user-attachments/assets/0d443ed0-4d30-46cf-9f05-ff7316aa5e79">

보갖ㅂ해보이지만, 메서드의 풀 시그니처를 문자열로 비교하는 개념으로 생각하면 간단하다. 리플렉션으로 `Target` 클래스의 `minus()` 라는 메서드의 풀 시그니처를 가져와 비교해보면 이해하기 쉬울 것이다. 다음 문장을 실행하면 리플렉션의 `Method` 오브젝트가 제공하는 `Target.minus()` 메서드의 풀 시그니처를 볼 수 있다.

```java
System.out.println(Target.class.getMethod("minus", int.class, int.class));
```

출력된 결과는 다음과 같다.

```
public int springbook.learingtest.spring.pointcut.Target.minus(int,int) throws java.lang.RuntimeException
```

##### • public
접근제한자. public, protected, private 등이 올 수 있고, 포인트컷 표현식에서는 생략 가능하다.

##### • int
리턴 값의 타입을 나타내는 패턴이다. 포인트컷의 표현식에서 리턴 값의 타입 패턴은 필수 항목이다. 따라서 반드시 하나의 타입을 지정해야 한다. 또는 `*`을 써서 모든 타입을 다 선택하겠다고 해도 된다.

##### • springbook.learingtest.spring.pointcut.Target
패키지 + 타입 이름을 포함한 클래스의 타입 패턴이다. 생략 가능하다. 생략하면 모든 타입을 다 허용하겠다는 뜻이다. 뒤에 나오는 메서드 이름 패턴과 `.` 으로 연결되기 때문에 작성할 때 잘 구분해야 한다.  
패키지 이름과 클래스 또는 인터페이스 이름에 `*` 을 사용할 수 있다. 또 `..` 을 사용하면 한 번에 여러 개의 패키지를 선택할 수 있다.

##### • minus
메서드 이름 패턴이다. 필수항목이다. 모든 메서드를 다 선택하겠다면 `*` 를 넣으면 된다.

##### • (int,int)
메서드 파라미터의 타입 패턴이다. `,` 로 파라미터를 구분하고 순서대로 적으면 된다. 파라미터가 없는 메서드를 지정할 땐 `()`로 적는다.  
다 허용하는 패턴을 만드려면 `..` 을 넣으면 된다. `...` 을 이용해서 뒷부분의 파라미터 조건만 생략할 수 있다. 
필수항목이다.

##### • throws java.lang.RuntimeException

예외 이름에 대한 타입 패턴. 생략 가능

`Method` 오브젝트를 출력했을 때 나오는 메서드 시그니처랑 동일한 구조를 가지고 비교하는 것이기에 이해하기 어렵지 않다. `Target` 클래스의 `minus()` 메서드만 선정해주는 포인트컷 표현식을 만들고 이를 검증하는 테스트를 작성해보자.


```java
public class PointcutTest {
    @Test
    public void methodSignaturePointcut() throws SecurityException, NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();

        // Target 클래스 minus() 메서드 시그니처
        pointcut.setExpression("execution(public int " + "tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Target.minus(int,int) " +
                "throws java.lang.RuntimeException)");

        // Target.minus()
        assertThat(pointcut.getClassFilter().matches(Target.class)).isTrue();
        assertThat(pointcut.getMethodMatcher().matches(
                Target.class.getMethod("minus", int.class, int.class), null))
                .isTrue();

        // Target.plus() - 메서드 매처에서 실패
        assertThat(pointcut.getClassFilter().matches(Target.class) &&
                pointcut.getMethodMatcher().matches(
                        Target.class.getMethod("plus", int.class, int.class), null))
                .isFalse();

        // Bean.method() - 클래스 필터에서 실패
        assertThat(pointcut.getClassFilter().matches(Bean.class) &&
                pointcut.getMethodMatcher().matches(
                        Bean.class.getMethod("method"), null))
                .isFalse();

    }
}
```

AspectJExpressionPointcut 클래스의 오브젝트를 만들고 포인트컷 표현식을 expression 프로퍼티에 넣어주면 포인트컷을 사용할 준비가 된다. 포인트컷 표현식은 메서드 시그니처를 `execution()` 안에 넣어서 작성한다. `execution()` 은 메서드를 실행에 대한 포인트컷이라는 의미다.

먼저 `Target` 클래스의 `minus()` 메서드에 대해 테스트를 해본다. 포인트컷의 선정 방식은 클래스 필터와 메서드 매처를 각각 비교해보는 것이다. 두 가지 조건을 모두 만족시키면 해당 메서드는 포인트컷의 선정 대상이 된다.

`Target` 클래스의 다른 메ㅓ드를 비교해본다. 클래스, 파라미터 등은 통과하지만, 메서드 이름과 예외 패턴이 포인트컷 표현식과 일치하지 않기 때문에 결과는 **false** 다. 

#### 포인트컷 표현식 테스트

메서드 시그니처를 그대로 사용한 포인트 표현식을 문법구조로 참고로 해서 정리해보자.  
이 중에서 필수가 아닌 항목인 접근제한자 패턴, 클래스 타입 패턴, 예외 패턴은 생략할 수 있다. 옵션 항목을 생략하면 다음과 같이 간단하게 만들 수 있다.

```java
// int 타입의 리턴 값, minus 라는 메서드 이름, 두 개의 int 파라미터를 가진 모든 메서드를 선정
execution(int minus(int,int))
```

좀 더 간결해졌지만, 이 포인트컷 표현식은 어떤 접근 제한자를 가졌든, 어떤 클래스에 정의됐든, 어떤 예외를 던지든 상관없이 정수 값을 리턴하고 두 개의 정수형 파라미터를 갖는 minus 라는 이름의 모든 메서드를 선정하는 좀 더 느슨한 포인트컷이 됐다는 점에 주의하자.

리턴 값의 타입에 제한을 없애려면 `*` 와일드 카드를 쓰면 된다.

```java
// 리턴 타입은 상관없이 minus라는 메서드 이름, 두 개의 int 파라미터를 가진 모든 메서드를 선정
execution(* minus(int,int))
```

모든 선정조건을 다 없애고 모든 메서드를 다 허용하는 포인트컷이 필요하다면 다음과 같이 메서드 이름도 바꾸면 된다.

```java
// 리턴 타입, 파라미터, 메서드 이름에 상관없이 모든 메서드 조건을 다 허용하는 포인트컷 표현식
execution(* *(..))
```

테스트를 보충해보자. 앞에서 만든 Target, Bean 클래스의 6개 메서드에 대해 각각 포인트컷을 적용해서 결과를 확인하는 테스트다.

```java
public void pointcutMatches(String expression, Boolean expected, Class<?> clazz, String methodName, Class<?>... args) throws Exception {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(expression);

    assertThat(pointcut.getClassFilter().matches(clazz)
    && pointcut.getMethodMatcher().matches(clazz.getMethod(methodName, args), null))
            .isTrue();
}
```
다음은 `pointcutMatches()` 메서드를 활용해서 타깃으로 만든 두 클래스의 모든 메서드에 대해 포인트컷 선정 여부를 확인하는 메서드를 추가한다.

```java
public void targetClassPointcutMatches(String expression, boolean... expected) throws Exception {
    pointcutMatches(expression, expected[0], Target.class, "hello");
    pointcutMatches(expression, expected[1], Target.class, "hello", String.class);
    pointcutMatches(expression, expected[2], Target.class, "plus", int.class, int.class);
    pointcutMatches(expression, expected[3], Target.class, "minus", int.class, int.class);
    pointcutMatches(expression, expected[4], Target.class, "method");
    pointcutMatches(expression, expected[5], Bean.class, "method");
}
```

이제 다양한 포인트컷을 만들어서 모든 메서드에 대한 포인트컷 적용 결과를 확인해보자. 아래 표는 포인트컷 표현식과 그에 대한 `targetClassPointcutMatches()` 의 각 메서드별 포인트컷 검사 결과다. 총 19가지의 포인트컷 표현식에 대해 결과를 검증할 수 있도록 만들어진 테스트의 결과를 정리한 것이다.

```java
@Test
public void pointcut() throws Exception {
    targetClassPointcutMatches("execution(* *(..))", true, true, true, true, true, true);
}
```

아래 표를 보고 테스트 결과를 확인해보자.

<img width="507" alt="image" src="https://github.com/user-attachments/assets/09eb9b33-bbd9-4447-b2aa-1aa153bbe0b9">


#### 포인트컷 표현식을 이용하는 포인트컷 적용

AspectJ 포인트컷 표현식은 메서드를 선정하는 데 편리하게 쓸 수 있는 강력한 표현식 언어다. `execution()` 외에도 몇 가지 표현식 스타일을 갖고 있다.  
`bean(*Service)` 이라고 쓰면 아이디가 `Service`로 끝나는 모든 빈을 선택한다. 단지 클래스와 메서드라는 기준을 넘어서는 유용한 선정 방식이다.  
또 특정 애노테이션의 타입, 메서드, 파라미터에 적용되어 있는 것을 보고 메서드를 선정하게 하는 포인트컷을 만들 수 있다. 아래는 `@Transactional` 애노테이션이 적용된 메서드를 선정해준다.

```java
@annotation(org.springframework.transaction.annotation.Transactional)
```

이제 적용해볼 차례다. 앞에서 만든 `transactionPointcut` 빈은 제거하자. 이제 `nameMatchClassMethodPointcut` 과 같이 직접 만든 포인트컷 구현 클래스를 사용할 일은 없을 것이다.

```java
pointcut.setMappedClassName("*NotServiceImpl");
pointcut.setMappedName("upgrade*");
```

기존 적용되었던 선정 조건을 다시 보자. 이걸 바탕으로 동일한 기준의 포인트컷 표현식을 만들어 보자.

```java
@Bean
public AspectJExpressionPointcut transactionPointcut() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    String expression = "* *..*ServiceImpl.upgrade*(..))";
    pointcut.setExpression(expression);
    return pointcut;
}
```

설정 파일을 수정했으면 `UserServiceTest` 테스트를 수행해보자.


#### 타입 패턴과 클래스 이름 패턴

포인트컷 표현식을 적용하기 전에는 클래스 이름의 패턴을 이용해 타깃 빈을 선정하는 포인트컷을 사용했다. `UserService` 를 구현한 `UserServiceImpl` 클래스와 테스트를 위한 `TestUserServiceImpl` 모두 빈으로 등록하고 해당 빈을 선정하는 포인트컷을 구성했다.  
이러한 방식을 타입 패턴으로 `*..*ServiceImpl` 로 풀어내니 간결해졌다.

그런데 앞에서 사용했던 단순한 클래스 이름 패턴과 포인트컷 표현식에서 사용하는 타입 패턴은 중요한 차이점이 있다. 이를 확인하기 위해서 `TestUserServiceImpl` 이라고 변경했던 테스트용 클래스의 이름을 다시 `TestUserService`로 바꿔보자.

테스트를 해보면 타입 패턴이 `*..*ServiceImpl` 이므로 `TestUserService`는 선정되지 않을 것이고, 테스트는 실패일거 같다. 하지만 실제로 실행해보면 결과는 성공이다.  
이 이유는 포인트컷 표현식의 클래스 이름에 적용되는 패턴은 클래스 이름 패턴이 아니라 타입 패턴이기 때문이다. `TesrUserService` 의 이름은 `TestUserService` 일 뿐, 타입을 따져보면 `TestUserService` 클래스이자, `UserServiceImpl`, 구현 인터페이스인 `UserService` 세가지 모두 적용된다. 즉 `TestUserService` 클래스로 정의된 빈은 `UserServiceImpl` 타입이기도 하고, 그 때문에 `ServiceImpl` 로 끝나는 타입 패턴의 조건을 충족하는 것이다. 포인트컷 표현식 테스트의 16번을 다시 생각해보면 이해가 될 것이다.

포인트컷 표현식의 타입 패턴 항목을 `*..UserService` 라고 직접 인터페이스 이름을 명시해도 두 개의 빈이 모두 선정된다. 두 클래스 모두 U`serService` 인터페이스를 구현하기 때문이다.


### 6.5.4 AOP란 무엇인가?

비즈니스 로직을 담은 `UserService`에 트랜잭션을 적용해온 과정을 정리해보자. 

#### 트랜잭션 서비스 추상화

트랜잭션 경계설정 코드를 비즈니스 로직을 담은 코드에 넣으면서 맞닥뜨린 첫 번째 문제는 특정 트랜잭션 기술에 종속되는 코드가 돼버린것이었다. JDBC의 로컬 트랜잭션 방식, JTA를 이용한 글로벌/분산 트랜잭션 방식 등 바꾸려면 모든 트랜잭션 적용 코드를 수정해야 한다는 문제점이 있었다. 트랜잭션을 처리한다는 기본적인 목적이 변하지 않아도 구체적 방법이 변하려면, 트랜잭션과는 직접 관련 없는 코드가 담긴 많은 클래스를 일일이 수정해야 했다.

그래서 트랜잭션 적용이라는 추상적인 작업 내용은 유지한 채로 구체적인 구현 방법을 바꿀 수 있도록 서비스 추상화 기법을 적용했다. 이 덕분에 비즈니스 코드는 트랜잭션을 어떻게 처리해야 한다는 구체적인 방법과 서버 환경에서 종속되지 않는다. 구체적인 구현 내용을 담은 의존 오브젝트는 런타임 시에 다이내믹하게 연결해준다는 DI를 활용한 전형적인 접근 방법이었다.

트랜잭션 추상화란 결국 인터페이스와 DI를 통해 무엇을 하는지 남기고, 그것을 어떻게 하는지를 분리한 것이다. 어떻게 할지는 더 이상 비즈니스 로직 코드에는 영향을 주지 않고 독립적으로 변경할 수 있게 됐다.

#### 프록시와 데코레이터 패턴

트랜잭션을 어떻게 다룰 것인가는 추상화를 통해 코드에서 제거했지만, 여전히 비즈니스 로직 코드에는 트랜잭션을 적용하고 있다는 사실은 드러나 있다. 트랜잭션이라는 부가적인 기능을 어디에 적용할 것인가는 적용 여전히 코드에 노출시켜야 했다. 문제는 트랜잭션은 거의 대부분의 비즈니스 로직을 담은 메서드에 필요하다는 점이다. 게다가 트랜잭션의 경계설정을 담당하는 코드의 특성 때문에 단순한 추상화와 메서드 추출 방법으로는 더 이상 제거할 방법이 없었다.

그래서 도입한 것이 DI를 이용해 데코레이터 패턴을 적용하는 방법이었다. 투명한 부가기능 부여를 가능하게 하는 데코레이터 패턴의 적용 덕에 비즈니스 로직을 담당하는 클래스도 자신을 사용하는 클라이언트와 DI 관계를 맺을 이유를 찾게 됐다. 클라이언트가 인터페이스와 DI를 통해 접근하도록 설계하고, 데코레이터 패턴을 적용해서, 비즈니스 로직을 담은 클래스의 코드에는 전혀 영향을 주지 않으면서 트랜잭션이라는 부가기능을 자유롭게 부여할 수 있는 구조를 만들었다.

트랜잭션을 처리하는 코드는 일종의 데코레이터에 담겨, 클라이언트와 비즈니스 로직을 담은 타깃 클래스 사이에 존재하도록 만들었다. 그래서 클라이언트가 일종의 대리자인 프록시 역할을 하는 트랜잭션 데코레이터를 거쳐서 타깃에 접근할 수 있게 됐다.

결국 비즈니스 로직 코드는 트랜잭션과 같은 성격이 다른 코드로부터 자유로워졌고, 독립적으로 로직을 검증하는 고립된 단위 테스트를 만들 수도 있게 됐다.

#### 자동 프록시 생성 방법과 포인트컷

트랜잭션 적용 대상이 되는 빈마다 일일이 프록시 팩토리 빈을 설정해줘야 한다는 부담이 남아 있었다.

이를 해결하기 위해 스프링 컨테이너의 빈 생성 후처리 기법을 활용해 컨테이너 초기화 시점에서 자동으로 프록시를 만들어주는 방법을 도입했다. 프록시를 적용할 대상을 일일이 지정하지 않고 패턴을 이용해 자동으로 선정할 수 있도록, 클래스를 선정하는 기능을 담은 확장된 포인트컷이라는 독립적인 정보로 완전히 분리할 수 있었다. 처음에는 클래스와 메서드 선정 로직을 담은 코드를 직접 만들어서 포인트컷으로 사용했지만, 최종적으로는 포인트컷 표현식이라는 좀 더 편리하고 깔끔한 방법으로 적용 대상을 선정했다.

#### 부가기능의 모듈화

관심사가 같은 코드를 분리해 한데 모으는 것은 소프트웨어 개발의 가장 기본이 되는 원칙이다. 그렇게 관심사가 같은 코드를 객체지향 설계 원칙에 따라 분리하고, 서로 낮은 결합도를 가진 채로 독립적이고 유연하게 확장할 수 있는 모듈로 만드는 것이 지금까지 해온 작업이다.

코드를 분리하고, 한데 모으고, 인터페이스를 도입하고, DI를 통해 런타임 시에 의존 관계를 만들어줌으로써 대부분의 문제를 해결할 수 있었다. 하지만 이 트랜잭션 적용 코드는 기존에 써왔던 방법으로는 간단하게 분리해서 독립된 모듈로 만들 수가 없었다.  
왜냐하면 트랜잭션 경계설정 기능을 다른 모듈의 코드에 부가적으로 부여되는 기능이라는 특징이 있기 때문이다. 그래서 트랜잭션 코드는 한데 모을 수 없고, 애플리케이션 전반에 여기저기 흩어져 있다. 따라서 트랜잭션 경계설정 기능을 독립된 모듈로 만들려면 특별한 기법이 필요하다. 클래스를 만들지 않고도 새로운 구현 기능을 가진 오브젝트를 다이내믹하게 만들어내는 동적 프록시라든가 IoC/DI 컨테이너의 빈 생성 작업을 가로채서 빈 오브젝트를 프록시로 대체하는 빈 후처리 기술과 같은 기술이 요구된다.

트랜잭션 부가기능은 핵심기능과 같은 방식으로는 모듈화하기 매우 힘들다. 말 그대로 부가기능이기 때문에 스스로는 독립적인 방식으로 존재해서는 적용되기 어렵기 때문이다. 트랜잭션 부가기능이란 트랜잭션 기능을 추가해줄 다른 대상, 즉 타깃이 존재해야만 의미가 있다. 따라서 각 기능을 부가할 대상인 각 타깃의 코드 안에 침투하거나 긴밀하게 연결되어 있지 않으면 안 된다. 기능이 부여되는 타깃은 애플리케이션의 핵심 기능이다. 사용자 관리와 같은 핵심기능을 가진 모듈은 그 자체로 독립적으로 존재할 수 있으며, 독립적으로 테스트도 가능하고, 최소한의 인터페이스를 통해 다른 모듈과 결합해 사용하면 된다. 반면에 부가기능은 여타 핵심 기능과 같은 레벨에서 독립적으로 존재하는 것 자체가 불가능하다.

그래서 많은 개발자는 핵심기능을 담당하는 코드 여기저기에 흩어져 나타나야 했던 이런 부가기능을 어떻게 독립적인 모듈로 만들 수 있을까를 고민해왔다. 지금까지 살펴본 DI, 데코레이터 패턴, 다이내믹 프록시, 오브젝트 생성 후처리, 자동 프록시 생성, 포인트컷과 같은 기법은 이런 문제를 해결하기 위해 적용한 대표적인 방법이다. 덕분에 부가기능인 트랜잭션 경계설정 기능은 `TrasactionAdvice` 라는 이름으로 모듈화될 수 있었다. 독립적으로 모듈화되어 있기 때문에 이 코드는 중복되지 않으며, 변경이 필요하면 한곳만 수정하면 된다. 또한 포인트컷이라는 방법을 통해 부가기능을 부여할 대상을 선정할 수 있었다. 이런 기법을 잘 활용한 덕분에 핵심기능을 담은 코드와 설정에는 전혀 영향을 주지 않아도 됐다.

결국 지금까지 해온 모든 작업은 핵심 기능에 부여되는 부가기능을 효과적으로 모듈화하는 방법을 찾는 것이었고, 어드바이스와 포인트컷을 결합한 어드바이저가 단순하지만 이런 특성을 가진 모듈의 원시적인 형태로 만들어지게 됐다.

#### AOP: 애스펙트 지향 프로그래밍

전통적인 객체지향 기술의 설계 방법으로는 독립적인 모듈화가 불가능한 트랜잭션 경계설정과 같은 부가기능을 어떻게 모듈화할 것인가를 연구해온 사람들은, 이 부가기능 모듈화 작업은 기존의 객체지향 설계 패러다임과는 구분되는 새로운 특성이 있다고 생각했다. 그래서 이런 부가기능 모듈을 객체지향 기술에서 주료 사용하는 오브젝트와는 다르게 특별한 이름으로 부르기 시작했다. 그것이 바로 `Aspect(에스팩트)`다. `Aspect`란 그 자체로 애플리케이션의 핵심 기능을 담고 있진 않지만, 중요한 구성 요소이고, 핵심 기능에 부가되어 의미를 갖는 특별한 모듈을 가리킨다.

`Aspect`는 부가될 기능을 정의한 코드인 어드바이스, 이 어드바이스를 어디에 적용할 것인지를 결정하는 포인트 컷을 함께 갖고 있다.` Aspect`는 그 단어 의미대로 애플리케이션을 구성하는 한 가지 측면이라고 생각할 수 있다. 아래 그림을 살펴보자.

<img width="534" alt="image" src="https://github.com/user-attachments/assets/00c67e5a-8387-45a4-a85e-34f482a6ee10">

왼쪽은 `Aspect`로 부가기느을 분리하기 전 상태다. 핵심 기능들이 모듈화되어 있지만, 부가기능이 추가되며 설계와 코드가 지저분해졌다. 오른쪽은 이렇게 핵심기능 코드 사이에 침투한 부가기능을 `Aspect`로 구분해낸 것이다. 애플리케이션의 부가기능이 핵심 기능과 어우러져 동작하게 되어 있다. 하나 이상의 부가기능이 핵심기능과 함께 동작 가능하다. 결국 런타임 시 왼쪽 그림처럼 각 부가기능 `Aspect`는 자기가 필요한 위치에 아니애믹하게 참여하게 될 것이다.

이렇게 핵심 기능에서 부가 기능을 분리해서 `Aspect`라는 독특한 모듈로 만들어 설계하고 개발하는 방법을 `에스팩트 지향 프로그래밍 (Aspect Oriented Programming` 또는 약자로 `AOP`라고 한다. 이름만 들으면 `OOP`와 다른 프로그래밍 언어 또는 기술이라고 느껴지지만, `AOP`는 `OOP`를 돕는 보조적인 기술이지 `OOP`를 완전히 대체하는 새로운 개념은 아니다.

`AOP`는 애플리케이션을 다양한 측면에서 독립적으로 모델링하고, 설계하고, 개발할 수 있도록 만들어주는 것이다. 애플리케이션을 사용자 관리라는 핵심 로직 대신 트랜잭션 경계설정이라는 관점에서 바라보고 그 부분에 집중해서 설계하고 개발할 수 있게 된다는 뜻이다.


### 6.5.5 AOP 적용기술

#### 프록시를 이용한 AOP

스프링은 IoC/DI 컨테이너와 다이내믹 프록시, 데코레이터 패턴, 프록시 패턴, 자동 프록시 생성 기법, 빈 오브젝트의 후처리 조작 기법 등의 다양한 기술을 조합해 AOP를 지원하고 있다. 그중 가장 핵심은 프록시를 이용했다는 것이다. 프록시로 만들어서 DI로 연결된 빈 사이에 적용해 타깃의 메서드 호출 과정에 참여해서 부가기능을 제공해주도록 만들었다. 따라서 스프링 AOP는 자바의 기본 JDK와 스프링 컨테이너 외에는 특별한 기술이나 환경을 요구하지 않는다. 스프링 컨테이너인 애플리케이션 컨텍스트는 특별한 환경이나 JVM 설정 등을 요구하지 않는다. 서버환경이라면 가장 기초적인 서블릿 컨테이너만으로도 충분하며, 원한다면 독립형 애플리케이션에서도 사용 가능하다. 스프링 AOP 또한 마찬가지다.

스프링 AOP의 부가기능을 담은 어드바이스가 적용되는 대상은 오브젝트의 메서드다. 프록시 방식을 사용했기 때문에 메서드 호출 과정에 참여해서 부가기능을 제공해주게 되어 있다. 어드바이스가 구현하는 `MethodInterceptor` 인터페이스는 다이내믹 프록시의 `InvocationHandler` 와 마찬가지로 프록시로부터 메서드 요청정보를 전달받아서 타깃 오브젝트의 메서드를 호출한다. 타깃의 메서드를 호출하는 전후에 다양한 부가기능을 제공할 수 있다.

독립적으로 개발한 부가기능 모듈을 다양한 타깃 오브젝트의 메서드에 다이내믹하게 적용해주기 위해 가장 중요한 역할을 맡고 있는 게 바로 프록시다. 그래서 스프링 AOP는 프록시 방식의 AOP라고 할 수 있다. 

#### 바이트코드 생성과 조작을 위한 AOP
그렇다면 프록시 방식이 아닌 AOP도 있다는 뜻인가? AOP 기술의 원조이자, AOP 프레임워크로 꼽히는 AspectJ는 프록시를 사용하지 않는 대표적인 AOP 기술이다. 스프링도 AspectJ 의 뛰어난 포인트컷 표현식을 차용해서 사용한다. AspectJ 는 스프링처럼 동적 프록시 방식을 사용하지 않는다.

AspectJ는 프록시처럼 간접적인 방법이 아니라, 타깃 오브젝트를 뜯어고쳐 부가기능을 직접 넣어주는 직접적인 방법을 사용한다. 부가기능을 넣기 위해 타깃 오브젝트 소스코드를 수정할 순 없으니, 컴파일된 타깃의 클래스 파일 자체를 수정하거나 클래스가 JVM에 로딩되는 시점을 가로채서 바이트코드로 조작하는 복잡한 방법을 사용한다. 트랜잭션 코드가 UserService 클래스에 비즈니스 로직과 함께 있었을 때처럼 만들어버리는 것이다. 물론 소스코드를 수정하지는 않으므로 개발자는 계속해서 비즈니스 로직에 충실한 코드를 만들 수 있다.

왜 바이트코드 조작과 같은 복잡한 방법을 사용할까?  
그 이유 중 첫째는 바이트코드를 조작해서 타깃 오브젝트를 직접 수정해버리면 스프링과 같은 DI 컨테이너의 도움을 받아서 자동 프록시 생성 방식을 사용하지 않아도 AOP를 적용할 수 있기 때문이다. 스프링과 같은 컨테이너가 사용되지 않는 환경에서도 손쉽게 AOP의 적용이 가능해진다.  
둘째는 프록시 방식보다 유연하기 때문이다. 프록시를 사용하면 부가기능을 부여할 대상은 클라이언트가 호출할 때 사용하는 메서드로 제한된다. 하지만 바이트코드를 직접 조작해서 AOP를 적용하면 오브젝트의 생성, 필드 값의 조회와 조작, 스태틱 초기화 등의 다양한 작업에 부가기능을 부여해줄 수 있다. 타깃 오브젝트가 생성되는 순간 부가기능을 부여해주고 싶을 수도 있다. 하지만 프록시 방식에서는 이런 작업이 불가능하다. 타깃 오브젝트의 생성은 프록시 패턴을 적용할 수 있는 대상이 아니기 때문이다. 또 private 메서드의 호출, static 메서드 호출이나 초기화, 심지어 필드 입출력 등에 부가기능을 부여하려고 하면 클래스 바이트 코드를 직접 조작하는 방법밖에 없다.

물론 대부분의 부가기능은 프록시 방식을 사용해 메서드의 호출 시점에 부여하는 것으로 충분하다. 게다가 AspectJ 는 바이트 조작을 위해 JVM 실행 옵션 병경, 별도의 바이트코드 컴파일러 사용, 특별한 클래스 로더 사용 등 추가 작업이 필요하다. 따라서 일반적인 AOP를 적용하는 데는 프록시 방식의 스프링 AOP로도 충분하다.