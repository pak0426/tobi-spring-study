## 6.8 트랜잭션 지원 테스트

### 6.8.1 선언적 트랜잭션과 트랜잭션 전파 속성

트랜잭션을 정의할 때 지정할 수 있는 트랜잭션 전파 속성은 매우 유용한 개념이다. 예를 들어 `REQUIRED`로 전파 속성을 지정해줄 경우, 앞에서 진행 중인 트랜잭션이 있으면 참여하고 없으면 자동으로 새로운
트랜잭션을 시작해준다. `REQUIRED` 전파 속성을 가진 메서드를 결합해서 다양한 크기의 트랜잭션 작업을 만들 수 있다. 트랜잭션 적용 때문에 불필요하게 코드를 중복하는 것도 피할 수 있으며, 애플리케이션을 작은
기능 단위로 쪼개서 개발할 수 있다.

예를 들어 사용자 등록 로직을 담당하는 `UserService`의 add() 메서드를 생각해보자. add() 메서드는 트랜잭션 속성이 디폴트로 지정되어 있으므로 트랜잭션 전파 방식은 `REQUIRED`다. 만약
add() 메서드가 처음 호출되는 서비스 계층의 메서드라면 한 명의 사용자를 등록하는 것이 하나의 비즈니스 작업 단위가 된다. 이때는 add() 메서드가 실행되기 전에 트랜잭션이 시작되고 add() 메서드를
빠져나오면 트랜잭션이 종료되는 것이 맞다. DB 트랜잭션은 단위 업무와 일치해야 하기 때문이다.

그런데 작업 단위가 다른 비즈니스 로직이 있을 수 있다. 예를 들어 그날의 이벤트의 신청 내역을 모아서 한 번에 처리하는 기능이 있다고 해보자. 처리되지 않은 이벤트 신청정보를 모두 가져와 DB에 등록하고 그에 따른
정보를 조작해주는 기능이다. 그런데 신청정보의 회원가입 항목이 체크되어 있는 경우에는 이벤트 참가자를 자동으로 사용자로 등록해줘야 한다. 하루치 이벤트 신청 내역을 처리하는 기능은 반드시 하나의 작업 단위로 처리돼야
한다. 이 기능을 `EventService` 클래스의 processDailyEventRegistration() 메서드로 구현했다고 한다면, 이 메서드가 비즈니스 트랜잭션의 경계가 된다. 그런데
processDailyEventRegistration() 메서드는 작업 중간에 사용자 등록을 할 필요가 있다. 직접 UserDao의 add() 메서드를 사용할 수도 있지만, 그보다는 `UserService`의
add() 메서드를 이용해서 사용자 등록 중 처리해야 할, 디폴트 레벨 설정과 같은 로직을 적용하는 것이 바람직하다.

이때 `UserService`의 add() 메서드는 독자적인 트랜잭션을 시작하는 대신 processDailyEventRegistration() 메서드에서 시작된 트랜잭션의 일부로 참여하게 된다. 만약 add()
메서드 호출 뒤에 processDailyEventRegistration() 메서드를 종료하지 못하고 예외가 발생한 경우에는 트랜잭션이 롤백되면서 `UserService`의 add() 메서드에서 등록한 사용자 정보도
취소된다.

트랜잭션 전파라는 기법을 사용했기 때문에 `UserService`의 add()는 독자적인 트랜잭션 단위가 될 수도 있고, 다른 트랜잭션의 일부로 참여할 수도 있다. 트랜잭션의 전파 방식을 이용할 수 없었다면 어떻게
될까? 그렇다면 `UserService`의 add() 메서드는 매번 트랜잭션을 시작하도록 만들어졌을 것이고, 이 때문에 processDailyEventRegistration() 등의 메서드에서 호출해서 사용할 수
없었을 것이다. processDailyEventRegistration()에서 사용자 등록 기능이 필요하긴 하지만 사용자 등록 작업도 같은 트랜잭션에 넣어야 하는데 add() 메서드는 독자적인 트랜잭션을 만들기
때문이다. 그래서 어쩔 수 없이 processDailyEventRegistration() 안에 add() 메서드의 코드를 그대로 복사해서 사용하게 될 것이다.

