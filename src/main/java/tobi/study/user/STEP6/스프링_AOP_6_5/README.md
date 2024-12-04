## 6.6 트랜잭션 속성

`PlatformTransactionManager`로 대표되는 스프링의 트랜잭션 추상화를 설명하면서 넘어간게 트랜잭션 매닞에서 트랜잭션을 가져올 때 사용한 `DefaultTransactionDefinition`
오브젝트다.

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

트랜잭션의 경계는 트랜잭션 매니저에게 트랜잭션을 가져오는 것과 `commit()`, `rollback()` 중의 하나를 호출하는 것으로 설정되고 있다. 트랜잭션을 시작(beginTransaction)한다고 하지 않고
트랜잭션을 가져온다(getTransaction)고 하는 이유는 차차 설명하기로 하고, 일단 트랜잭션을 가져올 때 파라미터로 트랜잭션 매니저에게 전달하는 `DefaultTransactionDefinition`의 용도가
무엇인지 알아보자.