### 6.6.1 트랜잭션 정의

트랜잭션이라고 모두 같은 방식으로 동작하는 것은 아니다. 물론 트랜잭션의 기본 개념인 더 이상 쪼갤 수 없는 최소 단위의 작업이라는 개념은 항상 유효하다. 따라서 트랜잭션 경계 안에서 진행된
작업은 `commit()`을 통해 모두 성공하든지 아니면 `rollback()` 을 통해 모두 취소돼야 한다. 그런데 이 밖에도 트랜잭션의 동작방식을 제어할 수 있는 몇 가지 조건이 있다.  
`DefaultTransactionDefinition` 이 구현하고 있는 `TransactionDefinition` 인터페이스는 트랜잭션의 동작방식에 영향을 줄 수 있는 네 가지 속성을 정의하고 있다.

#### 트랜잭션 전파

**트랜잭션 전파**란 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다. 아래의 그림의 트랜잭션 전파와 같이 각각 독립적인 트랜잭션 경계를 가진 두
개의 코드가 있다고 하자. 그런데 A의 트랜잭션이 시작되고 아직 끝나지 않은 시점에서 B를 호출했다면 B의 코드는 어떤 트랜잭션 안에서 동작할까?

<img width="339" alt="image" src="https://github.com/user-attachments/assets/4f265c44-6b25-40f8-a2a1-bd632b0f1a3f">

여러 가지 시나리오를 생각해볼 수 있다. A에서 트랜잭션이 시작돼서 진행 중이라면 B의 코드는 새로운 트랜잭션을 만들지 않고 A에서 이미 시작한 트랜잭션에 참여할 수 있다. 이 경우 B를 호출한 작업까지 마치고 (
2)의 코드를 진행하던 중에 예외가 발생했다고 하자. 이 경우에는 A와 B의 코드에서 진행했던 모든 DB 작업이 다 취소된다. A와 B가 하나의 트랜잭션으로 묶여 있기 때문이다. 반대로 B의 트랜잭션은 이미 앞에서
시작한 A의 트랜잭션과 무관하게 독립적인 트랜잭션으로 만들 수 있다. 이 경우 B의 트랜잭션 경계를 빠져 나오는 순간 B의 트랜잭션은 독자적으로 커밋 또는 롤백되고, A 트랜잭션은 그에 영향 받지 않고 진행된다.
만약 이후에 A의 (2)에서 예외가 발생해서 A 트랜잭션이 롤백되더라도 B에서 이미 종료된 트랜잭션의 결과에는 영향을 주지 않는다.

이렇게 B와 같이 독자적인 트랜잭션 경계를 가진 코드에 대해 이미 진행 중인 트랜잭션이 어떻게 영향을 미칠 수 있는 가를 정의하는 것이 트랜잭션 전파 속성이다.

대표적으로 다음과 같은 트랜잭션 전파 속성을 줄 수 있다.

##### PROPAGATION_REQUIRED

가장 많이 사용되는 트랜잭션 전파 속성이다. 진행 중인 트랜잭션이 없으면 새로 시작하고, 이미 시작된 트랜잭션이 있으면 이에 참여한다. `PROPAGATION_REQUIRED` 트랜잭션 전파 속성을 갖는 코드는
다양한 방식으로 결합해서 하나의 트랜잭션으로 구성하기 쉽다. A와 B가 모두 `PROPAGATION_REQUIRED`로 선언되어 있다면, A, B, A -> B, B -> A와 같은 4가지 조합이 가능하다.

`DefaultTransactionDefinition`의 트랜잭션 전파 속성은 바로 이 `PROPAGATION_REQUIRED`다.

##### PROPAGATION_REQUIRES_NEW

항상 새로운 트랜잭션을 시작한다. 즉 앞에서 시작된 트랜잭션이 있든 없든 상관없이 새로운 트랜잭션을 만들어서 독자적으로 동작하게 된다. 독립적인 트랜잭션이 보장돼야 하는 코드에 적용할 수 있다.

##### PROPAGATION_NOT_SUPPORTED

이 속성을 사용하면 트랜잭션 없이 동작하도록 만들 수 있다. 진행 중인 트랜잭션이 있어도 무시한다. 트랜잭션 없이 동작하게 할 거라면 뭐하러 설정해두는 걸까?

이유는 트랜잭션 경계설정은 보통 AOP를 이용해 한 번에 많은 메서드에 동시에 적용하는 방법을 사용한다. 그런데 그 중에서 특별한 메서드만 트랜잭션 적용에서 제외하려면 어떻게 해야 할까? 물론 포인트컷을 잘 만들어서
특정 메서드가 AOP 적용 대상이 되지 않게 하는 방법도 있겠지만 포인트컷이 상당히 복잡해질 수 있다. 그래서 차라리 모든 메서드에 트랜잭션 AOP가 적용되도록 하고, 특정 메서드의 트랜잭션 전파
속성만 `PROPAGATION_NOT_SUPPORTED`로 설정해서 트랜잭션 없이 동작하게 만드는 편이 낫다.

