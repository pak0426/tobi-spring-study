## 6.2 고립된 단위 테스트

가장 좋은 테스트 방법은 가능한 한 작은 단위로 쪼개서 테스트하는 것이다.

작은 단위의 테스트가 좋은 이유는 테스트가 실패했을 때 그 원인을 찾기 쉽기 때문이다. 반대로 테스트에서 오류가 발견됐을 때 그 테스트가 진행되는 동안 실행된 코드의 양이 많다면 그 원인을 찾기가 매우 힘들어질 수 있다.

### 6.2.1 복잡한 의존관계 속의 테스트

UserService 의 경우를 생각해보자. 현재 UserService의 구현 클래스들이 동작하려면 세 가지 타입의 의존 오브젝트가 필요하다. UserDao 타입의 오브젝트를 통해 DB와 데이터를 주고 받아야 하고, MailSender를 구혀한 오브젝트를 이용해 메일을 발송해야 한다. 마지막으로 트랜잭션 처리를 위해 PlatformTransactionManager와 커뮤니케이션이 필요하다.

<img width="609" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/0f292081-bd3e-42ba-96ef-a9d30f8c14db">

UserServiceTest가 테스트하고자 하는 대상인 UserService는 사용자 정보를 관리하는 비즈니스 로직의 구현 코드다. 따라서 UserService의 코드가 바르게 작성되어 있으면 성공하고, 아니라면 실패하면 된다. 따라서 테스트 단위는 UserService 클래스여야 한다.

하지만 UserService는 UserDao, TransactionManager, MailSender 라는 세가지 의존관계를 갖고 있다. 따라서 그 세 가지 의존관계를 갖는 오브젝트들이 테스트가 진행되는 동안에 같이 실행된다. 그것들과 함께 DB 드라이버, 서버, 네트워크 등 그 뒤에 존재하는 훨씬 더 많은 오브젝트와 환경, 서비스, 서버, 심지어 네트워크까지 함께 테스트하는 셈이 된다. 

따라서 이런 경우의 테스트는 준비하기 힘들고, 환경이 조금이라도 달라지면 동일한 테스트 결과를 내지 못할 수도 있으며, 수행 속도는 느리고 그에 따라 테스트를 작성하고 실행하는 빈도가 점차 떨어질 것이 분명하다.

### 6.2.2 테스트 대상 오브젝트 고립시키기

그래서 테스트의 대상이나 환경이나, 외부 서버, 다른 클래스의 코드에 종속되고 영향받지 않도록 고립시킬 필요가 있다. 테스트를 의존 대상으로부터 분리해서 고립시키는 방법은 MailSender에 적용해봤던 대로 테스트를 위한 대역으로 사용하는 것이다.

#### 테스트를 위한 UserServiceImpl 고립

UserService를 재구성해보면 아래 그림과 같은 구조가 될 것이다.

<img width="586" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/c989680e-8a8f-4228-90dc-f2cdab88753a">

```java
public class UserServiceImpl implements UserService {

    private UserDao userDao;
    private MailSender mailSender;
    
    // ...
}
```

UserDao는 단순히 테스트 대상의 코드가 정상적으로 수행되도록 도와주기만 하는 스텁이 아니라, 부가적인 검증 기능까지 가진 목 오브젝트로 만들었다. 그 이유는 고립된 환경에서 동작하는 upgradeLevels() 의 테스트 결과를 검증할 방법이 필요하기 때문이다.

UserServiceImpl()의 upgradeLevels() 메서드는 리턴 타입이 void이다. 따라서 메서드를 실행하고 그 결과를 받아서 검증하는 것이 불가능하다. 기존 코드에서는 UserService의 메서드를 실행하고 DB에서 결과를 가져와 검증했다.

그러나 의존 오브젝트나 외부 서비스에 의존하지 않는 고립된 테스트 방식으로 만든 UserServiceImpl은 아무리 그 기능이 수행돼도 그 결과가 DB에 남지 않기 때문에 테스트 중 DB에 결과가 반영되진 않지만 UserDao의 update() 메서드를 호출하는 것을 확인할 수 있다면, 그 결과가 DB에 반영될 것이라는 결론을 낼 수 있다.

#### 고립된 단위 테스트 활용

```java
    @Test
    public void upgradeLevels() {

        // 테스트 데이터 준비
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        // 메일 발송 여부 확인을 위해 목 오브젝트 DI
        MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);


        // 테스트 대상 실행
        userService.upgradeLevels();

        // DB에 저장된 결과 확인
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);


        // 목 오브젝트를 이용한 결과 확인
        List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }
```

#### UserDao 목 오브젝트