다행히 스프링은 트랜잭션 전파 속성을 선언적으로 적용할 수 있는 기능을 제공하기 때문에 이런 고민을 할 필요는 없다. 그렇다고 트랜잭션 전파 속성이 개발자의 부주의나 게으름으로 인해 발생하는 불필요한 코드 중복을
막아주지는 못한다.

아래 그림은 add() 메서드에 `REQUIRED` 방식의 트랜잭션 전파 속성을 지정했을 때 트랜잭션이 시작되고 종료되는 경계를 보여준다.

<img width="579" alt="image" src="https://github.com/user-attachments/assets/a62d46ba-75f9-4345-b335-fdfb658baa9a">

AOP를 이용해 코드 외부에서 트랜잭션 기능을 부여해주고 속성을 지정할 수 있게 하는 방법을 **선언적 트랜잭션**이라고 한다. 반대로 `TransactionTemplate` 이나 개별 데이터 기술의 트랜잭션
API를 사용해 직접 코드 안에서 사용하는 방법은 **프로그램에 의한 트랜잭션**이라고 한다. 스프링은 이 두 가지 방법을 모두 지원하고 있다. 물론 특별한 경우가 아니라면 선언적 방식의 트랜잭션을 사용하는 것이
바람직하다.

### 6.8.2 트랜잭션 동기화와 테스트

이렇게 트랜잭션의 자유로운 전파와 그로 인한 유연한 개발이 가능할 수 있었던 기술적인 배경에는 AOP가 있다. AOP 덕분에 프록시를 이용한 트랜잭션 부가기능을 간단하게 애플리케이션 전반에 적용할 수 있었다. 또 한
가지 중요한 기술적인 기반은 바로 스프링의 트랜잭션 추상화다. 데이터 액세스 기술에 상관없이, 또 트랜잭션 기술에 상관없이 DAO에서 일어나는 작업들을 하나의 트랜잭션으로 묶어서 추상 레벨에서 관리하게 해주는
트랜잭션 추상화가 없었다면 AOP를 통한 선언적 트랜잭션이나 트랜잭션 전파 등은 불가능했을 것이다.

#### 트랜잭션 매니저와 트랜잭션 동기화

트랜잭션 추상화 기술의 핵심은 트랜잭션 매니저와 트랜잭션 동기화다. `PlatformTransactionManager` 인터페이스를 구현한 트랜잭션 매니저를 통해 구체적인 트랜잭션 기술의 종류에 상관없이 일관된
트랜잭션 제어가 가능했다. 또한 트랜잭션 동기화 기술이 있었기에 시작된 트랜잭션 정보를 저장소에 보관해뒀다가 DAO에서 공유할 수 있었다.

트랜잭션 동기화 기술은 트랜잭션 전파를 위해서도 중요한 역할을 한다. 진행 중인 트랜잭션이 있는지 확인하고, 트랜잭션 전파 속성에 따라서 이에 참여할 수 있도록 만들어 주는 것도 트랜잭션 동기화 기술 덕분이다.

그렇다면 트랜잭션 전파 속성 중 `REQUIRED`는 이미 시작된 트랜잭션이 있으면 그 트랜잭션에 참여하게 해준다. 진행 중인 트랜잭션에 동기화되는 것이다. 지금은 모든 트랜잭션을 선언적으로 AOP로 적용하고
있지만, 필요하다면 프로그램에 의한 트랜잭션 방식을 함께 사용할 수도 있다. 어차피 트랜잭션 어드바이스에서도 트랜잭션 매니저를 통해 트랜잭션을 제어하는 것이니 코드에서 직접 트랜잭션 매니저를 이용해 트랜잭션에
참여하는 것이 안 될 이유는 없다. 물론 특별한 이유가 없다면 트랜잭션 매니저를 직접 이용하는 코드를 작성할 필요는 없다. 선언적 트랜잭션이 훨씬 편리하다.

