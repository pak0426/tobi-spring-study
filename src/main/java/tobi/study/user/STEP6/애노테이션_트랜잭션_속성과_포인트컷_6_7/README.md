## 6.7 애노테이션 트랜잭션 속성과 포인트컷

포인트컷 표현식과 트랜잭션 속성을 이용해 트랜잭션을 일괄적으로 적용하는 방식은 복잡한 트랜잭션 속성이 요구되지 않는 한 대부분의 상황에 잘 들어맞는다. 그런데 가끔은 클래스나 메서드에 따라 제각각 속성이 다른, 세밀하게 튜닝된 트랜잭션 속성을 적용해야 하는 경우도 있다. 이런 경우라면 메서드 이름 패턴을 이용해 일괄적으로 트랜잭션 속성을 부여하는 방식은 적합하지 않다. 기본 속성과 다른 경우가 있을 때마다 일일이 포인트컷과 어드바이스를 새로 추가해줘야 하기 때문이다. 포인트컷 자체가 지저분해지고 설정파일도 복잡해지기 쉽다.

이런 세밀한 트랜잭션 속성의 제어가 필요한 경우를 위해 스프링이 제공하는 다른 방법이 있다. 설정 파일에서 패턴으로 분류 가능한 그룹을 만들어서 일괄적으로 속성을 부여하는 대신에 직접 타깃에 트랜잭션 속성정보를 가진 어노테이션을 지정하는 방법이다.

### 6.7.1 트랜잭션 어노테이션

타깃에 부여할 수 있는 트랜잭션 어노테이션은 다음과 같이 정의되어 있다. 스프링 3.0은 자바 5에서 등장한 어노테이션을 많이 사용한다. 필요에 따라선 어노테이션 정의를 읽고 그 내용과 특징을 이해할 수 있도록 어노테이션 정의에 사용되는 주요 메타어노테이션을 알고 있어야 한다.

#### @Transactional

아래는 `@Transactional` 어노테이션을 정의한 코드다.

```java
@Target({ElementType.TYPE, ElementType.METHOD}) // 어노테이션을 사용할 대상을 지정. 여기선 '메서드와 타입(클래스, 인터페이스)' 처럼 한 개 이상의 대을 지정할 수 있다.
@Retention(RetentionPolicy.RUNTIME) // 어노테이션 정보가 언제까지 유지되는지를 지정. 이렇게 설정하면 런타임 때도 어노테이션 정보를 리플렉션을 통해 얻을 수 있다.
@Inherited // 상속을 통해서도 어노테이션 정보를 얻을 수 있게 한다.
@Documented
@Reflective
public @interface Transactional {
    // 아래 메서드들은 트랜잭션 속성의 모든 항목을 엘리먼트로 지정할 수 있다.
    // 디폴트 값이 설정되어 있으므로 모두 생략 가능하다.
    
    @AliasFor("transactionManager")
    String value() default "";

    @AliasFor("value")
    String transactionManager() default "";

    String[] label() default {};

    Propagation propagation() default Propagation.REQUIRED;

    Isolation isolation() default Isolation.DEFAULT;

    int timeout() default -1;

    String timeoutString() default "";

    boolean readOnly() default false;

    Class<? extends Throwable>[] rollbackFor() default {};

    String[] rollbackForClassName() default {};

    Class<? extends Throwable>[] noRollbackFor() default {};

    String[] noRollbackForClassName() default {};
}
```

`@Transactional` 어노테이션의 타깃은 메서드와 타입이다. 따라서 메서드, 클래스, 인터페이스에 사용할 수 있다. `@Transactional` 어노테이션을 트랜잭션 속성정보로 사용하도록 지정하면 스프링은 `@Transactional` 이 부여된 모든 오브젝트를 자동으로 타기 오브젝트로 인식한다. 이때 사용되는 포인트컷은 `TransactionAttributeSourcePointcut` 이다. `TransactionAttributeSourcePointcut`은 스스로 표현식과 같은 선정기준을 갖고 있진 않다. 대신 `@Transactional` 이 타입 레벨이든 메서드 레벨이든 상관없이 부여된 빈 오브젝트를 모두 찾아 포인트컷의 선정 결과로 돌려준다. `@Transactional` 은 기본적으로 트랜잭션 속성을 정의하는 것이지만, 동시에 포인트컷의 자동등록에도 사용된다.