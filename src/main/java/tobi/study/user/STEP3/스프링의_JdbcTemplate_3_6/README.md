## 3.6 스프링의 JdbcTemplate

템플릿과 콜백의 기본적인 원리와 동작방식, 만드는 방법을 알아봤으니 이번에는 스프링이 제공하는 템플릿/콜백 기술을 살펴보자. 스프링은 JDBC를 이용하는 DAO에서 사용할 수 있도록 준비된 다양한 템플릿과 콜백을 제공한다.
거의 모든 종류의 JDBC 코드에 사용 가능한 템플릿과 콜백을 제공할 뿐만 아니라, 자주 사용되는 패턴을 가진 콜백은 다시 템플릿에 결합시켜서 간단한 메서드 호출만으로 사용이 가능하도록 만들어져 있기 때문에 템플릿/콜백 방식의 기술을 사용하고 있는지 모르고도 쓸 수 있을 정도로 편리하다.

스프링이 제공하는 JDBC 코드용 기본 템플릿은 JdbcTemplate이다. 앞에서 만들었던 JdbcContext와 유사하지만 훨씬 강력하고 편리한 기능을 제공해준다. 지금까지 만들었던 JdbcContext는 버리고 스프링의 JdbcTemplate를 사용해보자.

```java
class UserDao {
    private User user;

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    // ...
}
```

### 3.6.1 update()

deleteAll()에 먼저 적용해보자. deleteAll()에 처음 적용했던 콜백은 StatementStrategy 인터페이스의 makePreparedStatement() 메서드다. 이에 대응되는 JdbcTemplate의 콜백은 PreparedStatementCreator 인터페이스의 createPreparedStatement() 메서드다. 템플릿으로부터 Connection을 제공받아 PreparedStatement를 만들어 돌려준다는 면에서 구조는 동일하다. PreparedStatementCreator 타입의 콜백을 받아서 사용하는 JdbcTemplate의 템플릿 메서드는 update() 다.

```java
    public void deleteAll() throws SQLException {
        jdbcTemplate.update(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        return con.prepareStatement("delete from users");
                    }
                }
        );
    }

```

앞에서 만들었던 executeSql()은 SQL 문장만 전달하면 미리 준비된 콜백을 만들어 템플릿을 호출하는 것까지 한 번에 해주는 편리한 메서드였다. JdbcTemplate 에도 기능이 비슷한 메서드가 존재한다. 콜백을 받는 update() 메서드와 이름은 동일한데 파라미터로 SQL 문장을 전달한다는 것만 다르다. 

아래 코드는 내장 콜백을 사용하는 update()로 변경한 것이다.

```java
    public void deleteAll() throws SQLException {
        jdbcTemplate.update("delete from users");
    }
```

JdbcTemplate은 앞에서 구상만 해보고 만들지는 못했던 add() 메서드에 대한 편리한 메서드도 제공된다. 치환자를 가진 SQL로 PreparedStatement 를 만들고 함께 제공하는 파라미터를 순서대로 바인딩해주는 기능을 가진 update() 메서드를 사용할 수 있다. SQL과 함께 가변인자로 선언된 파라미터를 제공해주면 된다.

현재 add() 메서드에서 만드는 콜백은 아래와 같이 PreparedStatement를 만드는 것과 파라미터를 바인딩하는 두 가지 작업을 수행한다.

```java
PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
ps.setString(1, user.getId());
ps.setString(2, user.getName());
ps.setString(3, user.getPassword());
```

이를 JdbcTemplate에서 제공하는 편리한 메서드로 바꿔보면 다음과 같이 간단하게 바꿀 수 있다.

```java
public void add(final User user) throws SQLException {
    jdbcTemplate.update("insert into users(id, name, password) values (?, ?, ?)", user.getId(), user.getName(), user.getPassword());
}
```

JdbcContext를 이용하던 UserDao 메서드를 모두 스프링이 제공하는 JdbcTemplate으로 변경했다. 


### 3.6.2 queryForInt()

다음은 템플릿/콜백 방식을 적용하지 않았던 메서드에 JdbcTemplate 을 적용해보자.

getCount()는 SQL 쿼리를 실행하고 ResultSet 을 통해 결과 값을 가져오는 코드다. 이런 작업 흐름을 가진 코드에서 사용할 수 있는 템플릿은 PreparedStatementCreator 콜백과 ResultSetExtractor 콜백을 파라미터로 받는 query() 메서드다. 