그런데 특별한 이유가 있다면 트랜잭션 매니저를 이용해 트랜잭션에 참여하거나 트랜잭션을 제어하는 방법을 사용할 수도 있다. 지금까지 진행했던 특별하고 독특한 작업은 모두 한 군데서 일어났다. 바로 테스트다.

스프링 테스트 컨텍스트를 이용해 테스트에서는 `@Autowired`를 이용해 애플리케이션 컨텍스트에 등록된 빈을 가져와 테스트 목적으로 활용할 수 있었다. 그렇다면 당연히 트랜잭션 매니저 빈도 가져올 수 있다.
트랜잭션 매니저는 아래와 같이 빈으로 선언되어 있다.

```java

@Bean
public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
}
```

따라서 아래처럼 `@Autowired`를 사용해서 테스트에서 사용할 수 있다.

```java

@SpringBootTest
class UserServiceTest {
    @Autowired
    private PlatformTransactionManager transactionManager;
}
```

이제 간단한 테스트 메서드를 추가하자.

```java

@Test
public void transactionSync() {
    userService.deleteAll();

    userService.add(users.get(0));
    userService.add(users.get(1));
}
```

위 테스트를 실행되는 동안 3개의 트랜잭션이 만들어진다. 각 메서드가 모두 독립적인 트랜잭션 안에서 실행된다. 테스트에서 각 메서드를 실행시킬 때는 기존에 진행 중인 트랜잭션이 없고 트랜잭션 전파
속성은 `REQUIRED`이니 새로운 트랜잭션이 시작된다. 그리고 그 메서드를 정상적으로 종료하는 순간 트랜잭션은 커밋되면서 종료될 것이다. deleteAll() 메서드가 실행되는 시점에서 트랜잭션이 시작됐으니
당연히 그 메서드가 끝나면 트랜잭션도 같이 종료된다. 그 후에 add() 메서드가 호출되면 현재 진행 중인 트랜잭션은 없으니 마찬가지로 새로운 트랜잭션이 만들어질 것이다. 마지막 add() 메서드도 마찬가지다.

#### 트랜잭션 매니저를 이용한 테스트 트랜잭션 제어

그렇다면 이 테스트 메서드에서 만들어지는 3개의 트랜잭션을 통합할 수는 없을까? 즉 하나의 트랜잭션 안에서 deleteAll()과 두 개의 add() 메서드가 동작하게 할 방법은 없을까?

세 개의 메서드 모두 트랜잭션 전파 속성이 `REQUIRED`이니 이 메서드들이 호출되기 전에 트랜잭션이 시작되게만 한다면 가능하다. `UserService`에 새로운 메서드를 만들고 그
안에서 `deleteAll()`과 `add()`를 호출한다면 물론 가능하다. `UserService`의 모든 메서드는 트랜잭션 경계가 되니 새로 만든 메서드에서 시작한 트랜잭션이 `deleteAll()`
과 `add()` 메서드를 묶어 하나의 트랜잭션 안에서 동작할 것이다.

그런데 메서드를 추가하지 않고 테스트 코드만으로 세 메서드의 트랜잭션을 통합하는 방법이 있다. 테스트 메서드에서 `UserService`의 메서드를 호출하기 전에 트랜잭션을 미리 시작해주면 된다. 트랜잭션 전파는
트랜잭션 매니저를 통해 트랜잭션 동기화 방식으로 적용되기 때문에 가능하다고 했다. 그렇다면 테스트에서 트랜잭션 매니저를 이용해 트랜잭션을 시작시키고 이를 동기화해주면 된다.

