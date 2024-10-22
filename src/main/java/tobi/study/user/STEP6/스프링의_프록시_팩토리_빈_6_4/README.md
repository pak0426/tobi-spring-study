## 6.4 스프링의 프록시 팩토리 빈

지금까지 기존 코드 수정 없이 트랜잭션 부가기능을 추가하는 다양한 방법을 살펴봤다. 이번엔 스프링은 이러한 문제에 어떤 해결책을 제시하는지 살펴보자.

### 6.4.1 ProxyFactoryBean

스프링은 트랜잭션, 메일 발송 기술에 적용했던 서비스 추상화를 프록시 기술에도 동일하게 적용한다. 자바에는 JDK에서 제공하는 다이내믹 프록시 외에도 편리하게 프록시를 만들 수 있도록 지원해주는 다양한 기술이 존재한다.

생성된 프록시는 스프링 빈으로 등록돼야 한다. 스프링은 프록시 오브젝트를 생성해주는 기술을 추상화한 팩토리 빈을 제공해준다.

스프링의 ProxyFactoryBean은 프록시를 생성해서 빈 오브젝트로 등록하게 해주는 팩토리 빈이다. 순수하게 프록시를 생성하는 작업만을 담당하고 프록시를 통해 제공해줄 부가기능은 별도의 빈에 둘 수 있다.

ProxyFactoryBean이 생성하는 프록시에서 사용할 부가기능은 `MethodInterceptor` 인터페이스를 구현해 만든다. `MethodInterceptor` 는 따라서 타깃은  `InvocationHandler`의 invoke() 메서드는 타깃 오브젝트에 대한 정보를 제공하지 않는다. `InvocationHandler`를 구현한 클래스가 직접 알아야 한다.

반면 `MethodInterceptor`의 invoke() 메서드는 `ProdxyFacotryBean`으로 부터 타깃 오브젝트에 대한 정보까지 제공받는다. 그 차이 덕분에 타깃 오브젝트에 상관없이 독립적으로 만들어질 수 있다. **따라서 `MethodInterceptor` 오브젝트는 타깃이 다른 여러 프록시에서 함께 사용할 수 있고, 싱글톤 빈으로 등록 가능하다.**


```java
class DynamicProxyTest {
    @Test
    public void simpleProxy() {
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { Hello.class },
                new UppercaseHandler(new HelloTarget())
        );
    }

    @Test
    public void proxyFactoryBean() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget()); // 타깃 설정
        pfBean.addAdvice(new UppercaseAdvice()); // 부가기능을 담은 어드바이스 추가, 여러 개 가능

        Hello proxiedHello = (Hello) pfBean.getObject(); // FactoryBean이므로 getObject()로 생성된 프록시를 가져옴

        assert proxiedHello != null;
        assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
        assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
        assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("THANK YOU HM");
    }

    static class UppercaseAdvice implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 리플렉션의 Method와 달리 메서드 실행 시 타깃 오브젝트를 전달할 필요가 없다. MethodInvocation은 
            // 메서드 정보와 함께 타깃 오브젝트를 알고 있기 때문이다. 
            String ret = (String) invocation.proceed();
            return ret.toUpperCase();
        }
    }
}
```

#### 어드바이스: 타깃이 필요 없는 순수한 부가기능 

**ProxyFactoryBean vs 기존의 JDK 동적 프록시**  
`MethodInterceptor`로는 메서드 정보와 함께 타깃 오브젝트가 담기기 때문에 `InvocationHandler`로 구현했을 때와 다르게 타깃 오브젝트가 등장하지 않는다. `MethodInterceptor`는 부가기능을 제공하는데만 집중할 수 있다.

`MethodInvocation`은 일종의 콜백 오브젝트로, proceed() 메서드를 실행하면 타깃 오브젝트의 메서드를 내부적으로 실행한다. 그렇다면 `MethodInvocation` 구현 클래스는 일종의 공유 가능한 템플릿처럼 동작하는 것이다. 바로 이 점이 JDK 동적 프록시를 직접 사용하는 코드와 스프링이 제공해주는 프록시 추상화 기능인 `ProxyFactoryBean` 을 사용하는 가장 큰 차이점이자 `ProxyFactoryBean`의 장점이다.

