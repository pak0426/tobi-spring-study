## 5.2 트랜잭션 서비스 추상화

이제 사용자 레벨 관리 기능에 대한 구현과 테스트를 통해 검증도 끝났다. 다음 기능으로 넘어가기 전에 

> 정기 사용자 레벨 관리 작업을 수행하는 도중에 네트워크가 끊기거나 서버에 장애가 생겨서 작업을 완료할 수 없다면, 변경된 사용자 레벨은 어떻게 해야하나요?

이런 질문이 있다면 어떻게 해야할까?

### 5.2.1 모 아니면 도

그렇다면 지금까지 만든 사용자 레벨 업그레이드 코드는 어떻게 작동할까? 테스트를 만들어서 확인해보자.

테스트를 수행하다가 중간에 예외가 발생되게 만든다. 시스템 예외 상황을 만들면 좋지만 그런 실제 상황을 연출하는건 불가능하다. 예외를 던져 상황을 의도적으로 만들어 보자.

#### 테스트용 UserService 대역

어떻게 작업 중간에 예외를 발생 시킬까? 가장 쉬운 방법은 애플리케이션 코드를 **예외가 발생하도록 수정하는 것이다.** 하지만 테스트를 위해 코드를 함부로 건드리는 것은 좋지 않다. 따라서 UserService 대역을 만들어 테스트의 목적에 맞게 동작하는 클래스를 만드는 것이다.

테스트용 UserService 확장 클래스는 어떻게 만들 수 있을까? 기존 코드를 복사해 새로운 클래스를 만들고 일부를 수정하면 되겠지만, **코드 중복도 발생하고 사용하기도 번거롭다.** 따라서 간단히 UserService를 상속해서 테스트에 필요한 기능을 추가하도록 일부 메서드를 오버라이딩하는 방법이 나을 것 같다.

```java
    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }
```

UserServiceTest 내부에 static 클래스로 UserService를 상속한 테스트 서비스를 만들어 준다.

```java
    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }
```

마찬가지로 테스트 서비스에서 사용되는 커스텀 예외도 내부 클래스로 만들어서 선언해준다.

```java
    static class TestUserServiceException extends RuntimeException {
    }
```

#### 강제 예외 발생을 통한 테스트

이제 테스트를 만들어보자. 테스트의 목적은 **사용자 레벨 업그레이드를 시도하다가 중간에 예외가 발생했을 경우, 그 전에 업그레이드했던 사용자도 원래 상태로 돌아가는지 확인하는 것**이다.

```java
    @Test
    public void upgradeAllOrNoting() {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(userDao); // userDao를 수동 DI한다.

        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevelUpgraded(users.get(1), false);
    }
```

눈 여겨서 보면 좋을 코드는 아래와 같다. 
- 테스트용으로 만들어둔 TestUserService의 오브젝트를 생성, 파라미터로 예외를 발생시킬 사용자의 id를 넣어준다.
- 스프링 컨텍스트로부터 가져온 userDao를 테스트용 TestUserService에 수동으로 DI 해준다.

TestUserService는 테스트 용으로 사용되는 것이므로 번거롭게 스프링 빈으로 등록할 필요는 없다. 컨테이너에 종속적이지 않은 평범한 자바 코드로 만ㄷ르어지는 스프링 DI 스타일의 장점이 바로 이런 것이다. UserDao를 DI 해주고 나면 testUserSerivce 오브젝트는 스프링 설정에 의해 정의된 userService 빈과 동일하게 UserDao를 사용해 데이터 액세스 기능을 할 수 있다.

위 테스트 코드를 실행해보면 

<img width="366" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/551d995e-3ce3-434b-8b13-5dea42ce3b72">

실패하는 걸 확인할 수 있다. 즉 2번째 사용자는 upgrade 조건을 충족하여 업그레이드 했지만 4번째 사용자에서 예외가 발생했을때 그대로 유지되고 있는 것이다.

#### 테스트 실패의 원인

DB와 JDBC 프로그래밍에 대한 기본적인 지식이 있다면 왜 이런 결과가 나왔는지 쉽게 알 수 있다. 바로 **트랜잭션 문제다.** 모든 사용자의 레벨을 업그레이드하는 작업인 upgradeLevels() 메서드가 하나의 트랜잭션 안에서 동작하지 않았기 때문이다.

**트랜잭션이란 더 이상 나눌 수 없는 작업 단위**를 말한다. 작업을 쪼개서 작은 단위로 만들 수 없다는 것은 트랜잭션의 핵심 속성인 원자성을 의미한다.