이 외에도 다양한 트랜잭션 전파 속성을 사용할 수 있다. 트랜잭션 매니저를 통해 트랜잭션을 시작하려고 할 때 `getTransaction()` 이라는 메서드를 사용하는 이유는 바로 트랜잭션 전파 속성이 있기
때문이다. 트랜잭션 매니저의 `getTransaction()` 메서드는 항상 트랜잭션을 새로 시작하는 것이 아니다. 트랜잭션 전파 속성과 현재 진행 중인 트랜잭션이 존재하는지 여부에 따라 새로운 트랜잭션을 시작할
수도 있고, 이미 진행 중인 트랜잭션에 참여하기만 할 수도 있다. 진행 중인 트랜잭션에 참여하는 경우는 트랜잭션 경계의 끝에서 트랜잭션을 커밋시킺도 않는다. 최초로 트랜잭션을 시작한 경계까지 정상적으로 진행돼야
비로소 커밋될 수 있다.

#### 격리 수준

모든 DB 트랜잭션은 **격리수준**을 갖고 있다. 서버 환경에서는 여러 개의 트랜잭션이 동시에 진행될 수 있다. 가능하다면 모든 트랜잭션이 순차적으로 진행돼서 다른 트랜잭션의 작업에 독립적인 것이 좋겠지만,
그러자면 성능이 크게 떨어질 수 밖에 없다. 따라서 적절하게 격리수준을 조정해 많은 트랜잭션을 동시에 진행시키면서 문제가 발생하지 않게 하는 제어가 필요하다.

격리 수준은 기본적으로 DB에 설정되어 있지만 JDBC 드라이버나 `DataSource` 등에서 재설정할 수 있고, 필요하다면 트랜잭션 단위로 격리 수준을 조정할 수
있다. `DefaultTransactionDefinition`에 설정된 격리수준은 `ISOLATION_DEFAULT` 다. 이는 `DataSource`에 설정되어 있는 디폴트 격리수준을 그대로 따른다는 뜻이다.
기본적으로 DB나 `DataSource`에 설정된 디폴트 격리 수준을 따르는 것이 좋지만, 특별한 작업을 수행하는 메서드의 경우는 독자적인 격리수준을 지정할 필요가 있다.

#### 제한시간

트랜잭션을 수행하는 제한시간을 설정할 수 있다. `DefaultTransactionDefinition`의 기본 설정은 제한시간이 없는 것이다. 제한시간은 트랜잭션을 직접 시작할 수
있는 `PROPAGATION_REQUIRED`나 `PROPAGATION_REQUIRES_NEW`와 함게 사용해야만 의미가 있다.

#### 읽기 전용

읽기전용(read only)으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄 수 있다. 또한 데이터 액세스 기술에 따라 성능이 향상될 수도 있다.

`TransactionDefinition` 타입 오브젝트를 사용하면 4가지 속성을 이용하여 제어할 수 있다.

트랜잭션 정의를 수정하려면 `TransactionDefinition` 오브젝트를 생성하고 사용하는 코드는 트랜잭션 경계설정 기능을 가진 `TransactionAdvice`다. 트랜잭션 정의를 바꾸고 싶다면 디폴트
속성을 갖고 있는 `DefaultTransactionDefinition`을 사용하는 대신 외부에서 정의된 `TransactionDefinition` 오브젝트를 DI 받아서 사용하도록 만들면
된다. `TransactionDefinition` 오브젝트를 DI 받아서 사용하도록 만들면 된다. `TransactionDefinition` 타입의 빈을 정의해두면 프로퍼티를 통해 원하는 속성을 지정해줄 수 있다.
하지만 이 방법으로 트랜잭션 속성을 변경하면 `TransactionAdvice`를 사용하는 모든 트랜잭션의 속성이 한꺼번에 바뀐다는 문제가 있다. 원하는 메서드만 선택해서 독자적인 트랜잭션 정의를 적용할 수 없을까?

### 6.6.2 트랜잭션 인터셉터와 트랜잭션 속성

메서드별로 다른 트랜잭션 정의를 적용하려면 어드바이스의 기능을 확장해야 한다. 마치 초기에 `TransactionHandler` 에서 메서드 이름을 이용해 트랜잭션 적용 여부를 판단했던 것과 비슷한 방식을 사용하면
된다. 메서드 이름 패턴에 따라 다른 트랜잭션 정의가 적용되도록 만드는 것이다.

#### TransactionInterceptor

이를 위해 기존에 만들었던 `TransactionAdvice`를 다시 설계할 필요는 없다. 이미 스프링에는 편리하게 트랜잭션 경계설정 어드바이스로 사용할 수 있도록
만들어진 `TransactionInterceptor`가 존재하기 때문이다. `TransactionAdvice`는 어드바이스의 동작원리를 알아보려고 만들었던 것이므로 그만 사용하고, 이제부터
스프링의 `TransactionInterceptor`를 이용해보자.