`ProxyFactoryBean`은 작은 단위의 템플릿/콜백 구조를 응용해서 적용했기 때문에 템플릿 역할을 하는 `MethodInvocation`을 싱글톤으로 두고 공유할 수 있다.  
또한 addAdvice() 라는 메서드는 일반적인 DI처럼 수정자 메서드를 사용하지 않게 도와준다. add라는 이름에서 알 수 있듯 여러 개의 MethodInterceptor를 추가할 수 있다. 즉 여러 개의 부가기능을 제공하는 프록시를 만들 수 있다는 뜻이다. 앞서 봤던 프록시 팩토리 빈의 단점 중 하나였던, 새로운 부가기능을 추가할 때마다 프록시와 프록시 팩토리 빈을 추가해야 한다는 문제를 해결한다.

하지만 `MethodInterceptor` 오브젝트를 추가하는 메서드 이름은 `addMethodInterceptor` 가 아니라 `addAdvice`다. `MethodInterceptor`는 `Advice` 인터페이스를 상속하고 있는 서브 인터페이스이기 때문이다.  
`MethodInterceptor`처럼 타깃 오브젝트에 적용하는 부가기능을 담은 오브젝트를 스프링에서는 **어드바이스 (Advice)**라고 부른다.

마지막으로 `ProxyFactoryBean` 을 적용한 코드에는 프록시가 구현해야 하는 Hello라는 인터페이스를 제공해주는 부분이 없다. 프록시를 직접 만들 때나 JDK 동적 프록시를 만들 때 반드시 제공해줘야 하는 정보가 Hello 인터페이스였다. 그래야만 다이내믹 프록시 오브젝트의 타입을 결정할 수 있기 때문이다.

그런데 스프링의 `ProxyFactoryBean`은 어떻게 인터페이스 타입을 제공받지 않고 Hello 인터페이스를 구현한 프록시를 만들어낼까? 물론 `ProxyFactoryBean`도 `setInterfaces()` 메서드를 통해 구현해야 할 인터페이스를 지정할 수 있다. 하지만 굳이 알려주지 않아도 자동검출 기능을 사용해 타깃 오브젝트가 구현하고 있는 인터페이스 정보를 알아낸다. 그리고 알아낸 인터페이스를 모두 구현하는 프록시를 만들어준다. **타깃 오브젝트가 구현하고 있는 모든 인터페이스를 동일하게 구현하는 프록시를 만들어주는 기능이다.**


#### 포인트컷: 부가기능 적용 대상 메서드 선정 방법

기존에 `InvocationHandler`를 직접 구현했을 때는 부가기능 적용 외에 메서드의 이름을 가지고 부가기능을 적용 대상 메서드를 선정하는 작업이 필요했다. `TxProxyFactoryBean`은 pattern 이라는 메서드 이름 비교용 스트링 값을 DI 받아서 `TransactionHandler`를 생성할 때 이를 넘겨주고, `TransactionHandler`는 요청이 들어오는 메서드의 이름과 패턴을 비교해서 부가기능인 트랜잭션 적용 대상을 판별했다.

그렇다면 스프링의 `ProxyFactoryBean`과 `MethodInterceptor`를 사용하는 방식에서 메서드 선정 기능을 넣을 수 있을까?

`MethodInterceptor` 오브젝트는 여러 프록시가 공유해서 사용한다. 그러기 위해 타깃 정보를 가지고 있지 않다. 그 덕분에 싱글톤 빈으로 등록할 수 있다. 그런데 여기에 트랜잭션 적용 대상 메서드 이름 패턴을 넣어주는건 곤란하다. 프록시마다 트랜잭션 적용 메서드 패턴이 다를 수 있기 때문에 특정 프록시에만 적용되는 패턴을 적용하면 문제가 된다.

이 문제를 해결하는 방법은 생성 방식과 의존관계가 다른 코드가 함께 있다면 분리해주면 된다. `MethodInterceptor`는 클라이언트로 부터 요청을 일일이 전달받을 필요가 없다. 재사용한 순수 부가기능 코드만 남겨주는 것이다. **대신 프록시에 부가기능 적용 메서드를 선택하는 기능을 넣자.**

아래 그림에서 볼 수 있듯 기존 방식도 동적 프록시와 부가기능을 분리할 수 있고, 부가기능 적용 대상 메서드를 선정할 수 있다.

