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

#### 트랜잭션 속성을 이용하는 포인트컷

아래 그림은 `@Transactional` 어노테이션을 사용했을 때 어드바이스 동작방식을 보여준다. `TransactionInterceptor`는 메서드 이름 패턴을 통해 부여되는 트랜잭션 속성정보 대신 `@Transactional` 의 앨리먼트에서 트랜잭션 속성을 가져오는 `AnnotationTransactionAttributeSource` 를 사용한다. `@Transactional`은 메서드마다 다르게 설정할 수 있어 매우 유연하다.

동시에 포인트컷도 `@Transactional`을 통한 트랜잭션 속성정보를 참조해 만든다. `@Transactional`로 트랜잭션 속성이 부여된 오브젝트라면 포인트컷 선정 대상이기도 하기 때문이다.

<img width="554" alt="image" src="https://github.com/user-attachments/assets/facb9323-f0bb-4c7f-a415-ce2d009ef317">

이 방식을 사용하면 포인트컷과 트랜잭션 속성을 어노테이션 하나로 지정할 수 있다. 트랜잭션 속성은 타입 레벨에 일괄적으로 부여할 수도 있지만, 메서드 단위로 세분화해서 트랜잭션 속성을 다르게 지정할 수도 있기 때문에 매우 세밀한 트랜잭션 속성 제어가 가능해진다.

트랜잭션 부가기능 적용 단위는 메서드다. 따라서 메서드마다 `@Transactional`을 부여하고 속성을 지정할 수 있다. 이렇게 하면 속성 제어는 가능하겠지만 코드는 지저분해지고, 동일한 속성 정보를 가진 어노테이션을 반복적으로 메서드마다 부여해주는 바람직하지 못한 결과를 가져올 수 있다.

#### 대체 정책

그래서 스프링은 `@Transactional`을 적용할 때 4단계의 대체 정책을 이용하게 한다. 메서드 속성을 확인할 때 타깃 메서드, 타깃 클래스, 선언 메서드, 선언 타입(클래스, 인터페이스)의 순서에 따라 `@Transactional`이 적용됐는지 차례대로 확인하고, 가장 먼저 발견되는 속성 정보를 사용하게 하는 방법이다. 가장 먼저 타깃의 메서드에 `@Transactional`이 있는지 확인한다. `@Transactional`이 부여되어 있다면 이를 속성으로 사용한다. 만약 없으면 다음 대체 후보인 타깃 클래스에 부여된 `@Transactional` 어노테이션을 찾는다. 타깃 클래스 메서드 레벨에는 없었지만 클레스 레벨에 `@Transactional`이 존재한다면 이를 메서드 트랜잭션 속성으로 사용한다. 이런식으로 메서드가 선언된 타입까지 단계적으로 확인해서 `@Transactional`이 발견되면 적용하고, 끝까지 발견되지 않으면 해당 메서드는 트랜잭션 적용 대상이 아니라고 판단한다.

아래와 같이 정의된 인터페이스와 구현 클래스가 있다고 하자. 위와 같은 코드라면 `@Transactional` 이 적용될 수 있는 위치는 6개다.

<img width="297" alt="image" src="https://github.com/user-attachments/assets/7516f916-7c67-4c51-beed-5e4a7a4a3cf1">

스프링은 트랜잭션 기능이 부여될 위치인 타깃 오브젝트의 메서드부터 시작해서 `@Transactional` 이 존재하는지 확인한다. 따라서 [5], [6] 이 `@Transactional`이 위치할 수 있는 첫 번째 후보다. 여기서 어노테이션이 발견되면 바로 속성을 가져다 해당 메서드의 트랜잭션 속성으로 사용한다.

메서드에서 `@Transactional`을 발견하지 못하면, 다음은 타깃 클래스인 [4]에서 `@Transactional`이 존재하는지 확인한다. 이를 통해 `@Transactional`이 타입 레벨, 즉 클래스에 부여되면 해당 클래스의 모든 메서드의 공통적으로 적용되는 속성이 될 수 있다. 메서드 레벨에 `@Transactional`이 없다면 모두 클래스 레벨의 속성을 사용할 것이기 때문이다. 메서드가 여러 개라면 클래스 레벨에 `@Transactional`을 부여하는 것이 편리하다. 특정 메서드만 공통 속성을 따르지 않는다면 해당 메서드에만 추가로 `@Transactional`을 부여해주면 된다. 대체 정책에서 지정한 순서에 따라서 항상 메서드에 부여된 `@Transactional`이 가장 우선이기 때문에 `@Transactional`이 붙은 메서드는 클래스 레벨의 속성을 무시하고 메서드 레벨의 속성을 사용할 것이다. 반면에 `@Transactional`이 붙이지 않은 여타 메서드는 클래스 레벨에 부여된 공통 `@Transactional`을 따르게 된다.

타깃 클래스에도 `@Transactional`을 발견하지 못하면, 스프링은 메서드가 선언된 인터페이스로 넘어간다. 인터페이스도 메서드 먼저 확인한다. 따라서 [2], [3]에 `@Transactional`이 부여됐는지 확인하고 있다면 이 속성을 적용한다. 인터페이스 메서드에도 없다면 마지막 단게인 인터페이스 타입 [1]에 위치에 어노테이션이 있는지 확인한다.

`@Transactional`을 사용하면 대체 정책을 잘 활용해서 어노테이션 자체는 최소한으로 사용하면서 세밀한 제어가 가능하다. `@Transactional`은 먼저 타입 레벨에 정의되고 공통 속성을 따르지 않는 메서드에 대해서만 메서드 레벨에 다시 `@Transactional`을 부여해주는 식으로 사용해야 한다. 기본적으로 `@Transactional` 적용 대상은 클라이언트가 사용하는 인터페이스가 정의한 메서드이므로 `@Transactional`도 타깃 클래스보단 인터페이스에 두는 게 바람직하다. 하지만 인터페이스를 사용하는 프록시 방식의 AOP가 아닌 방식으로 트랜잭션을 적용하면 인터페이스에 정의한 `@Transactional`은 무시된기 때문에 안전하게 타깃 클래스에 `@Transactional`을 두는 방법을 권장한다.

프록시 방식 AOP의 동작원리를 잘 이해하고 있고 `@Transactional`의 적용 대상을 적잘하게 변경해줄 확신이 있거나, 반드시 인터페이스를 사용하는 타깃에만 트랜잭션을 적용하겠다는 확인이 있다면 인터페이스에 `@Transactional`을 적용하고 아니라면 타깃 클래스, 메서드에 적용하는 편이 낫다.

인터페이스에 `@Transactional`을 두면 구현 클래스가 바뀌더라도 트랜잭션 속성을 유지할 수 있다는 장점이 있다.