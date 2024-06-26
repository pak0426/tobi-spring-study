# 5. 서비스 추상화

자바에는 표준 스펙, 상용 제품, 오픈소스를 통틀어서 사용 방법과 형식은 다르지만 기능과 목적이 유사한 기술이 존재한다. 심지어는 같은 자바의 표준 기술 중에서도 플랫폼과 컨텍스트에 차이가 있거나 발전한 역사가 다르기 때문에 목적이 유사한 여러 기술이 공존하기도 한다. 환경과 상황에 따라서 기술이 바뀌고, 그에 따라 다른 API를 사용하고 다른 스타일의 접근 방법을 따라야 한다는 건 매우 피곤한 일이다.

5장에서는 지금까지 만든 DAO에 트랜잭션을 적용해보면서 스프링이 어떻게 성격이 비슷한 여러 종류의 기술을 추상화하고 이를 일관된 방법으로 사용할 수 있도록 지원하는지를 살펴볼 것이다.

## 5.1 사용자 레벨 관리 기능 추가

지금까지 만들었던 UserDao는 User 오브젝트에 담겨 있는 사용자 정보를 등록, 조회, 수정, 삭제하는 CRUD 라고 불리는 기초적인 작업만 가능하다. 사용자 정보를 DB에 넣고 빼는 것을 제외하면 어떤 비즈니스 로직도 갖고 있지 않다.

이제 여기에 간단한 비즈니스 로직을 추가해보자. 지금까지 만들었던 UserDao 를 다수의 회원이 가입할 수 있는 인터넷 서비스의 사용자 관리 모듈에 적용한다고 생각해보자. 사용자 관리 기능에는 정기적으로 사용자의 활동 내역을 참고해서 레벨을 조정해주는 기능이 필요하다.

구현해야할 비즈니스 로직은 다음과 같다.

- 사용자 레벨은 BASIC, SILVER, GOLD 세 가지 중 하나다.
- 사용자가 처음 가입하면 BASIC 레벨이 되며, 이후 활동에 따라서 한 단계씩 업그레이드될 수 있다.
- 가입 후 50회 이상 로그인을 하면 BASIC에서 SILVER 레벨이 된다.
- SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 된다.
- 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행된다. 변경 작업 전에는 조건을 충족하더라도 레벨의 변경이 일어나지 않는다.

### 5.1.1 필드 추가

Level enum

먼저 User 클래스에 사용자 레벨을 저장할 필드를 추가하자. 각 레벨을 코드화해서 숫자로 넣어보자.

```java
class User {
    private static final int BASIC = 1;
    private static final int SILVER = 2;
    private static final int GOLD = 3;
    
    int level;
    
    public void setLevel(int level) {
        this.level = level;
    }
}
```

BASIC, SILVER, GOLD 처럼 의미 있는 상수도 정의해놨으니 아래처럼 깔끔하게 코드를 작성할 수 있다.

```java
if(user1.getLevel() == User.BASIC) {
    user1.setLevel(UserSILVER);
}
```

문제는 level의 타입이 int이기 때문에 다음처럼 다른 종류의 정보를 넣는 실수를 해도 컴파일러가 체크해주지 못한다는 점이다.

```java
user1.setLevel(thoer.getSum());
user1.setLevel(1000);
```

위와 같이 다른 의미의 값이나 범위를 벗어나는 값을 넣을 위험도 잇다.

그래서 숫자 타입을 직접 사용하는 것보다 자바 5 이상에서 제공하는 enum 을 이용하는 게 안전하고 편리하다. 사용자 레벨로 사용할 이넘을 아래와 같이 정의한다.

```java
public enum Level {
    BASIC(1), SILVER(2), GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1: return BASIC;
            case 2: return SILVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unexpected value: " + value);
        }
    }
}
```

이렇게 만들어진 Level 이넘은 내부에는 DB에 저장할 int 타입의 값을 갖고 있지만, 겉으로는 Level 타입의 오브젝트이기 때문에 안전하게 사용할 수 있다. user1.setLevel(1000)과 같은 코드는 컴파일러가 타입이 일치하지 않는다는 에러를 내면서 걸러줄 것이다.

#### User 필드 추가

만든 Level 타입의 변수를 아래와 같이 User 클래스에 추가하자. 사용자 레벨 관리 로직에서 언급된 로그인 횟수와 추천수도 추가하자.

