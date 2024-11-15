## 6.6 트랜잭션 속성

`PlatformTransactionManager`로 대표되는 스프링의 트랜잭션 추상화를 설명하면서 넘어간게 트랜잭션 매닞에서 트랜잭션을 가져올 때 사용한 `DefaultTransactionDefinition` 오브젝트다.

아래 나와 있는 `TransactionAdvice` 의 트랜잭션 경계설정 코드를 다시 살펴보자.

```java
@Override
public Object invoke(MethodInvocation invocation) throws Throwable {
    // 트랜잭션 시작? && 트랜잭션 정의
    TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

    try {
        Object ret = invocation.proceed();
        this.transactionManager.commit(status); // 트랜잭션 종료
        System.out.println("Committing transaction");
        return ret;
    } catch (RuntimeException e) {
        this.transactionManager.rollback(status); // 트랜잭션 종료
        System.out.println("Rolling back transaction due to: " + e.getClass());
        throw e;
    }
}
```

트랜잭션의 경계는 트랜잭션 매니저에게 트랜잭션을 가져오는 것과 `commit()`, `rollback()` 중의 하나를 호출하는 것으로 설정되고 있다. 트랜잭션을 시작(beginTransaction)한다고 하지 않고 트랜잭션을 가져온다(getTransaction)고 하는 이유는 차차 설명하기로 하고, 일단 트랜잭션을 가져올 때 파라미터로 트랜잭션 매니저에게 전달하는 `DefaultTransactionDefinition`의 용도가 무엇인지 알아보자.

### 6.6.1 트랜잭션 정의

트랜잭션이라고 모두 같은 방식으로 동작하는 것은 아니다. 물론 트랜잭션의 기본 개념인 더 이상 쪼갤 수 없는 최소 단위의 작업이라는 개념은 항상 유효하다. 따라서 트랜잭션 경계 안에서 진행된 작업은 `commit()`을 통해 모두 성공하든지 아니면 `rollback()` 을 통해 모두 취소돼야 한다. 그런데 이 밖에도 트랜잭션의 동작방식을 제어할 수 있는 몇 가지 조건이 있다.  
`DefaultTransactionDefinition` 이 구현하고 있는 `TransactionDefinition` 인터페이스는 트랜잭션의 동작방식에 영향을 줄 수 있는 네 가지 속성을 정의하고 있다.

#### 트랜잭션 전파

**트랜잭션 전파**란 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다. 아래의 그림의 트랜잭션 전파와 같이 각각 독립적인 트랜잭션 경계를 가진 두 개의 코드가 있다고 하자. 그런데 A의 트랜잭션이 시작되고 아직 끝나지 않은 시점에서 B를 호출했다면 B의 코드는 어떤 트랜잭션 안에서 동작할까?

<img width="339" alt="image" src="https://github.com/user-attachments/assets/4f265c44-6b25-40f8-a2a1-bd632b0f1a3f">

여러 가지 시나리오를 생각해볼 수 있다. A에서 트랜잭션이 시작돼서 진행 중이라면 B의 코드는 새로운 트랜잭션을 만들지 않고 A에서 이미 시작한 트랜잭션에 참여할 수 있다. 이 경우 B를 호출한 작업까지 마치고 (2)의 코드를 진행하던 중에 예외가 발생했다고 하자. 이 경우에는 A와 B의 코드에서 진행했던 모든 DB 작업이 다 취소된다. A와 B가 하나의 트랜잭션으로 묶여 있기 때문이다. 반대로 B의 트랜잭션은 이미 앞에서 시작한 A의 트랜잭션과 무관하게 독립적인 트랜잭션으로 만들 수 있다. 이 경우 B의 트랜잭션 경계를 빠져 나오는 순간 B의 트랜잭션은 독자적으로 커밋 또는 롤백되고, A 트랜잭션은 그에 영향 받지 않고 진행된다. 만약 이후에 A의 (2)에서 예외가 발생해서 A 트랜잭션이 롤백되더라도 B에서 이미 종료된 트랜잭션의 결과에는 영향을 주지 않는다.

이렇게 B와 같이 독자적인 트랜잭션 경계를 가진 코드에 대해 이미 진행 중인 트랜잭션이 어떻게 영향을 미칠 수 있는 가를 정의하는 것이 트랜잭션 전파 속성이다.

대표적으로 다음과 같은 트랜잭션 전파 속성을 줄 수 있다.

##### PROPAGATION_REQUIRED

가장 많이 사용되는 트랜잭션 전파 속성이다. 진행 중인 트랜잭션이 없으면 새로 시작하고, 이미 시작된 트랜잭션이 있으면 이에 참여한다. `PROPAGATION_REQUIRED` 트랜잭션 전파 속성을 갖는 코드는 다양한 방식으로 결합해서 하나의 트랜잭션으로 구성하기 쉽다. A와 B가 모두 `PROPAGATION_REQUIRED`로 선언되어 있다면, A, B, A -> B, B -> A와 같은 4가지 조합이 가능하다.

`DefaultTransactionDefinition`의 트랜잭션 전파 속성은 바로 이 `PROPAGATION_REQUIRED`다.

##### PROPAGATION_REQUIRES_NEW

항상 새로운 트랜잭션을 시작한다. 즉 앞에서 시작된 트랜잭션이 있든 없든 상관없이 새로운 트랜잭션을 만들어서 독자적으로 동작하게 된다. 독립적인 트랜잭션이 보장돼야 하는 코드에 적용할 수 있다.

##### PROPAGATION_NOT_SUPPORTED

이 속성을 사용하면 트랜잭션 없이 동작하도록 만들 수 있다. 진행 중인 트랜잭션이 있어도 무시한다. 트랜잭션 없이 동작하게 할 거라면 뭐하러 설정해두는 걸까?

이유는 트랜잭션 경계설정은 보통 AOP를 이용해 한 번에 많은 메서드에 동시에 적용하는 방법을 사용한다. 그런데 그 중에서 특별한 메서드만 트랜잭션 적용에서 제외하려면 어떻게 해야 할까? 물론 포인트컷을 잘 만들어서 특정 메서드가 AOP 적용 대상이 되지 않게 하는 방법도 있겠지만 포인트컷이 상당히 복잡해질 수 있다. 그래서 차라리 모든 메서드에 트랜잭션 AOP가 적용되도록 하고, 특정 메서드의 트랜잭션 전파 속성만 `PROPAGATION_NOT_SUPPORTED`로 설정해서 트랜잭션 없이 동작하게 만드는 편이 낫다.

이 외에도 다양한 트랜잭션 전파 속성을 사용할 수 있다. 트랜잭션 매니저를 통해 트랜잭션을 시작하려고 할 때 `getTransaction()` 이라는 메서드를 사용하는 이유는 바로 트랜잭션 전파 속성이 있기 때문이다. 트랜잭션 매니저의 `getTransaction()` 메서드는 항상 트랜잭션을 새로 시작하는 것이 아니다. 트랜잭션 전파 속성과 현재 진행 중인 트랜잭션이 존재하는지 여부에 따라 새로운 트랜잭션을 시작할 수도 있고, 이미 진행 중인 트랜잭션에 참여하기만 할 수도 있다. 진행 중인 트랜잭션에 참여하는 경우는 트랜잭션 경계의 끝에서 트랜잭션을 커밋시킺도 않는다. 최초로 트랜잭션을 시작한 경계까지 정상적으로 진행돼야 비로소 커밋될 수 있다.