트랜잭션을 시작하기 위해 먼저 트랜잭션 정의를 담은 오브젝트를 만들고 이를 트랜잭션 매니저에 제공하면서 새로운 트랜잭션을 요청하면 된다. 트랜잭션 매니저는 이미 `@Autowired`를 테스트 코드로 주입하게
해놨다.

```java

@Test
public void transactionSync() {
    // 트랜잭션 정의는 기본 값을 사용한다.
    DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

    // 트랜잭션 매니저에게 트랜잭션을 요청한다. 기존에 시작된 트랜잭션이 없으니 새로운 트랜잭션을 시작시키고 트랜잭션 정보를 돌려준다.
    // 동시에 만들어진 트랜잭션을 다른 곳에서도 사용할 수 있도록 동기화한다.
    TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);

    // 앞에서 만들어진 트랜잭션에 모두 참여한다.
    userService.deleteAll();

    userService.add(users.get(0));
    userService.add(users.get(1));
    //

    transactionManager.commit(txStatus); // 앞에서 시작한 트랜잭션을 커밋한다.
}
```

무책임한 코드긴 하지만 어쨋든, 이렇게 테스트 코드에서 트랜잭션 매니저를 이용해 트랜잭션을 만들고 그 후에 실행되는 `UserService`의 메서드들이 같은 트랜잭션에 참여할 수 있게 만들 수 있다. 3개의 메서드
속성이 `REQUIRED`이므로 이미 시작된 트랜잭션이 있으면 참여하고 새로운 트랜잭션을 만들지 않는다.

#### 트랜잭션 동기화 검증

테스트를 돌려보면 별문제 없이 작업을 마칠 테니 성공이라고 나올 것이다. 하지만 정말 이 세 개의 메서드가 테스트 코드 내에서 시작된 트랜잭션에 참여하고 있는지 알 수 없다. 그래서 트랜잭션 속성을 변경해 이를
증명해보자.

트랜잭션 속성 중에서 읽기전용과 제한시간 등은 처음 트랜잭션이 시작할 때만 적용되고 그 이후에 참여하는 메서드의 속성은 무시된다. 즉 `deleteAll()`의 트랜잭션 속성은 쓰기 가능으로 되어 있지만 앞에서
시작된 트랜잭션이 읽기전용이라고 하면 `deleteAll()`의 모든 작업도 읽기전용 트랜잭션이 적용된 상태에서 진행된다는 말이다.

이번엔 트랜잭션 속성을 아래와 같이 읽기 전용으로 만들고 다시 테스트를 돌려보자.

```java

@Test
public void transactionSync() {
    DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
    TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);

    // 트랜잭션이 활성화되었는지
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());
    // 격리 수준
    System.out.println(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

    userService.deleteAll();
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

    userService.add(users.get(0));
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

    userService.add(users.get(1));
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

    transactionManager.commit(txStatus);
}
```

이 코드를 살펴보면 트랜잭션 매니저를 통해 트랜잭션을 열고 `UserService`의 메서드들이 사용되기 전에 트랜잭션이 활성화되고 있는지 확인할 수 있다.
출력문을 확인하면 다음과 같다.

```
true
null
true
true
true
```

```java

@Test
public void transactionSync() {
//        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
//        TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);

    // 트랜잭션이 활성화되었는지
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());
    // 격리 수준
    System.out.println(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

    userService.deleteAll();
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

    userService.add(users.get(0));
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

    userService.add(users.get(1));
    System.out.println(TransactionSynchronizationManager.isActualTransactionActive());

//        transactionManager.commit(txStatus);
}
```

만약 트랜잭션 매니저 부분을 지우고 테스트를 하게 되면 결과는 다음과 같다.

```
false
null
false
false
false
```

우선 트랜잭션 매니저를 통한 트랜잭션도 실행되지 않았고 각 메서드 사이마다 트랜잭션이 유지되지 않는다는 결과를 확인할 수 있다.

#### 롤백 테스트