```java
class User {
    String id;
    String name;
    String password;
    Level level;
    int login;
    int recommend;

    public User(String id, String name, String password, Level level, int login, int recommend) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.level = level;
        this.login = login;
        this.recommend = recommend;
    }

    // getter & setter
    
    // ...
}
```

테이블에 컬럼을 추가해주자.

```sql
ALTER TABLE USERS ADD COLUMN LEVEL TINYINT NOT NULL;
ALTER TABLE USERS ADD COLUMN LOGIN INT NOT NULL;
ALTER TABLE USERS ADD COLUMN RECOMMEND INT NOT NULL;
```

#### UserDaoTest 테스트 수정

그리고 UserDaoTest도 생성자 에러가 나지 않게 수정해주자.

```java
public class UserDaoTet {
    @BeforeEach
    void setup() {
        // ...
        user1 = new User("a", "aUser","aUser", Level.BASIC, 1, 0);
        user2 = new User("b", "bUser","bUser", Level.SILVER, 55, 10);
        user3 = new User("c", "cUser","cUser", Level.GOLD, 100, 4);
    }
    
    // ...
}
```

다음은 UserDaoTest 테스트에서 두 개의 User 오브젝트 필드 값이 모드 같은지 비교하는 checkSameUser() 메서드에 새로 추가된 필드에 대한 테스트도 추가해주자.

```java
private void checkSameUser(User user1, User user2) {
    assertEquals(user1.getId(), user2.getId());
    assertEquals(user1.getName(), user2.getName());
    assertEquals(user1.getPassword(), user2.getPassword());
    assertEquals(user1.getLevel(), user2.getLevel());
    assertEquals(user1.getLogin(), user2.getLogin());
    assertEquals(user1.getRecommend(), user2.getRecommend());
}
```

테스트는 이제 준비됐다. 테스트 대상인 UserDaoJdbc는 아직 수정하지 않아서 테스트를 실행하면 실패가 날 것이다.

#### UserDaoJdbc 수정

이제 미리 준비된 테스트가 성공하도록 UserDaoJdbc 클래스를 수정할 차례다. 추가한 필드에 대한 변경을 적용하자.

```java
    private RowMapper<User> userMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));
        user.setLevel(Level.valueOf(rs.getInt("level")));
        user.setLogin(rs.getInt("login"));
        user.setRecommend(rs.getInt("recommend"));
        return user;
    };

    @Override
    public void add(final User user) {
        jdbcTemplate.update("insert into users(id, name, password, level, login, recommend) values (?, ?, ?, ?, ?, ?)", user.getId(), user.getName(), user.getPassword(), user.getLevel(), user.getLevel(), user.getRecommend());
    }
```

여기서 눈여겨볼 것은 Level 타입의 level 필드를 사용하는 부분이다. Level 이넘은 오브젝트이므로 DB에 저장될 수 있는 SQL 타입이 아니다. 따라서 DB에 저장 가능한 정수형 값으로 변환해줘야 한다. 따라서 미리 만들어둔 intValue() 메서드를 사용한다.

반대로 조회를 했을 경우, ResultSet 에서는 DB의 타입인 int로 level 정보를 가져온다. 이 값을 User의 setLevel() 메서드에 전달하면 타입이 일치하지 않는다는 에러가 발생할 것이다.

JDBC가 사용하는 SQL은 컴파일 과정에서 자동으로 검증이 되지 않는 단순 문자열에 불과하다. 따라서 SQL 문장이 완성돼서 DB에 전달되기 전까지는 문법 오류나 오탖차 발견하기 힘들다는 게 문제다. 미리미리 DB까지 연동되는 테스트를 잘 만들어 뒀기 때문에 SQL 문장에 사용될 필드 이름이 오타를 아주 빠르게 잡아낼 수 있었다. 그런데 테스트가 없는 채로 새로운 필드가 추가됐다면 어땠을까? 수정한 코드를 빌드하고 서버에 올려서 누군가 사용자의 정보를 읽고 쓰는 기능을 사용하기 전까지는 이런 오타조차 발견하기 힘들 것이다.

그래서 빠르게 실행 가능한 포괄적인 테스트를 만들어두면 이렇게 기능의 추가나 수정이 일어날 때 그 위력을 발휘한다. 테스트를 실행해보자. 결과는 성공이다.