`TransactionInterceptor` 어드바이스의 동작방식은 기존에 만들었던 `TransactionAdvice`와 다르지 않다. 다만 트랜잭션 정의를 메서드 이름 패턴을 이용해 다르게 지정할 수 있는 방법을
추가로 제공해준다. `TransactionInterceptor`는 `PlatformTransactionManager`와 `Properties` 타입의 두 가지 프로퍼티를 갖고 있다. 트랜잭션 매니저 프로퍼티는 잘
알고 있지만 `Properties` 타입의 프로퍼티는 처음 보는 것이다.

`Properties` 타입인 두 번째 프로퍼티 이름은 `transactionAttributes`로, 트랜잭션 속성을 정의한 프로퍼티다. 트랜잭션 속성은 `TransactionDefinition`의 네 가지 기본
항목 `rollbackOn()` 이라는 메서드를 하나 더 갖고 있는 `TransactionAttribute` 인터페이스로 정의된다. `rollbackOn()` 메서드는 어떤 예외가 발생하면 롤백을 할 것인가를
결정하는 메서드다. 이 `TransactionAttribute`를 이용하면 트랜잭션 부가기능의 동작방식을 모두 제어할 수 있다.

아래의 `TransactionAdvice` 경계설정 코드를 다시 살펴보면 트랜잭션 부가기능의 동작방식을 변경할 수 있는 곳이 두 군데 있다는 사실을 알 수 있다.

```java
public Object invoke(MethodInvocation invocation) throws Throwable {
    TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition()); // 트랜잭션 정의를 통한 4가지 조건

    try {
        Object ret = invocation.proceed(); // 롤백 대상인 예외 종류
        this.transactionManager.commit(status);
        System.out.println("Committing transaction");
        return ret;
    } catch (RuntimeException e) {
        this.transactionManager.rollback(status);
        System.out.println("Rolling back transaction due to: " + e.getClass());
        throw e;
    }
}
```

`TransactionAdvice`는 `RuntimeException`이 발생하는 경우에만 트랜잭션을 롤백시킨다. 하지만 런타임 예외가 아닌 경우에는 트랜잭션이 제대로 처리되지 않고 메서드를 빠져나가게 되어
있다. `UserService`는 런타임 예외만 던진다는 사실을 알기 때문에 일단 이렇게 정의해도 상관없지만, 체크 예외를 던지는 타깃에 사용한다면 문제가 될 수 있다. 그렇다면 런타임 예외만이 아니라 모든 종류의
예외에 대해 트랜잭션을 롤백시키도록 해야할 까? 안된다. 비즈니스상의 예외 경우를 나타내기 위해 타깃 오브젝트가 체크 예외를 던지는 경우에는 DB 트랜잭션은 커밋시켜야 하기 때문이다. 2장에서 봤듯 일부 체크 예외는
정상적인 작업 흐름 안에서 사용될 수도 있다.

스프링이 제공하는 `TransactionInterceptor`에는 기본적으로 두 가지 종류의 예외처리 방식이 있다. 런타임 예외가 발생하면 트랜잭션은 롤백된다. 반면에 타깃 메서드가 런타임 예외가 아닌 체크 예외를
던지는 경우에는 이것을 예외 상황이라고 해석하지 않고 트랜잭션을 커밋한다. 스프링의 기본적인 예외처리 원칙에 따라 비즈니스적인 의미가 있는 예외 상황에만 체크 예외를 사용하고 그 외의 모든 복구 불가능한 순수한
예외의 경우 런타임 예외로 포장돼서 잔달하는 방식을 따른다고 가정하기 때문이다.

그런데 `TransactionInterceptor`의 이러한 예외처리 기본 원칙을 따르지 않는 경우가 있을 수 있다. 그래서 `TransactionAttribute` 는 `rollbackOn()` 이라는 속성을 둬서
기본 원칙과 다른 예외처리가 가능하게 해준다. 이를 활용하면 특정 체크 에외의 경우는 트랜잭션을 롤백시키고, 특정 런타임 예외에 대해서는 트랜잭션을 커밋 시킬 수 있다.

#### 메서드 이름 패턴을 이용한 트랜잭션 속성 지정

Properties 타입의 transactionAttributes 프로퍼티는 메서드 패턴과 트랜잭션 속성을 키와 값으로 갖는 컬렉션이다. 트랜잭션 속성은 다음과 같은 문자열로 정의할 수 있다.

