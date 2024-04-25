# 2.3 개발자를 위한 테스팅 프레임워크 JUnit

JUnit은 사실상 자바의 표준 테스팅 프레임워크라고 불릴만큼 폭넓게 사용되고 있다. 스프링을 학습하고 제대로 활용하려면 최소한의 JUnit 테스트 작성 방법과 실행 방법은 알고 있어야 한다.

스프링 프레임워크 자체도 JUnit 프레임워크를 이용해 테스트 해가며 개발됐다.

## 2.3.1 JUnit 테스트 실행 방법

JUnitCore를 이용해 테스트를 실행하고 콘솔에 출력된 메세지를 보고 결과를 확인하는 방법은 가장 간단하긴 하지만 테스트의 수가 많아지면 관리하기 힘들다. 가장 좋은 방법은 IDE에 내장된 JUnit 테스트 지원
도구를 사용하는 것이다.

### 빌트 툴

프로젝트의 빌드를 위해 ANT나 메이븐 같은 빌드 툴과 스크립트를 사용하고 있다면, 빌드 툴에서 제공하는 JUnit 플러그인이나 태스크를 이용해 JUnit 테스트를 실행할 수 있다.

## 2.3.2 테스트 결과의 일관성

지금까지 JUnit을 적용해서 깔끔한 테스트 코드를 만들었다. 하지만 아직도 개선할 부분이 있다.

가장 불편한 부분은, 매번 UserDaoTest 테스트를 실행하기 전에 DB의 USER 테이블 데이터를 모두 삭제해줘야 할 때였다.

UserDaoTest의 문제는 이전 테스트 때문에 DB에 등록된 중복 데이터가 있을 수 있다는 점이다. 가장 좋은 해결첵은 addAndGet() 테스트를 마치고 나면 테스트가 등록한 사용자 정보를 삭제해서, 테스트를
수행하기 이전 상태로 만들어주는 것이다. 그러면 테스트를 아무리 반복해도 같은 결과를 얻을 수 있다.

### deleteAll()의 getCount() 추가

#### deleteAll

첫 번째 추가할 것은 USER 테이블의 모든 레코드를 삭제해주는 기능이다.

```java
public void deleteAll() throws SQLException, ClassNotFoundException {
    this.c = connectionMaker.makeNewConnection();

    PreparedStatement ps = c.prepareStatement(
            "delete from users"
    );

    ps.executeUpdate();

    ps.close();
    c.close();
}
```

#### getCount

두 번째 추가할 것은 USER 테이블의 레코드 개수를 돌려준다.

```java
public int getCount() throws SQLException, ClassNotFoundException {
    this.c = connectionMaker.makeNewConnection();

    PreparedStatement ps = c.prepareStatement(
            "select count(*) from users"
    );

    ResultSet rs = ps.executeQuery();

    int count = 0;
    if (rs.next()) {
        count = rs.getInt(1);
    }

    rs.close();
    ps.close();
    c.close();

    return count;
}
```

### deleteAll() 과 getCount() 테스트

새로운 기능을 추가했으니, 추가한 기능에 대한 테스트도 만들어야 한다. 그런데 deleteAll()과 getCount() 메서드의 기능은 add()와 get()처럼 독립적으로 자동 실행되는 테스트를 만들기가 좀 애매하다. 그래서, 새로운 테스트를 만들기 보다는 기존에 addAndGet() 테스트를 확장하는 방법을 사용하는 편이 더 나을거 같다. 

```java
@Test
public void addAndGet() throws SQLException, ClassNotFoundException {
    ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    UserDao userDao = context.getBean("userDao", UserDao.class);

    userDao.deleteAll();
    assertEquals(userDao.getCount(), 0);

    User user = new User();
    user.setId("hmmini");
    user.setName("박현민");
    user.setPassword("1234");
    userDao.add(user);
    assertEquals(userDao.getCount(), 1);

    User user2 = userDao.get("hmmini");

    assertEquals(user2.getName(), user.getName());
    assertEquals(user2.getPassword(), user.getPassword());
}
```

### 동일한 결과를 보장하는 테스트

이제 테스트를 실행하면 성공했다고 나올 것이다. DB의 테이블을 삭제하는 작업 없이 다시 테스트를 하거나 반복해서 여러 번 실행해도 계속 성공할 것이다. 이제 매번 데이터를 삭제하는 번거로운 작업이 사라졌다.

