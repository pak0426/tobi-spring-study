# 6장 AOP

AOP는 IoC/DI, 서비스 추상화와 더불어 스프링의 3대 기반기술의 하나다.

스프링에 적용된 가장 인기 있는 AOP의 적용 대상은 바로 선언적 트랜잭션 기능이다. 서비스 추상화를 통해 많은 근본적인 문제를 해결했던 트랜잭션 경계설정 기능을 AOP를 이용해 깔끔한 방식으로 바꿔보자. 그리고 그
과정에서 AOP를 도입해야 했던 이유도 알아보자.

## 6.1 트랜잭션 코드의 분리

지금까지 서비스 추상화 기법을 적용해 트랜잭션 기술에 독립적으로 만들어줬고, 메일 발송 기술과 환경에도 종속적이지 않은 깔끔한 코드로 다듬어온 UserService이지만, 코드를 볼 때마다 여전히 찜찜한 구석이 있는 것은 어쩔 수 없다. 트랜잭션 경계 설정을 위해 넣은 코드 때문이다.

스프링이 제공하는 깔끔한 트랜잭션 인터페이스를 썼음에도 비즈니스 로직이 주인이어야 할 메서드 안에 이름도 길고 무시무시하게 생긴 트랜잭션 코드가 더 많은 자리를 차지하고 있는 모습이 못마땅하다. 하지만 논리적으로 따져봐도 트랜잭션의 경계는 분명 비즈니스 로직의 전후에 설정돼야 하는 것이 분명하니 UserService의 메서드에 두는 것을 거부할 명분이 없다.

### 6.1.1 메서드 분리

리스트 6-1에 나온 트랜잭션이 적용된 코드를 다시 한번 살펴보자.

<img width="594" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d1917e2a-2bac-46b6-9739-6542a868c272">

위 코드는 트랜잭션 경계설정 코드와 비즈니스 코드, 두 가지 종류로 구분되어 있음을 알 수 있다.

또, 이 코드의 특징은 트랜잭션 경계설정의 코드와 비즈니스 로직 코드 간에 서로 주고받는 정보가 없다는 점이다. 그렇다면 이 다른 성격의 코드를 두 개의 메서드로 분리할 수 있지 않을까?

```java
public void upgradeLevels() throws SQLException {
    // 트랜잭션 시작
    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
        upgradeLevelsInternal();
        transactionManager.commit(status);
    } catch (Exception e) {
        transactionManager.rollback(status);
        throw e;
    }
}

// 분리된 비즈니스 로직 코드. 트랜잭션을 적용하기 전과 동일하다.
private void upgradeLevelsInternal() {
    // 트랜잭션 안에서 진행되는 작업
    List<User> users = userDao.getAll();
    for (User user : users) {
        if (canUpgradeLevel(user)) {
            upgradeLevel(user);
        }
    }
}
```

테스트를 돌려보면 성공이다.  

### 6.1.2 DI를 이용한 클래스의 분리

비즈니스 로직을 담당하는 코드는 깔끔하게 분리돼서 보기 좋긴 하다. 그렇지만 여전히 트랜잭션을 담당하는 기술적인 코드가 버젓이 UserService 안에 자리잡고 있다. 어차피 서로 직접적으로 정보를 주고 받는 것이 없다면, 아예 트랜잭션 코드를 존재하지 않는 것처럼 사라지게 할 수 는 없을까?

#### DI 적용을 이용한 트랜잭션 분리

현재 UserService는 UserServiceTest에서 사용 중인데 실전에서는 다른 클래스나 모듈에서 이 UserService를 호출해 사용할 것이다.  
그런데 UserService는 클래스로 되어 있어 다른 코드에서 사용하면 직접 참조하게 된다. 이를 DI를 통한 참조로 변경하게 된다면 트랜잭션 기능이 빠진 UserService를 사용하게 될 것이다.

<img width="430" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/255475b2-688a-4766-adbd-69ed3336a8ec">

그래서 UserService 인터페이스의 구현 클래스를 만들어 넣도록 해보자.

<img width="435" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d0498323-5013-443c-bb07-b8a532a183b2">

위와 같이 구현 클래스를 클라이언트에 노출하지 않고 **런타임 시에 DI를 통해 적용하는 방법을 쓰는 이유는, 일반적으로 구현 클래스를 바꿔가면서 사용하기 위해서다.** 테스트 때는 필요에 따라 테스트 구현 클래스를, 정식 운영 중에는 정규 구현 클래스를 DI 해주는 방법처럼 한 번에 한 가지 클래스를 선택해서 적용하도록 되어 있다.

하지만 꼭 그래야 한다는 제약은 없다. 한 번에 두 개의 UserService 인터페이스 구현 클래스를 동시에 이용한다면 어떨까?  
지금 해결하려고 하는 문제는 UserService에는 순수하게 비즈니스 로직을 담고 있는 코드만 놔두고 트랜잭션 경계설정을 담당하는 코드를 외부로 빼내려는 것이다.

그래서 아래와 같은 구조를 생각해볼 수 있다. **UserService를 구현한 또 다른 구현 클래스를 만든다.**   
<span style="color:red">이 클래스는 사용자 관리 로직을 담고 있는 구현 클래스인 UserSerivceImpl 을 대신하는게 아닌 단지 트랜잭션 경계설정이라는 책임만 맡을 뿐이다.</span>

<img width="571" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/fd309573-d424-4df7-b924-79365e8b4197">

이 방법을 이용해 트랜잭션 경계설정 코드를 분리해낸 결과를 살펴보자.

#### UserService 인터페이스 도입

UserService의 이름을 UserServiceImpl로 이름을 변경한다.

```java
public interface UserService {
    void add(User user);
    void upgradeLevels();
}
```

인터페이스를 생성하고 UserServiceImpl에서 구현한다.

```java
public class UserServiceImpl implements UserService {

    private UserDao userDao;
    private PlatformTransactionManager transactionManager;
    private MailSender mailSender;
    
    // ...

    public void upgradeLevels() {
        // 트랜잭션 안에서 진행되는 작업
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }

    // ...
```

기존 upgradeLevels() 메서드를 지우고 upgradeLevelsInternal() 를 upgradeLevels()로 변경한다.

#### 분리된 트랜잭션 기능

이제 비즈니스 트랜잭션 처리를 담은 UserServiceTx를 만들어보자.

```java
public class UserServiceTx implements UserService {
    // UserService를 구현한 다른 오브젝트를 DI 받는다.
    UserService userService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    // DI 받은 UserService 오브젝트에 모든 기능을 위임한다.
    @Override
    public void add(User user) {
        userService.add(user);
    }

    @Override
    public void upgradeLevels() {
        userService.upgradeLevels();
    }
}
```

UserServiceTx는 UserService 인터페이스를 구현했으나 사용자 관리라는 비즈니스 로직을 전혀 갖지 않고 다른 UserService 구현 오브젝트에 기능을 위암한다. 이를 위해 UserService 오브젝트를 DI 받을 수 있도록 만든다.

이렇게 준비된 UserServiceTx에 트랜잭션의 경계설정이라는 부가적인 작업을 부여해보자.

```java
public class UserServiceTx implements UserService {
    private UserService userService;
    private PlatformTransactionManager transactionManager;

    public void setPlatformTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void add(User user) {
        userService.add(user);
    }

    @Override
    public void upgradeLevels() {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            userService.upgradeLevels();
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
```

PlatformTransactionManager의 프로퍼티를 추가해주고 트랜잭션을 제어할 수 있도록 추가하였다.