```
PROPAGATION_NAME: 트랜잭션 전파방식, 필수항목이다. PROPAGATION_ 으로 시작한다.
ISOLATION_NAME: 격리 수준. ISOLATION_ 으로 시작한다. 생략 가능하다. 생략되면 디폴트 격리 수준으로 지정된다.
readOnly: 읽기 전용 항목, 생략 가능하다. 디폴트는 읽기전용이 아니다.
timeout_NNNN: 제한시간. timeout_ 으로 시작하고 초 단위 시간을 뒤에 붙인다. 생략 가능하다.
-Exception1: 체크 예외 중에서 롤백 대상으로 추가할 것을 넣는다. 한 개 이상을 등록할 수 있다.
+Exception2: 런타임 예외지만 롤백시키지 않을 예외들을 넣는다. 한 개 이상 등록할 수 있다.
```

이 중에서 트랜잭션 전파 항목만 필수이고 나머지는 다 생략 가능하다. 생략하면 모두 `DefaultTransactionDefinition`에 설정된 디폴트 속성이 부여된다. 모든 항목이 구분 가능하기 때문에 순서는 바꿔도 상관없다. 이 중에서 + 또는 -로 시작하는 건 기본 원칙을 따르지 않는 예외를 정의해주는 것이다. 모든 런타임 예외는 롤백돼야 하지만 `+XXXRuntimeException` 이라고 해주면 런타임 예외라도 커밋하게 만들 수 있다. 반대로 체크 예외는 모두 커밋하는 것이 기본 처리 방식이지만 -를 붙여서 넣어주면 트랜잭션은 롤백 대상이 된다.

이렇게 속성을 하나의 문자열로 표한하게 만든 이유는 트랜잭션 속성을 메서드 패턴에 따라 여러 개를 지정해줘야 하는데, 일일이 중첩된 태그와 프로퍼티를 설정하게 만들면 번거롭기 때문이다. 또, 대부분은 디폴트를 사용해도 충분하므로 생략 가능하다는 점도 한 가지 이유다.

아래는 메서드 이름 패턴과 문자열로 된 트랜잭션 속성을 이용해서 정의한 `TransactionInterceptor` 타입 빈의 예다.

<img width="642" alt="image" src="https://github.com/user-attachments/assets/1d812d2c-5a2a-4331-a2a7-0a3bfb1df856">

세 가지 메서드 이름 패턴에 대한 트랜잭션 속성이 정의되어 있다.

첫 번째는 이름이 get으로 시작하는 메서드에 대한 속성이다. `PROPAGATION_REQUIRED` 이면서 읽기전용이고 시간제한은 30초다. 보통 읽기전용 메서드는 get 또는 find 같은 일정한 이름으로 시작한다. 명명 규칙을 잘 정해두면 조회용 메서드의 트랜잭션은 읽기전용으로 설정해서 성능을 향상시킬 수 있다.

그런데 읽기전용이 아닌 트랜잭션 속성을 가진 메서드에서 읽기전용 속성을 가진, get으로 시작하는 메서드를 호출하면 어떨까? get 메서드는 PROPAGATION_REQUIRED 이기 때문에 다른 트랜잭션이 시작되어 있으면 그 트랜잭션에 참여한다. 그렇다면 이미 DB에 쓰기 작업이 진행된 채로 읽기전용 트랜잭션 속성을 가진 작업이 뒤따르게 돼서 충돌이 일어나진 않을까? 그렇지는 않다. 다행히도 트랜잭셩 속성 중 readOnly나 timeout 등은 트랜잭션이 처음 시작될 때가 아니라면 적용되지 않는다. 따라서 get으로 시작하는 메서드에서 트랜잭션을 시작하는 경우라면 읽기전용에 제한시간이 적용되지만 그 외의 경우에는 진행 중인 트랜잭션의 속성을 따르게 되어 있다.

두 번째 upgrade로 시작하는 메서드는 항상 독립적인 트랜잭션으로 동작하는 트랜잭션 전파 항목을 `PROPAGATION_REQUIRES_NEW`로 설정했다. 또, 다른 동시 작업에 영향을 받지 않도록 완벽하게 고립된 상태에서 트랜잭션이 동작하도록 격리수준을 최고 수준인 `ISOLATION_SERIALIZABLE`로 설정했다.

세 번째는 *만 사용해서 위의 두 가지 조건에 해당하지 않는 나머지 모든 메서드에 사용될 속성을 지정했다. 필수 항목인 `PROPAGATION_REQUIRED`만 지정하고 나머지 디폴트 설정을 따르게 했다.

때로는 메서드 이름이 하나 이상의 패턴과 일치하는 경우가 있다. 이 때는 메서드 이름 패턴 중에서 가장 정확히 일치하는 것이 적용된다. 이렇게 메서드 이름 패턴을 사용하는 트랜잭션 속성을 활용하면 하나의 트랜잭션 어드바이스를 정의하는 것만으로도 다양한 트랜잭션 설정이 가능해진다.

#### tx 네임스페이스를 이용한 설정 방법