단위 테스트는 항상 일관성 있는 결과가 보장돼야 한다는 점을 잊어선 안된다. DB에 남아있는 데이터와 외부 환경에 영향을 받지 말아야 하는 것은 물론이고, 테스트를 실행하는 순서를 바꿔도 동일한 결과가 보장되도록 만들어야 한다.


## 2.3.3 포괄적인 테스트

테스트를 안 만드는 것도 위험한 일이지만, 성의 없이 테스트를 만드는 바람에 문제가 있는 코드인데도 테스트가 성공하게 만드는 건 더 위험하다. 특히 한 가지 결과만 검증하고 마는 것은 상당히 위험하다.

### getCount() 테스트

좀 더 꼼꼼한 테스트를 만들어 보자. 이번엔 여러 개의 User를 등록해가면서 getCount()의 결과를 매번 확인해보겠다.


```java
@Test
public void count() throws SQLException, ClassNotFoundException {
    ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    UserDao userDao = context.getBean("userDao", UserDao.class);
    User user1 = new User("a", "aUser","aUser");
    User user2 = new User("b", "bUser","bUser");
    User user3 = new User("c", "cUser","cUser");

    userDao.deleteAll();
    assertEquals(userDao.getCount(), 0);

    userDao.add(user1);
    assertEquals(userDao.getCount(), 1);

    userDao.add(user2);
    assertEquals(userDao.getCount(), 2);

    userDao.add(user3);
    assertEquals(userDao.getCount(), 3);
}
```

<img width="820" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/799d6087-b4b2-42a6-aa27-c9e4e3483708">

테스스틀 전체 실행해볼 수 있다. 이 때 주의할 점은 테스트가 어떤 순서로 실행될지 알 수 없다는 것이다. JUnit은 특정한 테스트 메서드의 실행 순서를 보장해주지 않는다. 테스트의 결과가 테스트 실행 순서에 영향을 받는다면 테스트를 잘못 만든 것이다.

모든 테스트는 실행 순서 없이 독립적으로 항상 동일한 결과를 낼 수 있도록 해야 한다.

### addAndGet() 테스트 보완

이번엔 addAndGet() 테스트를 좀 더 보완해보자. User를 하나 더 추가해서 두 개의 User를 add()하고, 각 User의 id를 파라미터로 전달해서 get()을 실행하도록 만들어보자.

```java
    @Test
    public void addAndGet() throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);

        User user1 = new User("a", "aUser","aUser");
        User user2 = new User("b", "bUser","bUser");
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);
        
        userDao.add(user1);
        userDao.add(user2);
        assertEquals(userDao.getCount(), 2);

        User userGet1 = userDao.get("a");
        assertEquals(user1.getName(), userGet1.getName());
        assertEquals(user1.getPassword(), userGet1.getPassword());

        User userGet2 = userDao.get("b");
        assertEquals(user2.getName(), userGet2.getName());
        assertEquals(user2.getPassword(), userGet2.getPassword());
    }
```

### get() 예외조건에 대한 테스트

get() 메서드에 전달된 id 값에 해당하는 사용자 정보가 없다면 어떻게 될까? 두 가지 방법이 있을 것이다.

1. null과 같은 특별한 값을 리턴한다.
2. id에 해당하는 정보를 찾을 수 없다고 예외를 던진다.

여기서는 후자를 사용해보자.

```java
@Test
    public void getUserFailure() throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);

        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);
        Assertions.assertThrows(JdbcSQLNonTransientException.class, () -> userDao.get("unknownId"));
    }
```

### 포괄적인 테스트

사실 JDBC를 이용한 DAO를 개발해본 경험이 많은 개발자라면, 이 정도의 간단한 DAO는 굳이 이런 다양한 테스트를 하지 않고 코드만 살펴봐도 문제가 생기지 않으리라고 생각할 수 있다. 하지만 이렇게 DAO의 메서드에 대한 포괄적인 테스트를 만들어두는 편이 훨씬 안전하고 유용하다.

개발자가 테스트를 직접 만들 때 자주하는 실수가 성공하는 테스트만 골라서 만드는 것이다. 개발자는 머릿속으로 이 코드가 잘 돌아가는 케이스를 상상하면서 코드를 만드는 경우가 일반적이다. 그래서 테스트를 작성할 때도 문제가 될 만한 상황이나, 입력 값 등은 교모히도 잘 피해서 코드를 만드는 습성이 있다. 이건 테스트 코드를 통한 자동 테스트뿐 아니라, UI를 통한 수동 테스트를 할 때도 빈번하게 발생하는 문제다.