<img width="568" alt="image" src="https://github.com/user-attachments/assets/4ffb7fd6-4bd9-4ce5-9e6b-5dd07b050bd6">

하지만 문제는 부가기능을 가진 `InvocationHandler`가 타깃과 메서드 선정 알고리즘 코드에 의존하고 있다는 점이다. 만약 타깃이 다르고 메서드 선정 방식이 다르다면 `InvocationHandler` 오브젝트를 여러 프록시가 공유할 수 없다. 그래서 `InvocationHandler`는 따로 빈으로 등록하지 않고 `TxProxyFactoryBean` 내부에서 매번 생성하도록 만들었던 것이다.

반면 아래 그림에서 나타난 스프링의 `ProxyFactoryBean` 방식은 두 가지 확장 기능인 `부가기능 (Advice)`과 `메서드 선정 알고리즘(PointCut)`을 활용하는 유연한 구조를 제공한다.

<img width="628" alt="image" src="https://github.com/user-attachments/assets/5f1e3520-d614-4b55-89d3-8f20b8eff765">

스프링은 부가기능을 제공하는 오브젝트를 **어드바이스**라고 부르고, 메서드 선정 알고리즘을 담은 오브젝트를 **포인트컷**이라 부른다. 어드바이스와 포인트컷은 모두 프록시에 DI로 주입돼서 사용된다. 두 가지 모두 여러 프록시에서 공유가 가능하도록 만들어지기 때문에 스프링의 싱글톤 빈으로 등록 가능하다.

어드바이스와 함께 이름 패턴을 이용해 메서드를 선정하는 포인트컷까지 적용되는 테스트를 만들어 보자.

```java
@Test
public void pointcutAdvisor() {
    ProxyFactoryBean pfBean = new ProxyFactoryBean();
    pfBean.setTarget(new HelloTarget());

    NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
    pointcut.setMappedName("sayH*");

    // 포인트컷 + 어드바이스 = 어드바이저
    pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));

    Hello proxiedHello = (Hello) pfBean.getObject();

    assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
    assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
    assertThat(proxiedHello.sayThankYou("hm")); // 메서드 이름이 포인트컷 선정 조건에 맞지 않음. 부가기능 적용 x
}
```

포인트컷이 필요 없을 때는 `ProxyFactoryBean`의 addAdvice() 메서드를 호출해서 어드바이스만 등록하면 됐다. 하지만 포인트컷을 함께 등록할 때는 포인트컷 + 어드바이스하여 addAdvisor() 메서드를 호출해야 한다. 그 이유는 `ProxyFactoryBean`에는 여러 어드바이스와 포인트컷이 추가될 수 있기 때문이다. 포인트컷과 어드바이스가 따로 등록되면 어떤 부가기능을 어떤 메서드에 적용할지 애매해지기 때문이다.  
이렇게 어드바이스와 포인트컷을 묶은 오브젝트를 어드바이저라고 부른다.

> Advisor = Pointcut(메서드 선정 알고리즘) + Advice(부가기능)

### 6.4.2 ProxyFactoryBean 적용

#### TransactionAdvice

부가기능을 담당하는 어드바이스는 테스트에서 만들어본 것처럼 `MethodInterceptor` 라는 `Advice` 서브인터페이스를 구현해서 만든다. 아래 코드와 같이 JDK 동적 프록시 방식으로 만든 `TransactionHandler` 의 코드에서 타깃과 메서드 선정 부분을 제거해주면 된다.

```java
public class TransactionAdvice implements MethodInterceptor { // 스프링의 어드바이스 인터페이스 구현
    private final PlatformTransactionManager transactionManager;

    public TransactionAdvice(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    // 타깃을 호출하는 기능을 가진 콜백 오브젝트를 프록시로부터 받는다.
    // 덕분에 어드바이스는 특정 타깃에 의존하지 않고 재사용 가능하다.
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            // 콜백을 호출해 타깃 메서드 실행. 타깃 메ㅓ드 호출 전후로 필요한 부가기능을 넣을 수 있다.
            // 경우에 따라 타깃이 아예 호출되지 않게 하거나 재시도를 위한 반복 호출도 가능하다.
            Object ret = invocation.proceed();
            this.transactionManager.commit(status);
            return ret;
        } catch (RuntimeException e) {
            // JDK 동적 프록시가 제공하는 Method 와는 달리 스프링의 MethodInvocation 을 통한 타깃 호출은
            // 예외가 포장되지 않고 타깃에서 보낸 그대로 전달된다.
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
```