`TransactionInterceptor` 타입의 어드바이스 빈과 `TransactionAttribute` 타입의 속성 정보도 tx 스키마의 전용 태그를 이용해 정의할 수 있다. 트랜잭션 어드바이스도 포인트컷이나 어드바이저만큼 자주 사용되고, 애플리케이션의 컴포넌트가 아닌 컨테이너가 사용하는 기반기술 설정의 한 가지이기 때문이다.

`TransactionInterceptor` 빈으로 정의한 트랜잭션 어드바이스와 메서드 패턴에 따른 트랜잭션 속성 지정은 tx 스키마의 태그를 이용해 아래와 같이 간단히 정의할 수 있다.

<img width="469" alt="image" src="https://github.com/user-attachments/assets/bd6c84bb-ef4a-4488-8a79-7e354c8333b6">
<img width="447" alt="image" src="https://github.com/user-attachments/assets/2d15e3bb-a213-48a4-8b90-1a35fa5a50c6">

트랜잭션 속성이 개별 애트리뷰트를 통해 지정될 수 있으므로 설정 내용을 읽기가 좀 더 쉽고, XML 에디터의 자동완성 기능을 통해 편하게 작성할 수 있다. 문자열로 입력할 때 자주 발생하는 오타 문제도 XML 스키마에 미리 등록해둔 값을 통해 검증할 수 있어 편리하다. <bean> 태그로 등록하는 경우에 비해 장점이 많으므로 tx 스키마의 태그를 사용해 어드바이스를 등록하도록 권장한다.

### 6.6.3 포인트컷과 트랜잭션 속성의 적용 전략

트랜잭션 부가기능을 적용할 후보 메서드를 선정하는 작업은 포인트컷에 의해 진행된다. 그리고 어드바이스의 트랜잭션 전파 속성에 따라 메서드별로 트랜잭션의 적용 방식이 결정된다. aop와 tx 스키마의 전용 태그를 사용하면 애플리케이션의 어드바이저, 어드바이스, 포인트컷 기본 설정 방법은 바뀌지 않을 것이다. expression 애트리뷰트에 넣는 포인트컷 표현식과 `<tx:attributes>`로 정의하는 트랜잭션 속성만 결정하면 된다.  
포인트컷 표현식과 트랜잭션 속성을 정의할 때 따르면 좋은 몇 가지 전략을 생각해보자.

#### 트랜잭션 포인트컷 표현식은 타입 패턴이나 빈 이름을 이용한다

일반적으로 트랜잭션을 적용할 타깃 클래스의 메서드는 모두 트랜잭션 적용 후보가 되는 것이 바람직하다. 지금까지는 포인트컷의 메서드 선정 기능을 살펴보기 위해 `UserService` 의 `upgradeLevels()` 메서드 하나에만 트랜잭션이 적용되게 해왔다. 하지만 이렇게 비즈니스 로직을 담고 있는 클래스라면 메서드 단위까지 세밀하게 포인트컷을 정의해줄 필요는 없다.

`UserService`의 `add()` 메서드도 트랜잭션 적용 대상이어야 한다. 사용자 등록에 무슨 트랜잭션이 필요할까 싶겠지만, 트랜잭션 전파 방식을 생각해보면 `add()` 는 다른 트랜잭션에 참여할 가능성이 높다. `add()` 메서드는 `UserDao.add()` 를 호출해서 사용자 정보를 DB에 추가하는 것 외에도 DB의 정보를 다루는 작업이 추가될 가능성이 높다. 따라서 `add()` 메서드는 트랜잭션 안에서 동작해야 바람직하다.

쓰기 작업이 없는 단순 조회 작업 메서드에도 모두 트랜잭션을 적용하는 게 좋다. 조회의 경우 읽기전용으로 트랜잭션 속성을 설정해두면 성능 향상을 이룰 수 있다. 또 복잡한 조회의 경우 제한시간을 지정할 수 있고, 격리 수준에 따라 조회도 반드시 트랜잭션 안에서 진행해야 할 필요가 발생하기도 한다.

따라서 트랜잭션용 포인트컷 표현식에는 메서드나 파라미터, 예외에 대한 패턴을 정의하지 않는 게 바람직하다. 트랜잭션의 경계로 삼을 클래스들이 선정됐다면, 그 클래스들이 모여 있는 패키지를 통째로 선택하거나 클래스 이름에서 일정한 패턴을 찾아서 표현식으로 만들면 된다. 관례적으로 비즈니스 로직 서비스를 담당하는 클래스 이름은 `Service`, `ServiceImpl`로 끝나는 경우가 많은데 이런 경우엔 `execution(**..*ServiceImpl.*(..))`과 같이 포인트컷을 정의하면 된다. 인터페이스는 클래스에 비해 변경 빈도가 적고 일정 패턴을 유지하기 쉽기 때문이다.