PreparedStatementCreator는 update() 에서 사용해봤으니 그 용도를 잘 알 것이다. ResultSetExtractor는 PreparedStatement 쿼리를 실행해서 얻은 ResultSet을 전달받는 콜백이다. ResultSetExtractor 콜백은 템플릿이 제공하는 ResultSet을 이용해 원하는 ㄱ밧을 추출햇 ㅓ템플릿에 전달하면 템플릿은 나머지 작업을 수행한 뒤에 그 값을 query() 메서드의 리턴 값으로 돌려준다.

콜백이 두 개 등장하는 조금 복잡해 보이는 구조이지만 템플릿/콜백의 동작방식을 잘 생각해보면 어렵지 않게 이해할 수 있다. 첫 번째 PreparedStatementCreator 콜백은 템플릿으로부터 Connection을 받고 PreparedStatement를 돌려준다. 두 번째 ResultSetExtractor는 템플릿으로부터 ResultSet을 받고 거기서 추출한 결과를 돌려준다.

```java
    public int getCount() throws SQLException {
        return jdbcTemplate.query(new PreparedStatementCreator() {
                                      @Override
                                      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                                          return con.prepareStatement("select count(*) from users");
                                      }
                                  }, new ResultSetExtractor<Integer>() {
                                      @Override
                                      public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                                          rs.next();
                                          return rs.getInt(1);
                                      }
                                  }
        );
    }
```

콜백을 만들어보면 익명 내부 클래스가 두 번이나 등장을 한다. 원래 getCount() 메서드에 있던 코드 중에서 변하는 부분만 콜백으로 만들어져서 제공된다고 생각하면 이해하기 쉽다. 앞에서 만들었던 lineReadTemplate()과 유사하게 두 번째 콜백에서 리턴하는 값은 결국 템플릿 메서드의 결과로 다시 리턴된다. 원래 클라이언트/템플릿/콜백의 3단계 구조이니, 콜백을 만들어낸 결과는 템플릿을 거쳐야만 클라이언트인 getCount() 메서드로 넘어오는 것이다.

또 한가지 ResultSetExtractor 는 제네릭스 타입 파라미터를 갖는다는 점이다. lineReadTemplate()과 LineCallback 에 적용해봤던 방법과 동일하다. Result 에서 추출할 수 있는 값의 타입은 다양하기 때문에 타입 파라미터를 사용한 것이다. ResultSetExtractor 콜백에 지정한 타입은 제네릭 메서드에 적용돼서 query() 템플릿의 리턴 타입도 함께 바뀐다.

위 코드는 재사용하기 좋은 구조이다. SQL을 가지고 PreparedStatement 를 만드는 첫 번째 콜백은 이미 재사용 방법을 알아봤다. 두 번째 콜백도 간단하다ㅏ. SQL의 실행 결과가 하나의 정수 값이 되는 경우는 자주 볼 수 있다. 클라이언트에서 콜백의 작업을 위해 특별히 제공할 값도 없어서 단순하다. 손쉽게 ResultSetExtractor 콜백을 템플릿 안으로 옮겨 재활용할 수 있다.

JdbcTemplate 은 이런 기능을 가진 콜백을 내장하고 있는 queryForInt()라는 편리한 메서드를 제공한다. Integer 타입의 결과를 가져올 수 있는 SQL 문장만 전달해주면 된다. 

<br>

> Spring 3.2.2 버전 이후로 queryForInt와 queryForLong 같은 메소드들은 제거되었고, 대신 queryForObject 메소드를 사용하도록 변경되었습니다. queryForInt 메소드를 대체하기 위해서는 queryForObject 메소드를 사용하며, 반환되는 결과의 타입을 명시해야 합니다.

<br>

```java
public int getCount() throws SQLException {
    return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
}
```

### 3.6.3 queryForObject()

이번엔 get() 메서드에 JdbcTemplate를 적용해보자. get() 메서드는 지금까지 만들었던 것 중에 가장 복잡하다.

1. 일단 SQL은 바인딩이 필요한 치환자를 갖고 있다. -> 이것까지는 add()에서 사용했던 방법을 적용하면 될 것 같다.
2. 남은 것은 ResultSet에서 getCount() 처럼 단순한 값이 아니라 복잡한 User 오브젝트로 만드는 작업이다. ResultSet의 결과를 User 오브젝트로 만들어 프로퍼티로 넣어줘야 한다.