JDK 동적 프록시의 `InvocationHandler` 를 이용해서 만들었을 때보다 코드가 간결하다. 리플렉션을 통한 타깃 메서드 호출 작업의 번거로움은 MethodInvocation 타입의 콜백을 이용한 덕분에 대부분 제거할 수 있다. 타깃 메서드가 던지는 예외도 InvocationTargetException으로 포장돼서 오는 것이 아니기 때문에 그대로 잡아서 처리하면 된다.

### 스프링 DI 설정

```java
@Bean
public ProxyFactoryBean userService() {
    ProxyFactoryBean proxy = new ProxyFactoryBean();
    proxy.setTarget(userServiceImpl());
    proxy.addAdvisor(transactionAdvisor());
    return proxy;
}

@Bean
public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
}

@Bean
public TransactionAdvice transactionAdvice() {
    return new TransactionAdvice(transactionManager());
}

@Bean
public NameMatchMethodPointcut transactionPointcut() {
    NameMatchMethodPointcut nameMatchMethodPointcut = new NameMatchMethodPointcut();
    nameMatchMethodPointcut.setMappedName("upgrade*");
    return nameMatchMethodPointcut;
}

@Bean
public DefaultPointcutAdvisor transactionAdvisor() {
    return new DefaultPointcutAdvisor(transactionPointcut(), transactionAdvice());
}
```

어드바이저는 `addAdvisor()` 메서드를 통해 넣었다. 여러 개의 값을 넣을 수 있다. 만약 타깃이 모든 메서드에 적용해도 좋기 때문에 포인트컷 적용이 필요 없다면 transactionAdvice 라고 넣을 수도 있다.


#### 테스트

테스트 코드도 정리하자


```java
@Test
    @DirtiesContext
    public void upgradeAllOrNoting() {
    ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    TestUserService testUserService = new TestUserService(users.get(3).getId());
    testUserService.setUserDao(userDao);
    testUserService.setMailSender(mailSender);

    ProxyFactoryBean txProxyFactoryBean = new ProxyFactoryBean();
    txProxyFactoryBean.setTarget(testUserService);
    DefaultPointcutAdvisor advisor = context.getBean("transactionAdvisor", DefaultPointcutAdvisor.class);
    txProxyFactoryBean.addAdvisor(advisor);
    UserService txUserService = (UserService) txProxyFactoryBean.getObject();
    
    
    // ...
}
```

이제 스프링이 지원하는 `ProxyFactoryBean` 으로 전환을 마쳤다. UserServiceTest 를 실행하면 성공하는걸 확인할 수 있다.
`ProxyFactoryBean` 을 생성하고 타겟을 `testUserService` 로 설정해주고,  `ApplicationContext` 에서 `transactionAdvisor` 으로 등록한 빈의 정보를 가져와 어드바이저를 등록해주었다. 어드바이저에는 포인트컷과 어드바이스가 존재하므로 어떤 메서드에 어떤 기능을 추가해줄지에 대한 정보가 들어있다.

#### 어드바이스와 포인트컷의 재사용

`ProxyFactoryBean` 은 스프링의 DI와 템플릿/콜백 패턴, 서비스 추상화 등의 기법이 모두 적용된 것이다. 그 덕분에 독립적이며 여러 프록시가 공유할 수 있는 어드바이스와 포인트컷으로 확장 기능을 분리할 수 있었다.

이제 `UserService` 외에 새로운 비즈니스 로직을 담은 서비스 클래스가 만들어져도 이미 만들어둔 `TransactionAdvice` 를 그대로 재사용할 수 있다. 메서드 선정을 위한 포인트컷이 필요하면 이름 패턴만 지정해 `ProxyFactoryBean` 에 등록해주면 된다.

아래 그림은 `ProxyFactoryBean` 을 이용해 많은 수의 서비스 빈에게 트랜잭션 부가기능을 적용했을 때의 구조다.

<img width="558" alt="image" src="https://github.com/user-attachments/assets/a6f2eeb4-7a7e-43a6-8d1c-acb4d5d6dacc">