메서드 시그니처를 이용한 `execution()` 방식의 포인트컷 표현식 대신 스프링의 빈 이름을 이용하는 bean() 표현식을 사용하는 방법도 고려해볼 만하다. bean() 표현식은 빈 이름을 기준으로 선정하기 때문에 클래스나 인터페이스 이름에 일정한 규칙을 만들기가 어려운 경우에 유용하다. 포인트컷 표현식 자체가 간단해서 읽기 편하다는 장점도 있다. 빈의 아이디가 Service로 끝나는 모든 빈에 대해 트랜잭션을 적용하고 싶다면 포인트컷 표현식을 `bean(*Service)` 라고 하면 된다. 이름이 비슷한 다른 빈이 있는 경우 주의해야 한다.

그 외에 애노테이션을 이용한 포인트컷 표현식을 만드는 방법이 있는데 다음에 살펴보자.

#### 공통된 메서드 이름 규칙을 통해 최소한의 트랜잭션 어드바이스와 속성을 정의한다

실제로 하나의 애플리케이션에서 사용할 트랜잭션 속성의 종류는 그다지 다양하지 않다. 너무 다양하게 트랜잭션 속성을 부여하면 관리만 힘들어질 뿐이다. 따라서 기준이 되는 몇 가지 트랜잭션 속성을 정의하고 그에 따라 적절한 메서드 명명 규칙을 만들어두면 하나의 어드바이스만으로 애플리케이션의 모든 서비스 빈에 트랜잭션 속성을 지정할 수 있다.

그런데 가끔 트랜잭션 속성의 적용 패턴이 일반적인 경우와 크게 다른 오브젝트가 존재하기도 한다. 이런 예외적인 경우는 트랜잭션 어드바이스와 포인트컷을 새롭게 추가해줄 필요가 있다.

가장 간단한 트랜잭션 속성 부여 방법은 다음과 같이 모든 메서드에 디폴트 속성을 지정하는 것이다. 일단 트랜잭션 속성의 종류와 메시지 패턴이 결정되지 않았으면 아래와 같이 가장 단순한 디폴트 속성으로부터 출발하면 된다. 개발이 진행됨에 따라 단계적으로 속성을 추가해주면 된다.

<img width="435" alt="image" src="https://github.com/user-attachments/assets/7e73fc9f-8ba2-4748-99f5-c1089f48d9c3">

디폴트 속성을 일괄적으로 부여한 것에서 한 단계 더 나아가면 아래와 같이 간단한 이름 패턴을 적용할 수 있다.

<img width="496" alt="image" src="https://github.com/user-attachments/assets/0e6cea5d-ecd1-4423-a5a3-2af89964d960">

트랜잭션 적용 대상 클래스의 메서드는 일정한 명명 규칙을 따르게 해야 한다. 일반화하기에 적당하지 않은 특별한 트랜잭션 속성이 필요한 타깃 오브젝트에 대해서는 별도의 어드바이스와 포인트컷 표현식을 사용하는 편이 좋다.

아래는 두 개의 포인트컷과 어드바이스를 적용한 예다. 비즈니스 로직을 정의한 서비스 빈에는 기본적으로 메서드 이름 패턴을 따르는 트랜잭션 속성을 지정한다. 반면에 트랜잭션의 성격이 많이 다른 배치 작업용 클래스를 위해서는 트랜잭션 어드바이스를 별도로 정의해서 독자적인 트랜잭션 속성을 지정해준다.

<img width="613" alt="image" src="https://github.com/user-attachments/assets/70abb464-bc9a-46ed-bf11-3c76ab525c43">

#### 프록시 방식 AOP는 같은 타깃 오브젝트 내의 메서드를 호출할 때는 적용되지 않는다.

이건 주의사항이다. 프록시 방식의 AOP에서는 프록시를 통한 부가기능 적용은 클라이언트로부터 호출이 일어날 때만 가능하다. 여기서 클라이언트는 인터페이스를 통해 타깃 오브젝트를 사용하는 다른 모든 오브젝트를 말한다. 반대로 타깃 오브젝트가 자기 자신의 메서드를 호출할 때는 프록시를 통한 부가기능의 적용이 일어나지 않는다. 프록시가 적용되는 방식을 생각해보면 왜 그런지 알 수 있다. 아래는 트랜잭션 프록시가 타깃에 적용되어 있는 경우의 메서드 호출과정이다. `delete()` 와 `update()`는 모두 트랜잭션 적용 대상인 메서드다. 따라서 [1]과 [3] 이 전달되므로 트랜잭션 경계설정 부가기능이 부여될 것이다.

<img width="605" alt="image" src="https://github.com/user-attachments/assets/dfbb0754-f106-420d-863c-22b5273cc290">

하지만 [2]의 경우는 다르다. **일단 타깃 오브젝트 내로 들어와 타깃 오브젝트의 다른 메서드를 호출하는 경우에는 프록시를 거치지 않고 직접 타깃의 메서드가 호출된다.** 따라서 [1] 클라이언트를 통해 호출된 update() 메서드에 지정된 트랜잭션 속성이 적용되지만, [2]를 통해 update() 메서드가 호출될 때는 update() 메서드에 지정된 트랜잭셔 속성이 전혀 반영되지 않는다.