그래서 테스트를 작성할 때 부정적인 케이스를 먼저 만드는 습관을 들이는 게 좋다. get() 메서드의 경우라면, 존재하는 id가 주여졌을 때 해당 레코드를 정확히 가져오는가를 테스트하는 것도 중요하지만, 존재하지 않는 id가 주어졌을 때는 어떻게 반응할지를 먼저 결정하고, 이를 확인할 수 있는 테스트를 먼저 만들려고 한다면 예외적인 상황을 빠뜨리지 않는 꼼꼼한 개발이 가능하다.


## 2.3.4 테스트가 이끄는 개발

get() 메서드의 예외 테스트를 만드는 과정을 다시 돌아보면 흥미로운 점을 발견할 수 있다. 작업한 순서를 살펴보면 새로운 기능을 넣기 위해 UserDao 코드를 수정하고, 그런 당므 수정한 코드를 검증하기 위해 테스트를 만드는 순서로 진행한 것이 아니라 반대로 실패하는 것을 보고 UserDao 코드에 손대기 시작했다.

### 기능설계를 위한 테스트

우리가 한 작업을 돌이켜보면, 가장 먼저 **존재하지 않는 id로 get() 메서드를 실행하면 특정한 예외를 던져져야 한다.** 는 식으로 만들어야 할 기능을 결정했다. 그러고 나서 UserDao 코드를 수정하는 대신 getUserFailure() 테스트를 먼저 만들었다.

이것은 코드를 보고 어떻게 테스트할까 생각하면서 getUserFailure()를 만든 것이 아닌, 추가하고 싶은 기능을 코드로 표현하려고 했기 때문에 가능했다.

<img width="609" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/93c94de1-ae75-45a0-8bdc-524b992c0a42">

이렇게 비교해보면 이 테스트 코드는 마치 잘 작성된 하나의 기능 정의서처럼 보인다. 그래서 보통 기능 설계, 구현, 테스트라는 일반적인 개발 흐름의 기능설계에 해당하는 부분을 이 테스트 코드가 일부분 담당하고 있다고 볼 수 있다.

### 테스트 주도 개발

만들고자 하는 기능의 내용을 담고 있으면서 만들어진 코드를 검증도 해줄 수 있도록 테스트 코드를 먼저 만들고, 테스트를 성공하게 해주는 코드를 작성하는 개발 방법을 **테스트 주도 개발(TDD)** 라고 한다. TDD는 개발자가 테스트를 만들어가며 개발하는 방법이 주는 장점을 극대화한 방법이라고 할 수 있다.

TDD의 장점 중 하나는 코드를 만들어 테스트를 실행하는 그 사이의 간격이 매우 짧다는 점이다. 개발한 코드의 오류는 빨리 발견할수록 좋다. 빨리 발견된 오류는 쉽게 대응이 가능하기 때문이다. 테스트 없이 오랜 시간 동안 코드를 만들고 나서 테스트를 하면, 오류가 발생했을 때 원인을 찾기가 쉽지 않다.

## 2.3.5 테스트 코드 개선

지금까지 3개의 테스트 메서드를 만들었다. 이제 리팩토링해보자. 애플리케이션 코드만만이 리팩토링의 대상은 아니다. 필요하다면 테스트 코드도 언제든지 내부구조와 설계를 개선해서 좀 더 깔끔하고 이해하기 쉬우며 변경이 용이한 코드로 만들 필요가 있다.

테스트 코드 자체가 이미 자신에 대한 테스트이기 때문에 테스트 결과가 일정하게 유지된다면 얼마든지 리팩토링을 해도 좋다.

```java
ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
UserDao userDao = context.getBean("userDao", UserDao.class);
```

UserDaoTest를 살펴보면 위 코드가 중복되서 발생한다. 이 코드를 메서드 추출 리팩토링 말고 JUnit이 제공하는 기능을 활용해보자.

### @BeforeEach

중복됐던 코드를 아래와 같이 고쳐보자.