이를 위해, getCount()에 적용했던 ResultSetExtractor 콜백 대신 RowMapper 콜백을 사용하겠다. ResultSetExtractor 와 RowMapper 모두 템플릿으로부터 ResultSet 을 전달받고, 필요한 정보를 추출해서 전달하는 방식으로 동작한다.

**다른 점은 ResultSetExtractor 는 ResultSet 을 한 번 전달받아 알아서 추출 작업을 모두 진행하고 최종 결과만 리턴해주면 되는 데 반해, RowMapper 는 ResultSet 의 로우 하나를 매핑하기 위해 사용되기 때문에 여러 번 호출 될 수 있다는 점이다.**

기본 값으로 조회하는 get() 메서드는 SQL의 실행 결과가 로우가 하나인 ResultSet 이다. 따라서 첫 번째 로우에 RowMapper를 적용하도록 만들면 된다. RowMapper 콜백은 첫 번째 로우에 담긴 정보를 하나의 User 오브젝트에 매핑하게 해주면 된다. 이번에 사용할 템플릿 메서드는 queryForObject() 이다.

```java
    public User get(String id) throws SQLException, ClassNotFoundException {
        jdbcTemplate.queryForObject("select * from users where id = ?", new Object[] {id}, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                User user = new User();
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));
                return user;
            }
        });
    }
```

- 첫 번째 파라미터는 PreparedStatement 를 만들기 위한 SQL이고, 
- 두 번째는 여기에 바인딩할 값들이다. update()에서처럼 가변 인자를 사용하면 좋겠지만 뒤에 다른 파라미터가 있기 때문에 이 경우엔 가변인자 대신 Object 타입 배열을 사용해야 한다. 배열 초기화 블럭을 사용해서 `SQL의 ?`에 바인딩할 id 값을 전달한다. queryForObject() 내부에서 이 두 가지 파라미터를 사용하는 PreparedStatement 콜백이 만들어질 것이다.

queryForObject()는 SQL을 실행하면 한 개의 로우만 얻을 것이라고 기대한다. 그리고 ResultSet의 next()를 실행해서 첫 번째 로우로 이동시킨 후에 RowMapper 콜백을 호출한다. 이미 RowMapper가 호출되는 시점에서 ResultSet은 첫 번째 로우를 가리키고 있으므로 다시 rs.next()를 호출할 필요는 없다. RowMapper에서는 현재 ResultSet이 가리키고 있는 로우의 내용을 User 오브젝트에 그대로 담아서 리턴해주기만 하면 된다. RowMapper가 리턴한 User 오브젝트는 queryForObject() 메서드의 리턴 값으로 get() 메서드에 전달된다.

이렇게만 해도 User 오브젝트를 조회하는 get() 메서드의 기본 기능은 충분히 구현됐다. 하지만 한 가지 더 고려해야 할 게 있다. 기존의 get() 메서드는 조회 결과가 없을 때 EmptyResultDataAccessException을 던지도록 만들었다. 이 예외상황에 대한 테스트까지 만들어뒀다.

그렇다면 queryForObject() 를 이용할 때는 조회 결과가 없는 예외 상황을 어떻게 처리할까? 결론은 특별히 해줄 것은 없다. 이미 queryForObject()는 SQL을 실행해서 받은 로우의 개수가 하나가 아니라면 예외를 던지도록 만들어져 있다. 이 때 던져니는 예외 역시 EmptyResultDataAccessException 이다.

### 3.6.4 query()

#### 기능 정의와 테스트 작성

RowMapper 를 좀 더 사용해보자. 현재 등록되어 있는 모든 사용자 정보를 가져오는 getAll() 메서드를 추가한다. getAll()은 테이블의 모든 로우를 가져오면 된다. 그렇다면 어떤 포멧으로 변환하는 것이 좋을까? 여러 개라면 User 오브젝트의 컬렉션으로 만든다. List<User> 타입으로 돌려주는 게 가장 나을 것 같다. 리스트에 담는 순서는 어떻게 할까? 순서를 지정하지 않고 가져올 수도 있겠지만 그보다는 기본키인 id 순으로 정렬해서 가져오도록 만들자. 이번에도 테스트를 먼저 만들어 보자.

```java
    @Test
    public void getAll() throws SQLException {
        userDao.deleteAll();
        userDao.add(user1);
        List<User> users1 = userDao.getAll();
        checkSameUser(user1, users1.get(0));
        
        userDao.add(user2);
        List<User> users2 = userDao.getAll();
        checkSameUser(user1, users2.get(0));
        checkSameUser(user2, users2.get(1));

        userDao.add(user3);
        List<User> users3 = userDao.getAll();
        checkSameUser(user1, users3.get(0));
        checkSameUser(user2, users3.get(1));
        checkSameUser(user3, users3.get(1));
    }
    
    private void checkSameUser(User user1, User user2) {
        assertEquals(user1.getId(), user2.getId());
    }
```