만약 update() 메서드에 대해 트랜잭션 전파 속성을 `REQUIRES_NEW` 라고 했더라도 `delete()` 메서드를 통해 `update()` 가 호출되면 트랜잭션 전파 속성이 적용되지 않으므로 `REQUIRES_NEW`는 무시되고 프록시의 `delete()` 메서드에서 시작한 트랜잭션에 단순하게 참여하게 될 뿐이다. 또는 트랜잭션이 아예 적용되지 않는 타깃의 다른 메서드에서 update() 가 호출된다면 그때는 트랜잭션이 없는 채로 update() 메서드가 실행될 것이다.

이렇게 같은 타깃 오브젝트 안에서 메서드 호출이 이렁나는 경우에는 프록시 AOP를 통해 부여해준 부가기능이 적용되지 않는다는 점을 주의해야 한다.

##### 프록시 AOP 내부 메서드 호출문제 해결 방법

하나는 스프링 API를 이용해 프록시 오브젝트에 대한 레퍼런스를 가져온 뒤에 같은 오브젝트의 메서드 호출도 프록시를 이용하도록 강제하는 방법이다. 하지만 복잡한 과정을 거쳐 순수한 비즈니스 로직만을 남겨두려고 해는데 거기에 스프링 API와 프록시 호출 코드가 등장하는건 바람직하지 않다.

다른 방법은 AspectJ와 같은 타깃의 바이트코드를 직접 조작하는 AOP를 적용하는 것이다. 스프링은 프록시 기반의 AOP를 기본적으로 사용하고 있지만 필요에 따라 언제든 AspectJ 방식으로 변경할 수 있다. 지금까지 검토했던 설정은 그대로 둔 채 간단한 옵션을 바꿈으로써 AspectJ 방식으로 트랜잭션 AOP가 적용되게 할 수 있다. 하지만 그만큼 불편도 따르기 때문에 꼭 필요한 경우에만 사용해야 한다.

### 6.6.4 트랜잭션 속성 적용

트랜잭션 속성과 그에 트랜잭션 전략을 `UserService` 에 적용해보자. 지금까지 살펴봤던 몇 가지 원칙과 전략에 따라 작업을 진행할 것이다.

#### 트랜잭션 경계설정의 일원화

트랜잭션 경계 설정의 부가기능을 여러 계층에서 중구난방으로 적용하는 건 좋지 않다. 일반적으로 트겆ㅇ 계층의 경계를 트랜잭션 경계와 일치하는 것이 바람직하다. 비즈니스 로직을 담고 있는 서비스 계층 오브젝트의 메서드가 트랜잭션 경계를 부여하기에 가장 적절한 대상이다.

서비스 계층을 트랜잭션이 시작되고 종료되는 경계로 정했다면, 테스트와 같은 특별한 이유가 아니고는 다른 계층이나 모듈에서 DAO에 직접 접근하는 것은 차단해야 한다. 트랜잭션은 보통 서비스 계층의 메서드 조합을 통해 만들어지기 때문에 DAO가 제공하는 주요 기능은 서비스 계층에 위임 메서드를 만들어둘 필요가 있다. 가능하면 다른 모듈의 DAO에 접근할 때는 서비스 계층을 거치도록 만들어둘 필요가 있다. 가능하면 다른 모듈의 DAO에 접근할 때는 서비스 계층을 거치도록 하는 게 바람직하다. 그래야만 UserService의 add()처럼 부가 로직을 적용할 수도 있고, 트랜잭션 속성도 제어할 수 있기 때문이다. 예를 들어 `UserService`가 아니라면 UserDao에 직접 접근하지 않고 `UserService`의 메서드를 이용하는 편이 좋다. 물론 순수한 조회나 간단한 수정이라면 `UserService` 외의 서비스 계층 오브젝트에서 `UserDao`를 직접 사용해도 상관없다. 하지만 등록이나 수정, 삭제가 포함된 작업이라면 다른 모듈의 DAO를 직접 이용할 때 신중을 기해야 한다. 안전하게 사용하려면 다른 모듈의 서비스 계층을 통해 접근하는 방법이 좋다.

아키텍처를 단순하게 가져가면 서비스 계층과 DAO가 통합될 수도 있다. 비즈니스 로직이 거의 없고 단순 DB 입출력과 검색 수준의 조회가 전부라면 서비스 계층을 없애고 DAO를 트랜잭션 경계로 만드는 것이다. 하지만 비즈니스 로직을 독자적으로 두고 테스트하려면 서비스 계층을 만들어 사용해야 한다.