테스트 코드로 트랜잭션을 제어해서 적용할 수 있는 테스트 기법이 있다. 바로 롤백 테스트다. 롤백 테스트는 테스트 내의 모든 DB 작업을 하나의 트랜잭션 안에서 동작하게 하고 테스트가 끝나면 무조건 롤백해버리는 테스트를 말한다. 예를 들어 아래 코드는 전형적인 롤백 테스트다.

```java
    @Test
    public void transactionSync() {
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);

        try {
            // 테스트 안의 모든 작업을 하나의 트랜잭션으로 통합한다.
            userService.deleteAll();
            userService.add(users.get(0));
            userService.add(users.get(1));
        }
        finally {
            // 테스트 결과가 어떻든 테스트가 끝나면 무조건 롤백. 테스트 중에 발생했던 DB 변경사항은 모두 이전 상태로 복구된다.
            transactionManager.rollback(txStatus);
        }
    }
```

롤백 테스트는 DB 작업이 포함된 테스트가 수행돼도 DB에 영향을 주지 않기 때문에 장점이 많다. DB를 사용하는 코드를 테스트하는 건 여러 가지 이유로 작성하기 힘들다. 간단한 CRUD 테스트야 별문제가 안 되겠지만 복잡한 데이터를 바탕으로 동작하는 기능을 테스트하려면 테스트가 실행될 때의 DB 데이터와 상태가 매우 중요하다. 문제는 테스트에서 DB에 쓰기 작업을 기능을 실행하면서 테스트를 수행하고 나면 DB의 데이터가 바뀐다는 점이다. 따라서 테스트를 실행하기 전에 적절한 DB 상태를 만들어놓더라도 테스트가 끝나고 나면 데이터와 상태가 바뀐다. 테스트는 어떤 순서로 어떻게 진행될지 보장할 수 없고, 성공했을 때와 실패했을 때 DB에 다른 방식으로 영향을 줄 수 있다. 그래서 테스트용 데이터를 DB에 잘 준비해놓더라도 앞에서 실행된 테스트에서 DB의 데이터를 바꾸면 실행되는 테스트에 영향을 미칠 수 있다.

결국 DB를 액세스하는 테스트를 위해서는 테스트를 할 때마다 테스트 데이터를 초기화하는 번거로운 작업이 필요해진다. 테스트의 목적에 따라서 테스트용 DB는 제각각이겠지만, 그 중에서 레퍼런스 데이터처럼 읽기전용인 것도 있고, 다른 테스트에 의해 데이터가 변경되지만 않는다면 많은 테스트에서 공유 가능한 마스터 데이터도 있다. 이렇게 테스트를 위해 어느 정도 미리 준비해둘 수 있는 공통 테스트 데이터가 있는데, DB 데이터를 수정하고 삭제하는 등의 작업을 진행하는 테스트 때문에 준비된 데이터가 바뀌면 기껏 준비한 테스트 데이터는 무용지물이 된다.

예를 들면 add() 테스트는 새로운 사용자 정보가 등록되는지를 테스트하기 위해 일단 User 테이블의 데이터를 모두 삭제해버린다. 그리고 새로 등록한 데이터만으로 테스트를 수행한다. 다른 테스트에서 활용하려고 기껏 준비한 User 테이블의 테스트용 샘플 데이터가 있었더라도 add() 테스트를 거치고 나면 엉망이 되고 만다.

바로 이런 이유 때문에 롤백 테스트는 매우 유용하다. 롤백 테스트는 테스트를 진행하는 동안에 조작한 데이터를 모두 롤백하고 테스트 전 상태로 만들어주기 때문이다. 이처럼 테스트에서 트랜잭션을 제어할 수 있기 때문에 얻을 수 있는 가장 유익이 있다면 바로 이 롤백 테스트다. DB에 따라서 성공적인 작업이라도 트랜잭션을 롤백하면 커밋할 때보다 성능이 더 향상되기도 한다. 예를 들어 MySQL에서는 동일한 작업을 수행한 뒤에 롤백하는 게 커밋하는 것보다 더 빠르다. 하지만 DB의 트랜잭션 처리 방법에 따라 롤백이 커밋보다 더 많은 부하를 주는 경우도 있으니 단지 성능 때문에 롤백 테스트가 낫다고는 볼 수 없다.