이제 실제 UserDao와 DB까지 직접 의존하고 있는 첫 번째와 네 번째의 테스트 방식도 목 오브젝트를 만들어서 적용해보겠다. 목 오브젝트는 기본적으로 스텁과 같은 방식으로 테스트 방식을 통해 사용될 때 필요한 기능을 지원해줘야 한다. upgradeLevels() 메서드가 실행되는 중에 UserDao와 어떤 정보를 주고받는지 입출력 내역을 먼저 확인할 필요가 있다.

아래에서 볼 수 있듯이, UserServiceImpl의 코드를 살펴보면 upgradeLevels() 메서드와 그 사용 메서드에서 UserDao를 사용하는 경우는 두 가지다.

```java
public void upgradeLevels() {
    List<User> users = userDao.getAll(); // UserDao 사용
    for (User user : users) {
        if (canUpgradeLevel(user)) {
            upgradeLevel(user);
        }
    }
}

protected void upgradeLevel(User user) {
    user.upgradeLevel();
    userDao.update(user); // UserDao 사용
    sendUpgradeEmail(user);
}

```

getAll()에 대해서는 스텁으로서, update()에 대해서는 목 오브젝트로서 동작하는 UserDao 타입의 테스트 대역이 필요하다. 이 클래스의 이름을 MockUserDao 라고 하자.

MockUserDao 의 코드는 아래와 같이 만들 수 있다. UserServiceTest 내부에 static 클래스로 선언한다.

```java
    static class MockUserDao implements UserDao {
        
        private List<User> users;
        private List<User> updated = new ArrayList<>();
        
        public MockUserDao(List<User> users) {
            this.users = users;
        }
        
        public List<User> getUpdated() {
            return this.updated;
        }

        @Override
        public List<User> getAll() {
            return this.users;
        }

        @Override
        public void update(User user) {
            updated.add(user);
        }

        @Override
        public void add(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException();
        }


        @Override
        public DataSource getDataSource() {
            throw new UnsupportedOperationException();
        }
    }
```

MockUserDao 에는 두 개의 User 타입 리스트를 정의해둔다. 하나는 생성자를 통해 전달받은 사용자 목록을 저장해뒀다가, **getAll() 메서드가 호출되면 DB에 가져온 것처럼 돌려주는 용도다. 목 오브젝트를 사용하지 않을 때는 일일이 DB에 저장했다가 다시 가져와야 했지만, MockUserDao 는 미리 준비된 테스트용 리스트를 메모리에 갖고 있다가 돌려주기만 하면 된다.** 다른 하나는 update() 메서드를 실행하면서 넘겨준 **업그레이드 대상 User 오브젝트를 저장해뒀다가 검증을 위해 돌려주기 위한 것이다.** upgradeLevels() 메서드가 실행되는 동안 업그레이드 대상으로 서넞ㅇ된 사용자가 어떤 것인지 확인하는 데 쓰인다.

이제 upgradeLevels() 테스트가 MockUserDao를 사용하도록 수정해보자.

```java
    @Test
    public void upgradeLevels() {
        // 고립된 테스트에서는 테스트 대상 오브젝트를 직접 생성하면 된다.
        UserServiceImpl userServiceImpl = new UserServiceImpl();

        // 목 오브젝트로 만든 UserDao를 직접 DI 해준다.
        MockUserDao mockUserDao = new MockUserDao(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        // MockUserDao 로부터 업데이트 결과를 가져온다.
        List<User> updated = mockUserDao.getUpdated();

        // 업데이트 횟수와 정보를 확인한다.
        assertThat(updated.size()).isEqualTo(2);
        checkUserAndLevel(updated.get(0), "b", Level.SILVER);
        checkUserAndLevel(updated.get(1), "d", Level.GOLD);

        List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }

    // id와 level을 확인하는 결과 메서드
    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assertThat(updated.getId()).isEqualTo(expectedId);
        assertThat(updated.getLevel()).isEqualTo(expectedLevel);
    }
```
기존 테스트는 @Autowired를 통해 가져온 UserService 타입의 빈이었다. 컨테이너에서 가져온 UserService 오브젝트는 DI를 통해서 많은 의존 오브젝트와 서비스, 외부 환경에 의존하고 있었다. 이제는 완전히 고립돼서 테스트만을 위해 만든 테스트 대상을 사용할 것이기 때문에 스프링 컨테이너에서 빈을 가져올 필요가 없다.

Mock으로 만든 테스트 스텁을 이용해서 테스트를 진행할 수 있는 코드를 작성했다.

#### 테스트 수행 성능의 향상