### query() 템플릿을 이용하는 getAll() 구현

이제 이 테스트를 성공시키는 getAll() 멧더ㅡ를 만들어보자. 이번에는 JdbcTemplate 의 query() 메서드를 사용하겠다. 앞에서 사용한 queryForObject()는 쿼리의 결과가 로우 하나일 때 사용하고, query()는 여러 개의 로우가 결과로 나오는 일반적인 경우에 쓸 수 있다. query()의 리턴 타입은 List<T> 이다. query()는 제네릭 메서드로 타입은 파라미터로 넘기는 RowMapper<T> 콜백 오브젝트에서 결정된다.

```java
public List<User> getAll() {
    return jdbcTemplate.query("select * from users", new RowMapper<>() {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
        }
    });
}
```

첫 번째 파라미터에는 실행할 쿼리를 넣는다. 바인딩할 파라미터가 있다면 두 번째 파라미터에 추가할 수도 있다. 파라미터가 없다면 생략할 수 있다. 마지막 파라미터는 RowMapper 콜백이다. 

**query() 템플릿은 SQL을 실행해서 얻은 ResultSet의 모든 로우를 열람하면서 로우마다 RowMapper 콜백을 호출한다.** SQL 쿼리를 실행해 DB에서 가져오는 로우의 개수만큼 호출될 것이다. RowMapper는 현재 로우의 내용을 User 타입 오브젝트에 매핑해서 돌려준다. 이렇게 만들어진 User 오브젝트는 템플릿이 미리 준비한 List<User> 컬렉션에 추가된다. 모든 로우에 대한 작업을 마치면 모든 로우에 대한 User 오브젝트를 담고 있는 List<User> 오브젝트가 리턴된다.

#### 테스트 보완

성공적인 테스트 결과를 보면 빨리 다음 기능으로 넘어가고 싶겠지만 너무 서두르는 것은 좋지 않다. 항상 빠진 것은 없는지 개선할 부분은 없는지 한 번쯤 생각해보자.

get()과 마찬가지로 getAll()에서도 예외적인 조건에 대한 테스트를 빼먹지 말아야 한다.

```java
    @Test
    public void getAll() throws SQLException {
        userDao.deleteAll();

        List<User> users0 = userDao.getAll();
        assertEquals(users0.size(), 0);
        
        // ...
    }
```

이런 테스트를 짜면 질문을 해볼 수 있다. 이미 JdbcTemplate의 query() 메서드가 예외적ㅇ니 경우에는 크기가 0인 리스트 오브젝트를 리턴하는 것으로 정해져 있다. 그런데 getAll() 에서 query()의 결과에 손댈 것도 아니면서 굳이 검증 코드를 추가해야 할까?

**물론이다. 테스트 코드를 만드는게 좋다. UserDao를 사용하는 쪽의 입장에서 생각해본다면 getAll()이 내부적으로 JdbcTemplate를 사용하는지, 개발자가 직접 만든 JDBC 코드를 사용하는지 알 수 없고 알 필요도 없다.** getAll()이라는 메서드가 어떻게 동작하는지에만 대한 관심이 있는 것이다. userDaoTest 클래스의 테스트는 UserDao의 getAll() 이라는 메서드에 기대하는 동작방식에 대한 검증이 먼저다. 따라서 그 예상되는 결과를 모두 검증하는 게 옳다.

### 3.6.5 재사용 가능한 콜백의 분리

테스트는 이제 충분한듯하니 이쯤에서 UserDao 코드를 한번 살펴보자. UserDao 전체가 처음 try/catch/finally 를 덕지덕지 붙여 만들었을 때의 메서드 한 개 분량밖에는 안된다. 코드의 양이 줄었을 뿐 아니라 각 메서드의 기능을 파악하기도 쉽게 되어 있다. 핵심적인 SQL 문장과 파라미터, 그리고 생성되는 결과의 타입정보만 남기고 모든 판에 박힌 로우레벨 중복코드는 깔끔하게 제거됐기 때문이다.

#### DI를 위한 코드 정리