### 5.2.2 트랜잭션 경계설정

DB는 그 자체로 완벽한 트랜잭션을 지원한다. SQL을 이용해 다중 로우의 수정이나 삭제를 위한 요청을 했을 때 일부 로우만 삭제되고 나머지는 안 된다거나, 일부 필드는 수정했는데 나머지 필드는 수정이 안 되고 실패로 끝나는 경우는 없다. 하나의 SQL 명령을 처리하는 경우는 DB가 트랜잭션을 보장해준다고 믿을 수 있다. 하지만 여러 개의 SQL이 사용되는 작업을 하나의 트랜잭션으로 취급해야 하는 경우도 있다.

문제는 첫 번째 SQL을 성공적으로 실행했지만 두 번째 SQL이 성공하기 전에 장애가 생겨서 작업이 중단되는 경우다. 이때 두 가지 작업이 하나의 트랜잭션이 되려면, 두 번째 SQL이 성공적으로 DB에 수행되기 전에 문제가 발생할 경우에는 앞에서 처리한 SQL 작업도 취소시켜야 한다. 이런 취소 작업을 **트랜잭션 롤백** 이라고 한다. 반대로 여러 개의 SQL을 하나의 트랜잭션으로 처리하는 경우에 모든 SQL 수행 작업이 다 성공적으로 마무리됐다고 DB에 알려줘서 작업을 확정시켜야 한다. 이것을 **트랜잭션 커밋**이라고 한다.

#### JDBC 트랜잭션의 트랜잭션 경계 설정

<img width="520" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/864231d4-ff1a-477f-ade1-954bf29e35b9">

JDBC의 트랜잭션은 하나의 Connection을 가져와 사용하다가 닫는 사이에서 일어난다. 트랜잭션의 시작과 종료는 Connection 오브젝트를 통해 이뤄지기 때문이다. JDBC에서 트랜잭션을 시작하려면 자동커밋 옵션을 false로 만들어주면 된다. JDBC의 기본 설정은 DB 작업을 수행한 직후에 자동으로 커밋이 되도록 되어 있다.

이렇게 setAutoCommit(false)로 트랜잭션의 시작을 선언하고 commit() 또는 rollback() 으로 트랜잭션을 종료하는 작업을 **트랜잭션의 경계설정**이라고 한다. **트랜 잭션의 경계는 하나의 Connection이 만들어지고 닫히는 범위 안에 존재한다는 점**도 기억하자. 이렇게 하나의 DB 커넥션 안에서 만들어지는 트랜잭션을 **로컬 트랜잭션이**라고 한다.

#### UserService와 UserDao의 트랜잭션 문제

그렇다면 이제 왜 UserService의 upgradeLevels() 에는 트랜잭션이 적용되지 않았는지 생각해보자. JdbcTemplate은 직접 만들었던 JdbcContext와 작업 흐름이 거의 동일하다. 하나의 템플릿 메서드 안에서 DataSource의 getConnection() 메서드를 호출해서 Connection 오브젝트를 가져오고, 작업을 마치면 Conenction을 확실히 닫아주고 템플릿 메서드를 빠져 나온다.

따라서 템플릿 메서드가 호출될 때마다 트랜잭션이 새로 만들어지고 메서드를 빠져나오기 전에 종료된다. 결국 JdbcTemplate의 메서드를 사용하는 UserDao는 각 메서드마다 하나씩 독립적인 트랜잭션으로 실행될 수밖에 없다.

<img width="510" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/e4feff02-096d-496c-961c-7ab553164a3a">

그렇다면 upgradeLevels()와 같이 여러 번 DB에 업데이트를 해야하는 하나의 트랜잭션으로 만들려면 어떻게 해야 할까?

#### 비즈니스 로직 내의 트랜잭션 경계 설정

이 문제를 해결하기 위해 DAO 메서드 안으로 upgradeLevels() 메서드의 내용을 옮기는 방법을 생각해볼 수 있다. 하지만 이 방식은 비즈니스 로직과 데이터 로직을 한데 묶어버리는 심각한 결과를 초래한다. 결국 그대로 둔 채 트랜잭션을 적용하려면 트랜잭션 경계설정 작업을 UserService 쪽으로 가져와야 한다. 프로그램 흐름을 볼 때 upgradeLevels() 메서드의 시작과 함께 트랜잭션이 시작하고 메서드를 빠져나올 때 트랜잭션이 종료돼야 하기 때문이다.

<img width="361" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/daa7c1a5-47e5-481e-88e2-2837c9203479">

