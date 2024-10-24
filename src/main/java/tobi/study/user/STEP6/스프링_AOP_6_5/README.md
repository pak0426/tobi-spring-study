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