이제 필요 없어진 DataSource 인스턴스 변수는 제거하자. UserDao의 모든 메서드가 JdbcTempate을 이용하도록 만들었으니 DataSource를 직접 사용할 일은 없다. 단지 JdbcTemplate을 생성하면서 직접 DI 해주기 위해 필요한 DataSource를 전달받아야 하니 수정자 메서드는 남겨둔다.

```java
class UserDao {
    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}
```
JdbcTemplate을 직접 스프링 빈으로 등록하는 방식을 사용하고 싶다면 setDataSource를 setJdbcTemplate으로 바꿔주기만 하면 된다.

#### 중복 제거

다음은 중복된 코드가 업나 살펴보자. 웬만한 JDBC의 템플릿성 코드나 반복적인 콜백 코드도 모두 JdbcTemplate의 도움으로 제거했으니 메서드 단위에서 보자면 줄일 만한 것은 없다. 하지만 get()과 getAll()을 보면 사용한 RowMapper의 내용이 똑같다는 사실을 알 수 있다. 따라서 User용 RowMapper 콜백을 메서드에서 분리해 중복을 없애고 재사용되게 만들어야 한다.

```java
class UserDao {
    private JdbcTemplate jdbcTemplate;

    private RowMapper<User> userMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
        }
    };

    // ...

    public User get(String id) throws SQLException, ClassNotFoundException {
        return jdbcTemplate.queryForObject("select * from users where id = ?", new Object[]{id}, rowMapper);
    }

    public List<User> getAll() {
        return jdbcTemplate.query("select * from users", rowMapper);
    }

    // ...
}
```

위와 같이 코드를 짜고 테스트를 해보자. 문제없이 성공이다.

### 템플릿/콜백 패턴과 UserDao

아래 코드는 최종적으로 완성된 UserDao 클래스다. 템플릿/콜백 패턴과 DI를 이용해 예외처리와 리소스 관리, 유연한 DataSource 활용 방법까지 제공하면서도 군더더기 하나 없는 깔끔하고 간결한 코드로 정리할 수 있게 됐다.

```java
class UserDao {
    private JdbcTemplate jdbcTemplate;

    private RowMapper<User> userMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
        }
    };

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void add(final User user) throws SQLException {
        jdbcTemplate.update("insert into users(id, name, password) values (?, ?, ?)", user.getId(), user.getName(), user.getPassword());
    }

    public User get(String id) throws SQLException, ClassNotFoundException {
        return jdbcTemplate.queryForObject("select * from users where id = ?", new Object[]{id}, rowMapper);
    }

    public void deleteAll() throws SQLException {
        jdbcTemplate.update("delete from users");
    }

    public int getCount() throws SQLException {
        return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
    }

    public List<User> getAll() {
        return jdbcTemplate.query("select * from users", rowMapper);
    }
}
```

**UserDao에는 User 정보를 DB에 넣거나 가져오거나 조작하는 방법에 대한 핵심적인 로직만 담겨 있다.** User라는 자바 오브젝트와 User 테이블 사이에 어떻게 정보를 주고 받을지, DB와 커뮤니케이션하기 위한 SQL 문장이 어떤 것인지에 대한 최적화된 코드를 갖고 있다. 만약 사용할 테이블과 필드 정보가 바뀌면 UserDao의 거의 모든 코드가 함께 바뀐다. 따라서 응집도가 높다고 볼 수 있다.

반면에 JDBC API를 사용하는 방식, 예외처리, 리소스의 반납, DB 연결을 어떻게 가져올지에 관한 책임과 관심은 모두 JdbcTemplate 에게 있다. 따라서 변경이 일어난다고 해도 UserDao 코드에는 아무런 영향을 주지 않는다. 그런 면에서 책임이 다른 코드와는 낮은 결합도를 유지하고 있다. 다만 JdbcTemplate이라는 템플릿 클래스를 직접 이용한다는 면에서 특정 템플릿/콜백 구현에 대한 강한 결합을 갖고 있다. JdbcTemplate이 스프링에서 JDBC를 이용해 DAO를 만드는 데 사용되는 사실상 표준 기술이고, JDBC 대신 다른 데이터 액세스 기술을 사용하지 않는 한 바뀔 리도 없겠지만, 그래도 더 낮은 결합도를 유지하고 싶다면 JdbcTemplate 을 독립적인 빈으로 등록하고 JdbcTemplate 이 구현하고 있는 JdbcOperations 인터페이스를 통해 DI 받아 사용하도록 만들어도 된다.

JdbcTemplate 은 DAO 안에서 직접 만들어 사용하는 게 스프링의 관례이긴 하지만 원한다면 얼마든지 독립된 싱글톤 빈으로 등록하고 DI 받아 인터페이스를 통해 사용할 수 있다.