트랜잭션을 사용하는 전형적인 JDBC 코드의 구조다. 그런데 여기서 생성된 Connection 오브젝트를 가지고 데이터 액세스 작업을 진행하는 코드는 UserDao의 update() 메서드 안에 있어야 한다.

UserService에서 만든 Connection 오브젝트를 UserDao에서 사용하려면 DAO 메서드를 호출할 때마다 Connection 오브젝트를 파라미터로 전달해줘야 한다.

```java
public interface UserDao {
    public void add(Connection c, User user);
    public void get(Connection c, User user);
    // ....
    public void update(Connection c, User user);
}
```

트랜잭션을 담고 있는 Connection을 공유하려면 더 해줄 일이 있다. UserService의 upgradeLevels()는 UserDao의 update()를 직접 호출하지 않는다. UserDao를 사용하는 것은 사용자별로 업그레이드 작업을 진행하는 upgradeLevel() 메서드다. 결국 아래와 같이 UserService의 메서드 사이에도 같은 Connection 오브젝트를 사용하도록 파라미터로 전달해줘야만 한다.

<img width="418" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/70d0ba18-8b48-4ffe-a7d7-9a48e9049aca">

이렇게 Conenction 오브젝트를 전달해서 사용하면 UserService의 upgradeLevels() 안에서 시작한 트랜잭션에 UserDao의 메서드들도 참여할 수 있다.

#### UserService 트랜잭션 경계설정의 문제점

UserService와 UserDao를 이런 식으로 수정하면 트랜잭션 문제는 해결할 수 있겟지만, 그 대신 여러 가지 새로운 문제가 발생한다.

1. DB 커넥션을 비롯한 리소스의 깔끔한 처리를 가능하게 했던 JdbcTemplate을 더 이상 활용할 수 없다. 결국 JDBC API를 직접 사용하는 초기 방식으로 돌아가야 한다.
2. DAO의 메서드와 비즈니스 로직을 담고 있는 UserService의 메서드에 Connection 파라미터가 추가돼야 한다는 점이다. upgradeLevels() 에서 사용하는 메서드의 어딘가에서 DAO를 필요로 한다면, 그 사이의 모든 메서드에 걸쳐서 Connection 오브젝트가 계속 전달돼야 한다. UserService는 스프링 빈으로 선언해서 싱글톤으로 되어 있으니 UserService의 인스턴스 변수에 이 Connection을 저장해뒀다가 다른 메서드에서 사용하게 할 수도 없다.
3. Connection 파라미터가 UserDao 인터페이스 메서드에 추가되면 UserDao는 더 이상 데이터 액세스 기술에 독립적일 수가 없다는 점이다. JPA나 하이버네티으로 UserDao의 구현 방식을 변경하려고 하면 Connection 대신 EntityManager나 Session 오브젝트를 UserDao 메서드가 전달받도록 해야 한다. 결국 UserDao 인터페이스는 바뀔 것이고, 그에 따라 UserService 코드도 함께 수정돼야 한다.
4. DAO 메서드에 Connection 파라미터를 받게 하면 테스트 코드에도 영향을 미친다. 지금까지 DB 커넥션을 신경쓰지 않고 테스트에서 UserDao를 사용할 수 있었는데, 이제는 테스트 코드에서 직접 Connection 오브젝트를 일일이 만들어서 DAO 메서드를 호출하도록 모두 변경해야 한다.

### 5.2.3 트랜잭션 동기화

비즈니스 로직을 담고 있는 UserService 메서드 안에서 트랜잭션의 경계를 설정해 관리하려면 지금까지 만들었던 정리된 코드를 포기해야 할까? 아니면, 트랜잭션 기능을 포기해야 할까?

물론 둘 다 아니다. 스프링은 이 딜레마를 해결할 수 있는 멋진 방법을 제공해준다.

#### Connection 파라미터 제거

먼저 Connection 파라미터로 직접 전달하는 문제를 해결해보자. Connection 오브젝트를 계속 메서드의 파라미터로 전달하다가 DAO를 호출할 때 사용하게 하는 건 피하고 싶다. 이를 위해 스프링이 제안하는 방법은 독립적인 **트랜잭션 동기화** 방식이다. 트랜잭션 동기화란 UserService에서 트랜잭션을 시작하기 위해 만든 Connection 오브젝트를 특별한 저장소에 보관해두고, 이후에 호출되는 DAO의 메서드에서는 저장된 Connection을 가져다가 사용하게 하는 것이다. 정확히는 DAO가 사용하는 JdbcTemplate 이 트랜잭션 동기화 방식을 이용하도록 하는 것이다.