### 6.8.3 테스트를 위한 트랜잭션 어노테이션

`@Transactional` 어노테이션을 타깃 클래스 또는 인터페이스에 부여하는 것만으로 트랜잭션을 적용해주는 건 매우 편리한 기술이다. 그런데 이 편리한 방법을 테스트 클래스와 메서드에도 적용할 수 있다.

스프링의 컨텍스트 테스트 프레임워크는 어노테이션을 이용해 테스트를 편리하게 만들 수 있는 여러 가지 기능을 추가하게 해준다. `@ContextConfiguration` 을 클래스에 부여하면 테스트를 실행하기 전에 스프링 컨테이너를 초기화하고, `@Autowired` 어노테이션이 붙은 필드를 통해 테스트에 필요한 빈에 자유롭게 접근할 수 있다. 그 외에도 스프링 컨텍스트 테스트에서 쓸 수 있는 유용한 어노테이션이 여러 개 있다.

#### @Transactional

테스트에도 `@Transactional`을 적용할 수 있다. 테스트 클래스 또는 메서드에 `@Transactional` 어노테이션을 부여해주면 마치 타깃 클래스나 인터페이스에 적용된 것처럼 테스트 메서드에 트랜잭션 경계가 자동으로 설정된다. 이를 이용하면 테스트 내에서 진행하는 모든 트랜잭션 관련 작업을 하나로 묶어줄 수 있다. `@Transactional`에는 모든 종류의 트랜잭션 속성을 지정할 수 있기도 하다.

테스트의 `@Transactional`은 앞에서 테스트 메서드의 코드를 이용해 트랜잭션을 마들어 적용했던 것과 동일한 결과를 가져온다. 트랜잭션 매니저와 번거로운 코드를 사용하는 대신 간단한 어노테이션만으로 트랜잭션이 적용된 테스트를 손쉽게 만들 수 있는 것이다.

물론 테스트에서 사용하는 `@Transactional`은 AOP를 위한 것은 아니다. 단지 컨텍스트 테스트 프레임워크에 의해 트랜잭션을 부여해주는 용도로 쓰일 뿐이다. 하지만 기본적인 동작방식과 속성은 `UserService` 등에 적용한 `@Transactional`과 동일하므로 이해하기 쉽고 사용하기 편리하다.

테스트의 트랜잭션 속성을 메서드 단위에 부여한다면 아래와 같이 만들 수 있다. `UserService`의 메서드에 적용했을 때와 마찬가지로 테스트 메서드 실행 전에 새로운 트랜잭션을 만들어주고 메서드가 종료되면 트랜잭션을 종료해준다.

```java
    @Test
    @Transactional
    public void transactionSync() {
            userService.deleteAll();
            userService.add(users.get(0));
            userService.add(users.get(1));
    }
```

트랜잭션 적용 여부를 확인해보고 싶다면 트랜잭션을 아래와 같이 읽기 전용으로 바꾸고 테스트를 실행해 예외가 발생하는지 확인해보면 된다.

```java
    @Test
    @Transactional(readOnly = true)
    public void transactionSync() {
            userService.deleteAll();
            userService.add(users.get(0));
            userService.add(users.get(1));
    }
```

`@Transactional`은 테스트 클래스 레벨에 부여할 수도 있다. 그러면 테스트 클래스 내의 모든 메서드에 트랜잭션이 적용된다. 각 메서드에 `@Transactional`을 지정해서 클래스의 공통 트랜잭션과는 다른 속성을 지정할 수도 있다. 메서드의 트랜잭션 속성이 클래스의 속성보다 우선시 된다.

#### @Rollback