그런데 여기서 더 개선할 수도 있을까?
- 첫째는 userMapper가 인스턴스 변수로 설정되어 있고, 한 번 만들어지면 변경되지 않는 프로퍼티와 같은 성격을 띠고 있으니 아예 UserDao 빈의 DI용 프로퍼티를 만들어버리면 어떨까? UserMapper를 독립된 빈으로 만들고 XML 설정에 User 테이블의 필드 이름과 User 오브젝트 프로퍼티의 매핑정보를 담을 수도 있을 것이다. 이렇게 UserMapper 를 분리할 수 있다면 User 의 프로퍼티와 User 테이블의 필드 이름이 바뀌거나 매핑 방식이 바뀌는 경우에 UserDao 코드를 수정하지 않고도 매핑정보를 변경할 수 있다는 장점이 있다.
- 둘째는 DAO 에서드에서 사용하는 SQL 문장을 UserDao 코드가 아니라 외부 리소스에 담고 이를 읽어와 사용하게 하는 것이다. 이렇게 해두면 DB 테이블의 이름이나 필드 이름을 변경하고나 SQL 쿼리를 최적화해야 할 때도 UserDao 코드에는 손을 댈 필요가 없다.

## 3.7 정리

3장에서는 예외처리와 안전한 리소스 반환을 보장해주는 DAO 코드를 만들고 이를 객체지향 설계 원리와 디자인 패턴, DI 등을 적용해서 깔끔하고 유연하며 단순한 코드로 만드는 방법을 살펴봤다. 3장에서 다룬 내용은 다음과 같다.

- JDBC와 같은 예외가 발생할 가능성이 있으며 공유 리소스의 반환이 필요한 코드는 반드시 try/catch/finally 블록으로 관리해야 한다
- 일정한 작업 흐름이 반복되면서 그 중 일부만 바뀌는 코드가 존재한ㄴ다면 전략 패턴을 적용한다. 바뀌지 않는 부분은 컨텍스트, 바뀌는 부분은 전략으로 만들고 인터페이스를 통해 유연하게 전략을 변경할 수 있도록 구성한다.
- 같은 어플리케이션 안에서 여러 가지 종류의 전략을 다이내믹하게 구성하고 사용해야 한다면 컨텍스트를 이용하는 클라이언트 메서드에서 직접 전략을 정의하고 제공하게 만든다.
- 클라이언트 메서드 안에 익명 내부 클래스를 사용해 전략 오브젝트를 구현하면 코드도 간결해지고 메서드의 정보를 직접 사용할 수 있어서 편리하다.
- 컨텍스트가 하나 이상의 클라이언트 오브젝트에서 사용된다면 클래스를 분리해서 공유하도록 만든다.
- 컨텍스트는 별도의 빈으로 등록해서 DI 받거나 클라이언트 클래스에서 직접 생성해서 사용한다. 클래스 내부에서 컨텍스트를 사용할 때 컨텍스트가 의존하는 외부의 오브젝트가 있다면 코드를 이용해서 직접 DI 해줄 수 있다.
- 단일 전략 메서드를 갖는 전략 패턴이면서 익명 내부 클래스를 사용해서 매번 전략을 새로 만들어 사용하고, 컨텍스트 호출과 동시에 전략 DI를 수행하는 방식을 템플릿/콜백 패턴이라고 한다.
- 콜백의 코드에도 일정한 패턴이 반복된다면 콜백을 템플릿에 넣고 재활용하는 것이 편리하다.
- 템플릿과 콜백의 타입이 다양하게 바뀔 수 있다면 제네릭스를 이용한다.
- 스프링은 JDBC 코드 작성을 위해 JdbcTemplate 을 기반으로 하는 다양한 템플릿과 콜백을 제공한다.
- 템플릿은 한 번에 하나 이상의 콜백을 사용할 수도 있고, 하나의 콜백을 여러 번 호출할 수도 있다.
- 템플릿/콜백을 설계할 때는 템플릿과 콜백 사이에 주고받는 정보에 관심을 둬야 한다.

템플릿/콜백은 스프링이 객체지향 설계와 프로그래밍에 얼마나 가치를 두고 있는지를 잘 보여주는 예다. 스프링이 제공하는 템플릿/콜백을 잘 사용해야 하는 것은 물론이며 직접 템플릿/콜백을 만들어 활용할 수도 있어야 한다.