<img width="534" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/dc7c9e65-4df5-44cc-b4c6-120ed0d4fce0">

(1) Userservice는 Connection을 생성하고 (2) 이를 트랜잭션 동기화 저장소에 저장 해두고 Connection의 setAutoCommit(false)를 호출해 트랜잭션을 시작시킨 후에 본 격적으로 DAO의 기능을 이용하기 시작한다. (3) 첫 번째 update() 메소드가 호출되 고, update() 메소드 내부에서 이용하는 JdbcTemplate 메소드에서는 가장 먼저 (4) 트 랜잭션 동기화 저장소에 현재 시작된 트랜잭션을 가진 Connection 오브젝트가 존재하 는지 확인한다. (2) upgradeLevels() 메소드 시작 부분에서 저장해둔 Connection을 발 견하고 이를 가져온다. 가져온 (5) Connection을 이용해 PreparedStatement를 만들 어 수정 SQL을 실행한다. 트랜잭션 동기화 저장소에서 DB 커넥션을 가져왔을 때는 JdbcTemplate은 Connection을 닫지 않은 채로 작업을 마친다. 이렇게 해서 트랜잭션 안에서 첫 번째 DB 작업을 마쳤다. 여전히 Connection은 열려 있고 트랜잭션은 진행 중인 채로 트랜잭션 동기화 저장소에 저장되어 있다.
(6) 두 번째 update()가 호출되면 이때도 마찬가지로 (7) 트랜잭션 동기화 저장소에서 Connection을 가져와 (8) 사용한다. (9) 마지막 update()도 (10) 같은 트랜잭션을 가진 Connection을 가져와 (11) 사용한다. 트랜잭션 내의 모든 작업이 정상적으로 끝났으면 Userservice는 이제 (12)
Connection의 commit()을 호출해서 트랜잭션을 완료시킨다. 마지막으로 (13) 트랜잭 션 저장소가 더 이상 Connection 오브젝트를 저장해두지 않도록 이를 제거한다. 어느 작 업 중에라도 예외상황이 발생하면 UserService는 즉시 Connection의 rollback()을 호 출하고 트랜잭션을 종료할 수 있다. 물론 이때도 트랜잭션 저장소에 저장된 동기화된 Connection 오브젝트는 제거해줘야 한다. 

**트랜잭션 동기화 저장소는 작업 스레드마다 독립적으로 Connection 오브젝트를 저장 하고 관리하기 때문에 다중 사용자를 처리하는 서버의 멀티스레드 환경에서도 충돌이 날 염려는 없다.** 이렇게 트랜잭션 동기화 기법을 사용하면 파라미터를 통해 일일이 Connection 오브젝트를 전달할 필요가 없어진다. 트랜잭션의 경계설정이 필요한 upgradeLevels() 에서만 Connection을 다루게 하고, 여기서 생성된 Connection과 트랜잭션을 DAO의 JdbcTemplate이 사용할 수 있도록 별도의 저장소에 동기화하는 방법을 적용하기만 하면 된다. 더 이상 로직을 담은 메소드에 Connection 타입의 파라미터가 전달될 필요도 없고, userDao의 인터페이스에도 일일이 JDBC 인터페이스인 Connection을 사용한다고 노출 할 필요가 없다.

#### 트랜잭션 동기화 적용

문제는 멀티스레드 환경에서도 안전한 트랜잭션 동기화 방법을 구현하는 일이 기술적으로 간단하지 않다는 점인데, 다행히도 스프링은 JdbcTemplate과 더불어 이런 트랜잭션 동기화 기능을 지원하는 간단한 유틸리티 메서드를 제공하고 있다.

아래는 트랜잭션 동기화 방법을 적용한 UserService 클래스 코드다.
```java
private DataSource dataSource;

public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }

public void upgradeLevels() throws SQLException {
    TransactionSynchronizationManager.initSynchronization(); // 트랜잭션 동기화 관리자를 이용해 동기화 작업을 초기화

    // DB 커넥션을 생성하고 트랜잭션을 시작, 이후의 DAO 작업은 모두 여기서 시작한 트랜잭션 안에서 진행
    Connection c = DataSourceUtils.getConnection(dataSource); // DB 커넥션 생성과 동기화를 함께 해주는 유틸리티 메서드
    c.setAutoCommit(false);

    try {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
        c.commit(); // 정상적으로 작업을 마치면 커밋
    } catch (Exception e) {
        c.rollback(); // 예외 발생시 롤백
        throw e;
    } finally {
        DataSourceUtils.releaseConnection(c, dataSource); // 스프링 유틸리티 메서드를 이용해 DB 커넥션을 안전하게 닫는다.

        // 동기화 작업 종료 및 정리
        TransactionSynchronizationManager.unbindResource(this.dataSource);
        TransactionSynchronizationManager.clearSynchronization();
    }
}
```