이제 UserServiceTest의 upgradeLevels() 테스트를 실행해보자. 테스트 결과는 물론 성공이다. 목 오브젝트를 사용하게 되면 DB, 네트워크 연결과 같은 부가적인 실행이 없기 때문에 테스트 시간이 줄어들게 된다.  
**고립된 테스트를 하면 테스트가 다른 의존 대상에 영향을 받을 경우를 대비해 복잡하게 준비할 필요가 없고, 테스트 수행 성능도 향상된다.**

### 6.2.3 단위 테스트와 통합 테스트

단위 테스트의 단위는 사용자 관리 기능 전체를 하나의 단위로 볼 수도 있고 하나의 클래스나 하나의 메서드를 단위로 볼 수도 있다. 중요한 것은 하나의 단위에 초점을 맞춘 테스트라는 점이다. 

**테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록 고립시켜서 테스트하는 것을 단위 테스트라고 부르겠다.**  
**반면에 두 개 이상의, 성격이나 계층이 다른 오브젝트가 연동하도록 만들어 테스트하거나, 또는 외부의 DB나 파일, 서비스 등의 리소스가 참여하는 테스트는 통합테스트라고 부르겠다.** 통합 테스트란 두 개 이상의 단위가 결합해서 동작하는 테스트가 수행되는 것이라고 보면 된다. **스프링의 테스트 컨텍스트 프레임워크를 이용해서 컨텍스트에서 생성되고 DI된 오브젝트를 테스트하는 것도 통합 테스트다.**

**단위 테스트**와 **통합 테스트** 중에서 어떤 방법을 쓸지는 어떻게 결정할 것인가?

- **항상 단위 테스트를 먼저 고려한다.**
- 하나의 클래스나 성격과 목적이 같은 긴밀한 클래스 몇 개를 모아서 **외부와의 의존관계를 모두 차단하고 필요에 따라 스텁이나 목 오브젝트 등의 테스트 대역을 이용**하도록 테스트를 만든다. 단위 테스트는 테스트 작성도 간단하고 실행 속도도 빠르며 테스트 대상 외의 코드나 환경으로부터 테스트 결과에 영향을 받지도 않기 때문에 가장 빠른 시간에 효과적인 테스트를 작성하기에 유리하다.
- **외부 리소스를 사용해야만 가능한 테스트는 통합 테스트로 만든다.**
- 단위 테스트로 만들기가 어려운 코드도 있다. 대표적인 게 DAO다. DAO는 그 자체로 로직을 담고 있기 보다는 DB를 통해 로직을 수행하는 인터페이스와 같은 역할을 한다. SQL을 JDBC를 통해 실행하는 코드만으로는 고립된 테스트를 작성하기 힘들다. 따라서 DAO는 DB까지 연동하는 테스트로 만드는 편이 효과적이다. DB를 사용하는 테스트는 DB에 테스트 데이터를 준비하고, DB를 직접 확인하는 등의 부가적인 작업이 필요하다.
- DAO 테스트는 DB라는 외부 리소스를 사용하기 때문에 통합 테스트로 분류된다. 하지만 코드에서 보자면 하나의 기능 단위를 테스트하는 것이기도 하다. DAO 테스트를 통해 충분히 검증해두면 DAO를 이용하는 코드는 DAO 역할을 스텁이나 목 오브젝트로 대체해서 테스트할 수 있다. 이후에 실제 DAO와 연동했을 때도 바르게 동작하리라고 확실할 수 있다. 물론 각각의 단위 테스트가 성공했더라도 여러 개의 단위를 연결해서 테스트하면 오류가 발생할 수 있다. 하지만 충분한 단위 테스트를 거친다면 통합 테스트에서 오류를 발생할 확률도 줄어들고 발생한다고 하더라도 쉽게 처리할 수 있다.
- 여러 개의 단위가 의존관계를 가지고 동작할 때를 위한 통합 테스트는 필요하다. 다만, 단위 테스트를 충분히 거쳤다면 통합 테스트의 부담은 상대적으로 줄어든다.
- **단위 테스트를 만들기 복잡한 코드는 처음부터 통합 테스트를 고려한다.** 이때도 통합 테스트에 참여하는 코드 중에서 가능한 한 많은 부분을 미리 단위 테스트로 검증해두는 게 유리하다.
- **스프링 테스트 컨텍스트 프레임워크를 이용하는 테스트는 통합 테스트다.** 가능하면 스프링의 지원 없이 직접 코드 레벨의 DI를 사용하면서 단위 테스트를 하는게 좋지만 **스프링의 설정 자체도 테스트 대상이고, 스프링을 이용해 좀 더 추상적인 레벨에서 테스트해야 할 경우도 있다.** 이럴 땐 스프링 컨텍스트 프레임워크를 이용해 통합 테스트를 작성한다.