### 5.1.2 사용자 수정 기능 추가

사용자 관리 비즈니스 로직에 따르면 사용자 정보는 여러 번 수정될 수 있다. 상식적으로 생각해봐도 기본키인 id를 제외한 나머지 필드는 수정될 가능성이 있다. 상식적으로 생각해봐도 기본키인 id를 제외한 나머지 필드는 수정될 가능성이 있다. 성능을 극대화하기 위해, 수정된느 필드의 종류에 따라서 수정용 DAO 메서드를 만들어야 할 때도 있다. 하지만 아직은 사용자가 정보가 단순하고 필드도 몇 개 되지 않으며 사용자 정보가 자주 변경되는 것도 아니므로 간단히 접근하자. 수정할 정보가 담긴 User 오브젝트를 전달하면 id를 참고해서 사용자를 찾아 필드 정보를 UPDATE 문을 이용해 모두 변경해주는 메서드를 하나 만들겠다.

#### 수정 기능 테스트 추가

만들어야 할 코드의 기능을 생각해볼 겸 아래와 같은 테스트를 작성한다.

```java
    @Test
    public void update() {
        userDao.deleteAll();
        
        userDao.add(user1);
        user1.setName("현민박");
        user1.setPassword("niceWeather");
        user1.setLevel(Level.GOLD);
        user1.setLogin(100);
        user1.setRecommend(999);
        userDao.update(user1);
        
        User updatedUser1 = userDao.get(user1.getId());
        checkSameUser(user1, updatedUser1);
    }
```

먼저 픽스처 오브젝트 하나를 등록한다. 그리고 id를 제외한 필드의 내용을 바꾼 뒤 update() 를 호출하낟. 이제 해당 id의 사용자 정보가 변경됐어야 한다. 다시 id로 조회해서 가져온 User 오브젝트와 수정된 픽스처 오브젝트를 비교한다.

그런데 user1이라는 텍스터 픽스처는 인스턴스 변수로 만들어놓은 것인데, 이를 직접 변경해도 될까? 상관 없다. 어차피 테슽트 메서드가 실행될 때마다 UserDaoTest 오브젝트는 새로 만들어지고, setUp() 메서드도 다시 불려서 초기화되기 때문이다.

#### UserDao와 UserDaoJdbc 수정

여기까지 만들고 나면 dao 변수의 타입인 UserDao 인터페이스에 update() 메서드가 없다는 컴파일 에러가 날 것이다. IDE의 자동수정 기능을 이용해 UserDao 인터페이스에 아래와 같이 update() 메서드를 추가한다.

```java
public interface UserDao {
    // ...
    public void update(User user1);
}
```

UserDao 인터페이스에 update() 를 추가하고 나면 이번에 UserDaoJdbc에서 메서드를 구현하지 않았다고 에러가 날 것이다. 

<img width="935" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/119efa1b-89f4-4847-8812-c2a9c0cdb7b8">

UserDaoJdbc에 update()를 구현하는 코드를 작성해주자.

```java
    @Override
    public void update(User user) {
        jdbcTemplate.update(
                "update users set name = ?, password = ?, LEVEL = ?, LOGIN = ?, RECOMMEND = ? where ID = ?",
                user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(),
                user.getId()
        );
    }

```

이제 테스트를 돌려서 결과를 확인해보자.

<img width="272" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/547af384-a21c-4bac-8f1a-c85cbf87c81e">

성공했다.

#### 수정 테스트 보완

테스트가 성공하는 것을 봤으니 이쯤에서 만족하고 다른 작업으로 넘어가도 좋을 것 같다. 하지만 꼼꼼한 개발자라면 이 테스트에 뭔가 불만을 가지고 의심스럽게 코드를 다시 살펴봐야 할 것이다. JDBC 개발에서 리소스 반환과 같은 기본 작업을 제외하면 가장 많은 실수가 일어나는 곳은 바로 SQL 문장이다. 필드 이름이나 SQL 키워드를 잘못 넣은거라면 테스트를 돌려보면 에러가 나니 쉽게 확인할 수 있다. 하지만 update() 메서드 테스트 코드로는 검증하지 못하는 오류가 있다.