아래는 DataSource 빈에 대한 DI 설정 코드를 추가한 DaoFactory.java 코드이다.
```java
@Configuration
class DaoFactory {
    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:tcp://localhost/~/tobiSpringStudy");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        return dataSource;
    }

    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDaoJdbc = new UserDaoJdbc();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }

    @Bean
    public UserService userService() {
        UserService userService = new UserService();
        userService.setUserDao(userDao());
        userService.setDataSource(dataSource());
        return userService;
    }
}
```

UserService에서 DB 커넥션을 직접 다룰 때 DataSource가 필요하므로 DataSource빈에 대한 DI 설정을 해줘야 한다.

스프링이 제공하는 트랜잭션 동기화 관리 클래스는 TransactionSynchronizationManager 다. 이 클래스를 이용해 먼저 트랜잭션 동기화 작업을 초기화하도록 요청한다. 그리고 DataSourceUtils 에서 제공하는 getConnection() 메서드를 통해 DB 커넥션을 생성한다. DataSource에서 Connection을 직접 가져오지 않고, 스프링이 제공하는 유틸리티 메서드를 쓰는 이유는 이 DataSourceUtils의 getCOnnection() 메서드는 Connection 오브젝트를 생성해줄 뿐만 아니라 트랜잭션 동기화에 사용하도록 저장소에 바인딩해주기 때문이다.

#### 트랜잭션 테스트 보완

이제 트랜잭션이 적용됐는지 테스트를 해보자. 앞에서 만든 UserServiceTest 의 upgradeAllOrNothing() 테스트 메서드에 아래와 같이 dataSource 빈을 가져와 주입해주는 코드를 추가해야 한다. 테스트용으로 확장해서 만든 TestUserService는 UserService의 서브클래스이므로 UserService와 마찬가지로 트랜잭션 동기화에 필요한 DataSource를 DI 해줘야 하기 때문이다.

```java
    @Autowired
    private DataSource dataSource;

    // ...

    @Test
    public void upgradeAllOrNoting() {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(userDao); // userDao를 수동 DI한다.
        testUserService.setDataSource(dataSource);
    
        userDao.deleteAll();
    
        for (User user : users) {
            userDao.add(user);
        }
    
        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException | SQLException e) {
    
        }
    
        checkLevelUpgraded(users.get(1), false);
    }
```

위 테스트를 다시 실행해보자.

<img width="553" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/6365be3d-fd01-49a3-8ead-fda726fd3e6b">

이번엔 테스트가 성공했다. 이제 모든 사용자의 레벨 업그레이드 작업을 완료하지 못하고 작업이 중단되면 이미 변경된 사용자의 레벨도 모두 원래 상태로 돌아갈 것이다.

### JdbcTemplate과 트랜잭션 동기화

한 가지 궁금한 것이 있다. JdbcTemplate의 동작방식이다. 지금까지 JdbcTemplate은 update()나 query() 같은 JDBC 작업의 템플릿 메서드를 호출하면 직접 Connection을 생성하고 종료하는 일을 모두 담당한다고 설명했다. 테스트에서 특별한 준비 없이 DAO의 메서드를 직접 사용했을 때도 제대로 동작하는 것을 보면 스스로 Connection을 생성해서 사용한다는 사실을 알 수 있다.

JdbcTemplate은 만약 미리 생성돼서 트랜잭션 동기화 저장소에 등록된 DB 커넥션이나 트랜잭션이 없는 경우에는 JdbcTemplate이 직접 DB 커넥션을 만들고 트랜잭션을 시작해서 JDBC 작업을 진행한다. 반면에 upgradeLevels() 메서드에서처럼 트랜잭션 동기화를 시작해놓았다면 그때부터 실행되는 JdbcTemplate는 메서드에서는 직접 DB 커넥션을 만드는 대신 트랜잭션 동기화 저장소에 들어 있는 DB 커넥션을 가져와서 사용한다.

따라서 DAO를 사용할 때 트랜잭션이 굳이 필요 없다면 바로 호출해서 사용해도 되고, DAO 외부에서 트랜잭션을 만들고 이를 관리할 필요가 있다면 미리 DB 커넥션을 생성한 다음 트랜잭션 동기화를 해주고 사용하면 된다.