`UserDao` 인터페이스에 정의된 6개의 메서드 중에서 이미 서비스 계층에 부가적인 로직을 담아서 추가한 `add()`를 제외한 나머지 5개가 UserService에 새로 추가할 후보 메서드다. 이 중에서 단순히 레코드 개수를 리턴하는 `getCount()`를 제외하면 나머지는 독자적인 트랜잭션을 가지고 사용될 가능성이 높다. 따라서 이 4개의 메서드를 아래와 같이 추가한다.

```java
public interface UserService {
    void add(User user);
    
    // 신규 추가 메서드
    // DAO 메서드와 1:1 대응되는 CRUD 메서드이지만 add() 처럼 단순 위임 이상의 로직을 가질 수 있다.
    List<User> getAll();
    void deleteAll();
    void update(User user);
    
    void upgradeLevels();
}
```

다음은 아래처럼 `UserServiceImpl` 클래스에 추가된 메서드 구현 코드를 넣어준다.

```java
public class UserServiceImpl implements UserService {

    private UserDao userDao;
    
    // ...

    // DAO에 위임하도록 한다. 꼭 필요한 부가로직을 넣어도 된다.
    @Override
    public User get(String id) {
        return userDao.get(id);
    }

    @Override
    public List<User> getAll() {
        return userDao.getAll();
    }

    @Override
    public void deleteAll() {
        userDao.deleteAll();
    }

    @Override
    public void update(User user) {
        userDao.update(user);
    }
}
```

이제 모든 User 관련 데이터 조작은 `UserService`라는 트랜잭션 경계를 통해 진행할 경우 모두 트랜잭션을 적용할 수 있게 됐다.

#### 서비스 빈에 적용되는 포인트컷 표현식 등록

`upgradeLevels()` 에만 트랜잭션이 적용되게 했던 기존 포인트컷 표현식을 모든 비즈니스 로직의 서비스 빈에 적용되도록 수정한다.

```java
@Bean
public AspectJExpressionPointcut transactionPointcut() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    String expression = "bean(*Service)";
    pointcut.setExpression(expression);
    return pointcut;
}
```

이제 아이디가 Service로 끝나는 모든 빈에 `transactionAdvice` 빈의 부가기능이 적용될 것이다.

#### 트랜잭션 속성을 가진 트랜잭션 어드바이스 등록

다음은 `TransactionAdvice` 클래스로 정의했던 어드바이스 빈을 스프링의 `TransactionInterceptor`를 이용하도록 변경한다. 메서트 패턴과 트랜잭션 속성은 가장 보편적인 방법인 get으로 시작하는 메서드는 읽기 전용 속성을 두고 나머지는 디폴트 트랜잭션 속성을 따르도록 설정한다. 다음과 같이 만들면 된다.

```java
@Bean
public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
}
@Bean
public TransactionInterceptor transactionAdvice() {
    TransactionInterceptor txInterceptor = new TransactionInterceptor();
    txInterceptor.setTransactionManager(transactionManager());

    Properties txAttributes = new Properties();
    txAttributes.setProperty("get*", "PROPAGATION_REQUIRED,readOnly");
    txAttributes.setProperty("*", "PROPAGATION_REQUIRED");

    txInterceptor.setTransactionAttributes(txAttributes);
    return txInterceptor;
}

@Bean
public AspectJExpressionPointcut transactionPointcut() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    String expression = "bean(*Service)";
    pointcut.setExpression(expression);
    return pointcut;
}

@Bean
public DefaultPointcutAdvisor transactionAdvisor() {
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
    advisor.setPointcut(transactionPointcut());
    advisor.setAdvice(transactionAdvice());
    return advisor;
}

@Bean(name = "transactionAdvice")
public TransactionInterceptor txAdvice() {
    NameMatchTransactionAttributeSource txAttributeSource = new NameMatchTransactionAttributeSource();

    // get* 메소드에 대한 트랜잭션 속성 설정
    RuleBasedTransactionAttribute readOnlyTx = new RuleBasedTransactionAttribute();
    readOnlyTx.setReadOnly(true);
    readOnlyTx.setPropagationBehavior(Propagation.REQUIRED.value());

    // 나머지 메소드에 대한 트랜잭션 속성 설정
    RuleBasedTransactionAttribute requiredTx = new RuleBasedTransactionAttribute();
    requiredTx.setPropagationBehavior(Propagation.REQUIRED.value());

    // 트랜잭션 속성 맵핑
    Map<String, TransactionAttribute> txMethods = new HashMap<>();
    txMethods.put("get*", readOnlyTx);
    txMethods.put("*", requiredTx);

    txAttributeSource.setNameMap(txMethods);

    return new TransactionInterceptor(transactionManager(), txAttributeSource);
}

@Bean
public Advisor txAdvisor() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* *..*Service.*(..))");

    return new DefaultPointcutAdvisor(pointcut, txAdvice());
}
```

이쯤에서 테스트를 수행해서 확인해보자.

<img width="308" alt="image" src="https://github.com/user-attachments/assets/bd627a91-9c7e-4ded-beeb-73206fb657a0">