바로 UPDATE 문장에서 WHERE 절을 빼먹는 경우에다. UPDATE는 WHERE가 없어도 아무런 경고 없이 정상적으로 동작하는 것처럼 보인다. 

이 문제를 해결할 방법을 생각해보자.

**첫 번째 방법은 JdbcTemplate의 update() 가 돌려주는 리턴 값을 확인하는 것이다.** JdbcTemplate의 update()는 UPDATE나 DELETE 같이 테이블 내용에 영향을 주는 SQL을 실행하면 영향받은 로우의 개수를 알려준다. UserDao의 add(), deleteAll(), update() 메서드의 리턴 타입을 int로 바꾸고 이 정보를 리턴하게 만들 수 있다.

**두 번째 방법은 테스트를 보강해서 원하는 사용자 외의 정보는 변경되지 않았음을 직접 확인하는 것이다.** 사용자를 두 명 등록해놓고, 그 중 하나만 수정한 뒤에 수정된 사용자와 수정하지 않은 사용자의 정보를 모두 확인하면 된다.

여기서는 두 번째 방법을 적용해보자.

확실하게 테스트하려면 UserDao update() 메서드의 SQL 문장에서 WHERE 부분을 빼보면 된다. 그래도 기존 update() 테스트는 성공할 것이고 테스트에 결함이 있다는 증거이다.

```java
    @Test
    public void update() {
        userDao.deleteAll();

        userDao.add(user1); // 수정할 사용자
        userDao.add(user2); // 수정하지 않을 사용자

        user1.setName("현민박");
        user1.setPassword("goodDay");
        user1.setLevel(Level.GOLD);
        user1.setLogin(100);
        user1.setRecommend(999);

        userDao.update(user1);

        User updatedUser1 = userDao.get(user1.getId());
        checkSameUser(user1, updatedUser1);
        User updatedUser2 = userDao.get(user2.getId());
        checkSameUser(user2, updatedUser2);
    }
```

update() 메서드의 SQL에서 WHERE를 빼먹었다면 이 테스트는 실패로 끝날 것이다. 테스트가 성공하도록 WHERE 조건문을 다시 넣어주자.

사용자 정보를 수정하는 기능을 추가했으니 이제 본격적인 사용자 관리 비즈니스 로직을 구현할 차례다.

### 5.1.3 UserService.upgradeLevels()

레벨 관리 기능을 구현하기는 어렵지 않다. 사용자 관리 로직은 어디다 두는 것이 좋을까? UserDaoJdbc는 적당하지 않다. DAO는 데이터 어떻게 가져오고 조작할지를 다루는 곳이지 비즈니스 로직을 두는 곳이 아니다. UserService를 만들어 비즈니스 로직을 작성해보자. UserService는 UserDao 인터페이스 타입으로 userDao 빈을 DI 받아 사용하게 만든다.

<img width="487" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/a06d5243-6db5-4f33-ab11-5169de33d068">

#### UserService 클래스와 빈 등록

```java
public class UserService {
    UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

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
        return userService;
    }
}
```

#### UserServiceTest 테스트 클래스

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    @Test
    public void bean() {
        Assertions.assertThat(userService).isNotNull();
    }
}
```

#### upgradeLevels() 메서드

사용자 레벨 관리 기능과 테스트 코드를 작성해보자.

```java
    public void upgradeLevels() {
        List<User> users = userDao.getAll();

        for (User user : users) {
            boolean changed = false;

            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
                user.setLevel(Level.SILVER);
                changed = true;
            } else if (user.getLevel() == Level.SILVER && user.getLogin() >= 31) {
                user.setLevel(Level.GOLD);
                changed = true;
            }

            if (changed) {
                userDao.update(user);
            }
        }

    }
```

테스트 코드를 작성해보자.

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    List<User> users;
    @Autowired
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("a", "aUser", "1234", Level.BASIC, 49, 0),
                new User("b", "bUser", "1234", Level.BASIC, 50, 0),
                new User("c", "cUser", "1234", Level.SILVER, 60, 29),
                new User("d", "dUser", "1234", Level.SILVER, 29, 30),
                new User("e", "eUser", "1234", Level.GOLD, 100, 0)
        );
    }

    @Test
    public void upgradeLevels() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(users.get(0), Level.BASIC);
        checkLevel(users.get(1), Level.SILVER);
        checkLevel(users.get(2), Level.SILVER);
        checkLevel(users.get(3), Level.GOLD);
        checkLevel(users.get(4), Level.GOLD);
    }

    public void checkLevel(User user, Level expectedLevel) {
        User userUpdated = userDao.get(user.getId());
        assertThat(userUpdated.getLevel()).isEqualTo(expectedLevel);
    }
}
```