테스트 메서드나 클래스에 사용하는 `@Transactional`은 애플리케이션의 클래스에 적용할 때와 디폴트 속성은 동일하다. 하지만 중요한 차이점이 있는데, 테스트용 트랜잭션은 테스트가 끝나면 자동으로 롤백된다는 것이다. 테스트에 적용된 `@Transactional`은 기본적으로 트랜잭션을 강제 롤백시키도록 설정되어 있다. `@Transactional`을 지정해주면 롤백 테스트가 되는 것이다.

정말 그런지 확인을 해보자. `transactionSync()` 메서드는 테스트가 정상적으로 끝나면 테스트용으로 넣은 두 개의 사용자 정보가 DB에 남아 있어야 한다. 테스트 메서드의 `@Transactional`을 제거하고, DB Users 테이블 정보를 모두 삭제한 후에 `transactionSync()` 테스트 한 개만 실행해보자. 테스트가 끝나고 DB를 확인해보면 두 개의 사용자가 등록되어 있을 것이다.

다시 테이블 내용을 모두 삭제하고, `@Transactional`을 다시 부여 후 테스트해보자. 그리고 DB의 Users 테이블을 살펴보면 이번에는 아무런 데이터가 남아있지 않은걸 확인할 수 있다.

이렇게 스프링의 컨텍스트 테스트는 `@Transactional`을 테스트에서도 편리하게 쓸 수 있도록 해준다. 그런데 테스트 메서드 안에서 진행되는 작업을 하나의 트랜잭션으로 묶고 싶기는 하지만 강제 롤백을 원하지 않을 수도 있다. 트랜잭션을 커밋시켜서 테스트에서 진행한 작업을 그대로 DB에 반영하고 싶다면 어떻게 해야 할까?

이때는 `@Rollback`이라는 어노테이션을 이용하면 된다. `@Transactional`은 기본적으로 테스트에서 사용할 용도로 만든 게 아니리 때문에 롤백 테스트에 관한 설정을 담을 수 없다. 따라서 롤백 기능을 제어하려면 별도의 어노테이션을 사용해야 한다. `@Rollback`은 롤백 여부를 지정하는 값을 갖고 있다. `@Rollback`의 기본 값은 true다. 따라서 트랜잭션은 적용되지만 롤백을 원치 않는다면 `@Rollback(false)`라고 해줘야 한다.

아래와 같이 테스트 메서드를 설정해주면 테스트 전체에 걸쳐 하나의 트랜잭션이 만들어지고 예외가 발생하지 않는 한 트랜잭션은 커밋된다.

```java
    @Test
    @Transactional
    @Rollback(false)
    public void transactionSync() {
            userService.deleteAll();
            userService.add(users.get(0));
            userService.add(users.get(1));
    }
```

#### @TransactionConfiguration

`@Transactional`은 테스트 클래스에 넣어서 모든 테스트 메서드에 일괄 적용할 수 있지만 `@Rollback`은 메서드 레벨에만 적용할 수 있다.

테스트 클래스의 모든 메서드에 트랜잭션을 적용하면서 모든 트랜잭션이 롤백되지 않고 커밋되게 하려면 어떻게 해야할까? `@TransactionalConfiguration` 어노테이션을 이용하면 편리하다.

`@TransactionalConfiguration`을 사용하면 롤백에 대한 공통 속성을 지정할 수 있다. 디폴트 롤백 속생은 false로 해두고, 테스트 메서드 중에서 일부만 롤백을 적용하고 싶으면 메서드에 `@Rollback`을 부여해주면 된다. 기본 값이 true이므로 이때는 트랜잭션을 롤백해준다.

```java
@SpringBootTest
@TransactionConfiguration
class UserServiceTest {
    // ...

    @Test
    @Transactional
    @Rollback
    public void transactionSync() {
        userService.deleteAll();
        userService.add(users.get(0));
        userService.add(users.get(1));
    }
}
```

> Spring 4.2 이후부터 Deprecated 되었다. @Rollback, @Commit을 사용하면 된다. 