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