실행결과 참임을 확인할 수 있다.

<img width="252" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/45420cda-65c3-4d87-a3ae-3759d4ef20dd">

### 5.1.4 UserService.add()

사용자 관리 비즈니스 로직에서 대부분 구현했지만 처음 가입하는 사용자는 기본적으로 BASIC 레벨이어야 한다는 부분을 구현해야 한다. UserDaoJdbc의 add() 메서드에 비즈니스 로직을 추가하기엔 이미 주어진 User 오브젝트를 DB에 넣고 읽는데에만 관심을 지니고 있다. 비즈니스적인 의미를 지닌 정보는 UserService에 이 로직을 만들어주면 관심사의 분리가 확실할거 같다.

이번에는 테스트부터 만들어보자.

1. 레벨이 미리 정해진 경우
2. 레벨이 비어있는 경우

```java
    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4);
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);
        
        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel()).isEqualTo(Level.GOLD);
        assertThat(userWithoutLevelRead.getLevel()).isEqualTo(Level.BASIC);
    }
```

### 5.1.5 코드 개선

비즈니스 로직의 구현을 모두 마쳤다. 코드를 다시 검토해보자.

- 중복된 부분은 없는가?
- 무엇을 하는 것인지 이해하기 불편하지 않나?
- 코드가 자신이 있어야 할 자리에 있나?
- 앞으로 변경이 일어난ㄴ다면 어떤 것이 있을 수 있고, 그 변화에 쉽게 대응할 수 있게 작성되어 있는가?

#### upgradeLevels() 메서드 코드의 문제점

- for 루프 속의 if/else if/else 블록들이 가독성이 안좋다. 플래그를 두고 확인하고 업데이트하는 방법도 깔끔하지 않다.

#### upgradeLevels() 리팩토링

가장 먼저 추상적인 레벨에서 로직을 작성해보자. 기존 메서드는 자주 변경될 가능성이 있는 구체적인 내용이 추상적인 로직의 흐름과 함께 섞여 있다.

```java
    public void upgradeLevels() {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }
```

위의 코드는 쉽게 읽을 수 있다.

- 모든 사용자 정보를 가져와 한 명씩 업그레이드 가능한지 확인
- 업그레이드 가능하다면 업그레이드 한다.

미구현된 메서드까지 구현해보자.

```java
    private boolean canUpgradeLevel(User user) {
        Level level = user.getLevel();
        switch (level) {
            case BASIC: return (user.getLogin() >= 50);
            case SILVER: return (user.getRecommend() >= 30);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown level: " + level);
        }
    }

    private void upgradeLevel(User user) {
        if (user.getLevel() == Level.BASIC) user.setLevel(Level.SILVER);
        else if (user.getLevel() == Level.SILVER) user.setLevel(Level.GOLD);
        userDao.update(user);
    }
```

메서드를 분리해두면 나중에 작업이 추가되더라도 어떤 곳을 수정할지 명확해진다.

하지만 upgradeLevel() 메서드가 맘에 들지 않는다. level 필드를 변경해주는 로직이 함께 있고 너무 노골적이다. 예외 처리도 안되어있다. 이것을 더 분리해보자.

```java
public enum Level {
    BASIC(1), SILVER(2), GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public Level nextLevel() {
        switch (this) {
            case BASIC: return SILVER;
            case SILVER: return GOLD;
            case GOLD: return null;
            default: throw new IllegalArgumentException("알 수 없는 등급");
        }
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1: return BASIC;
            case 2: return SILVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unexpected value: " + value);
        }
    }
}
```

위 코드는 다음 Level에 대한 관심사를 객체 그 자체인 Level에서 처리하는 메서드 nextLevel() 를 만든 코드이다. 이제 User에 Level에 존재하니까 User에서 다음 레벨을 확인할 수 있는 코드를 만들어주자.