```java
class UserDaoTest {
    private UserDao userDao;

    @BeforeEach
    void setup() {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        userDao = context.getBean("userDao", UserDao.class);
    }

    @Test
    public void addAndGet() throws SQLException, ClassNotFoundException {
        User user1 = new User("a", "aUser","aUser");
        User user2 = new User("b", "bUser","bUser");
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);

        userDao.add(user1);
        userDao.add(user2);
        assertEquals(userDao.getCount(), 2);

        User userGet1 = userDao.get("a");
        assertEquals(user1.getName(), userGet1.getName());
        assertEquals(user1.getPassword(), userGet1.getPassword());

        User userGet2 = userDao.get("b");
        assertEquals(user2.getName(), userGet2.getName());
        assertEquals(user2.getPassword(), userGet2.getPassword());
    }

    @Test
    public void count() throws SQLException, ClassNotFoundException {
        User user1 = new User("a", "aUser","aUser");
        User user2 = new User("b", "bUser","bUser");
        User user3 = new User("c", "cUser","cUser");

        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);

        userDao.add(user1);
        assertEquals(userDao.getCount(), 1);

        userDao.add(user2);
        assertEquals(userDao.getCount(), 2);

        userDao.add(user3);
        assertEquals(userDao.getCount(), 3);
    }

    @Test
    public void getUserFailure() throws SQLException, ClassNotFoundException {
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);
        Assertions.assertThrows(JdbcSQLNonTransientException.class, () -> userDao.get("unknownId"));
    }
}
```
이 코드를 실행해보면 테스트가 모두 성공할 것이다. 위 코드를 이해하려면 JUnit 프레임워크가 테스트 메서드 를 실행하는 과정을 알아야 한다. 프레임워크는 스스로 제어권을 가지고 주도적으로 동작하고, 개발자가 만든 코드는 프레임워크에 의해 수동적으로 실행된다. 그래서 프레임워크에 사용되는 코드만으로는 실행 흐름이 보이지 않기 때문에 프레임워크가 어떻게 사용할지를 잘 이해하고 있어야 한다.

JUnit이 하나의 테스트 클래스를 가져와 테스트를 수행하는 방식은 다음과 같다.

1. 테스트 클래스에서 @Test가 붙은 public 이고 void형이며 파라미터가 없는 테스트 메서드를 모두 찾는다.
2. 테스트 클래스의 오브젝트를 하나 만든다.
3. @BeforeEach가 붙은 메서드가 있으면 실행한다.
4. @Test가 붙은 메서드를 하나 호출하고 결과를 저장한다.
5. @AfterEach가 붙은 메서드가 있으면 실행한다.
6. 나머지 테스트 메서드에 대해 2~5번 반복한다.
7. 모든 테스트의 결과를 종합해서 돌려준다.

실제로는 이보다 더 복잡하다.

@BeforeEach, @AfterEach 메서드를 테스트 메서드에서 직접 호출하지 않기 때문에 서로 주고받을 정보나 오브젝트가 있으면 인스턴스 변수를 이용해야 한다. 

테스트 메서드를 실행할 때마다 테스트 클래스의 오브젝트를 새로 만든다는 점이다. 한번 만들어진 테스트 클래스의 오브젝트는 하나의 테스트 메서드를 사용하고 나면 버려진다. 테스트 클래스가 @Test 테스트 메서드를 두 개 가지고 있다면, 테스트가 실행되는 중에 JUnit은 이 클래스의 오브젝트를 두 번 만들 것이다.

<img width="490" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/e63fd3af-174e-493e-b1d8-a6c676d7acf3">

왜 테스트 메서드를 실행할때 마다 새로운 오브젝트를 만드는 것일까?

JUnit 개발자는 각 테스트가 서로 영향을 주지 않고 독립적으로 실행됨을 확실히 보장하기 위해 새로운 오브젝트를 만들게 했다. 덕분에 인스턴스 변수도 부담없이 사용할 수 있다.


### 픽스처

테스트를 수행하는데 필요한 정보나 오브젝트를 **픽스처(fixture)** 라고 한다. 일반적으로 픽스처는 여러 테스트에서 반복적으로 사용되기 때문에 @BeforeEach 메서드를 이용해 생성해두면 편리하다. add() 메서드에 전달하는 User 오브젝트들도 픽스처라 볼 수 있다. 이걸 @BeforeEach 메서드로 추출해보자.

```java
class UserDaoTest {
    private UserDao userDao;
    private User user1;
    private User user2;
    private User user3;


    @BeforeEach
    void setup() {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        userDao = context.getBean("userDao", UserDao.class);

        user1 = new User("a", "aUser", "aUser");
        user2 = new User("b", "bUser", "bUser");
        user3 = new User("c", "cUser", "cUser");
        
        // ...
    }
    
    // ...
}
```