```java
    public void upgradeLevel() {
        Level nextLevel = this.level.nextLevel();
        if (nextLevel == null) {
            throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다.");
        }
        else {
            this.level = nextLevel;
        }
    }
```

이렇게 작성하면 UserService의 업그레이드 레벨 코드는 아래와 같다.

```java
    private void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }
```

테스트를 돌려보면 성공이다. 

지금 개선한 코드를 살펴보면 각 객체와 메서드가 각각 자기 몫의 책임을 맡아 일을 하고 있다.

UserService, User, Level 이 내부 정보를 다루는 자신의 책임에 충실한 기능을 갖고 있으면서 필요가 생기면 이런 작업을 수행해달라고 서로 요청하는 구조다. 각자 자기 책임에 충실한 작업을 하고 있으니 

- 코드 이해가 쉽다. 
- 변경에 용이하며 확장에도 열려있다. 
- 각각을 독립저긍로 테스트하도록 만들면 테스트 코드도 단순해진다.

**객체 지향적인 코드는 다른 오브젝트의 데이터를 가져와서 작업하는 대신 데이터를 갖고 있는 다른 오브젝트에게 작업을 해달라고 요청한다.** 이게 기본이 되는 원리이다.

#### User 테스트

User의 간단한 로직을 추가했지만 이것도 테스트를 만드는 것이 좋다.

```java
class UserTest {
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    public void upgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() == null) continue;
            user.setLevel(level);
            user.upgradeLevel();
            assertThat(user.getLevel()).isEqualTo(level.nextLevel());
        }
    }

    @Test
    public void cannotUpgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() != null) continue;
            user.setLevel(level);

            assertThatThrownBy(() -> user.upgradeLevel()).isInstanceOf(IllegalStateException.class);
        }
    }
}
```

User 클래스에 대한 테스트는 스프링의 컨텍스트를 사용하지 않아도 된다. User 오브젝트는 스프링이 IoC로 관리해주는 오브젝트가 아니기 때문이다.

위 코드는 레벨을 업그레이드 할 수 있는 케이스와 예외 처리가 발생하는 케이스 2가지의 상황을 작성하였다.

#### UserServiceTest 개선

UserServiceTest 를 보면 checkLevel() 메서드를 호출할때 레벨 업그레이드 수행 후 다음 단계의 레벨이 무엇인지 직접 넣어주었다. 이것도 중복이다. 수정해보자.

```java
    @Test
    public void upgradeLevels() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(users.get(0), false);
        checkLevel(users.get(1), true);
        checkLevel(users.get(2), false);
        checkLevel(users.get(3), true);
        checkLevel(users.get(4), false);
    }

    private void checkLevel(User user, boolean upgraded) {
        User updatedUser = userDao.get(user.getId());
        if (upgraded) {
            assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel().nextLevel());
        } else {
            assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel());
        }
    }
```

아까는 업그레이드 됐는지 안됐는지 확인 할 수 없게 enum 으로 checkLevel() 메서드에 파라미터를 넘겼다. boolean 타입을 이용해 업그레이드 여부를 알리고 객체 내부에서 다음 레벨에 대한 메서드를 만들었기 때문에 그걸 사용해서 DB에서 새롭게 저장한 User들을 가져오고 변경되지 않은 User 객체에서 Level을 찾아 nextLevel() 메서드를 호출하여 비교하는 코드로 변경하였다.

```java
case BASIC: return (user.getLogin() >= 50); // UserService

new User("b", "bUser", "1234", Level.BASIC, 50, 0), // UserServiceTest
```

위와 같이 숫자가 중복되는것도 제거해줘야한다. 상수를 이용해보자.

```java
    private boolean canUpgradeLevel(User user) {
        Level level = user.getLevel();
        switch (level) {
            case BASIC: return (user.getLogin() >= MIN_LOGIN_COUNT_FOR_SILVER);
            case SILVER: return (user.getRecommend() >= MIN_LOGIN_COUNT_FOR_SILVER);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown level: " + level);
        }
    }
```

```java
    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("a", "aUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0),
                new User("b", "bUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0),
                new User("c", "cUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1),
                new User("d", "dUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD),
                new User("e", "eUser", "1234", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }
```

setUp() 에서 UserService의 상수 이용하는 곳은 기존 테스트 케이스를 만들때에 유용하고 가독성이 높아진걸 볼 수 있다.