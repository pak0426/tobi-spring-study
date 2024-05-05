## 3.4 컨텍스트와 DI

### 3.4.1 JdbcContext의 분리

전략 패턴의 구조로 보자면 UserDao의 메서드가 클라이언트이고 <br>
익명 내부 클래스로 만들어지는 것이 개별적인 전략이고
jdbcContextWithStatementStrategy() 메서드는 컨텍스트이다.

컨텍스트 메서드는 UserDao 내의 PreparedStatement를 실행하는 기능을 가진 메서드에서 공유할 수 있다.

그런데 JDBC의 일반적인 작업 흐름을 담고 있는 jdbcContextWithStatementStrategy()는 다른 DAO에서도 사용 가능하다. 그러니 jdbcContextwithStatementStrategy()를 UserDao 클래스 밖으로 독립시켜서 모든 DAO가 사용할 수 있게 해보자.

#### 클래스 분리

분리해서 만들 클래스의 이름은 JdbcContext 라고 하자. JdbcContext에 UserDao에 있던 컨텍스트 메서드를 workWithStatementStrategy() 라는 이름으로 옮겨놓는다. 그런데 이렇게 하면 DataSource가 필요한 것은 UserDao가 아니라 JdbcContext가 돼버린다. DB커넥션을 필요로 하는 코드는 JdbcContext 안에 있기 떄문이다. 따라서 JdbcContext가 DataSource에 의존하고 있으므로 DataSource 타입 빈을 DI 받을 수 있게 해줘야 한다.

```java
public class JdbcContext {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void workWithStatementStrategy(StatementStrategy strategy) throws Exception {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();
            ps = strategy.makePrepareStatement(c);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) { try { ps.close(); } catch (SQLException e) {} }
            if (c != null) { try { c.close(); } catch (SQLException e) { } }
        }
    }
}
```

다음은 위의 UserDao가 분리된 JdbcContext를 DI 받아서 사용할 수 있게 만든다.

<img width="632" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/17e0b22c-3fdd-46d6-bd4d-20b6d005f016">

### 빈 의존관계 변경

새롭게 작성된 오브젝트 간의 의존관계를 살펴보고 이를 스프링 설정에 적용해보자.

UserDao는 이제 JdbcContext에 의존하고 있다. 그런데 JdbcContext는 인터페이스인 DataSource와는 달리 구체 클래스다. 스프링의 DI는 기본적으로 인터페이스를 사이에 두고 그 자체로 독립적인 JDBC 컨텍스트를 제공해주는 서비스 오브젝트로서 의미가 있을 뿐이고 구현 방법이 바뀔 가능성은 없다. 따라서 인터페이스를 구현하도록 만들지 않았고 UserDao와 JdbcContext는 인터페이스를 사이에 두지 않고 DI를 적용하는 특별한 구조가 된다. 아래 그림은 추가된 의존관계를 나타내주는 클래스 다이어그램이다.

<img width="607" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/2a28ae16-0bb0-49f0-a3a8-b0b70301ba3e">

**스프링의 빈 설정은 클래스 레벨이 아니라 런타임 시에 만들어지는 오브젝트 레벨의 의존관계에 따라 정의된다.**

빈으로 정의되는 오브젝트 사이의 관계를 그려보면 아래와 같다. 기존에는 userDao 빈이 dataSource 빈을 직접 의존했지만 이제는 jdbcContext 빈이 그 사이에 끼게 된다.

<img width="599" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/fde9518d-6537-4300-91c1-0fef4758ee36">

위 그림의 의존관계에 따라서 DaoFactory를 수정해보자.

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
    public JdbcContext jdbcContext() {
        JdbcContext jdbcContext = new JdbcContext();
        jdbcContext.setDataSource(dataSource());
        return jdbcContext;
    }

    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setDataSource(dataSource());
        userDao.setJdbcContext(jdbcContext());
        return userDao;
    }
}
```

아직은 userDao의 모든 메서드가 JdbcContext를 사용하는 것은 아니니, 기존 방법을 사용해서 동작하는 메서드들을 위해 UserDao가 아직은 dataSource를 DI 받도록 하고 있음에 주의하자.

이제 JdbcContext를 UserDao로부터 완전히 분리하고 DI를 통해 연결될 수 있도록 설정을 마쳤다. UserDaoTest 테스트를 실행해서 이 JdbcContext를 분리해서 사용하도록 하는 코드 수정 작업에 이상이 없는지 확인해보자.

### 3.4.2 JdbcContext의 특별한 DI

JdbcContext를 분리하면서 사용했던 DI 방법에 대해 좀 더 생각해보자. UserDao와 JdbcContext 사이에는 인터페이스를 사용하지 않고 DI를 적용했다. 지금까지 적용했던 DI에서는 클래스 레벨에서 구체적인 의존관계가 만들어지지 않도록 인터페이스를 사용했다. 인터페이스를 적용했기 때문에 코드에서 직접 클래스를 사용하지 않아도 됐고, 그 덕분에 설정을 변경하는 것만으로도 얼마든지 다양한 의존 오브젝트를 변경해서 사용할 수 있게 됐다.

그런데 UserDao는 인터페이스를 거치지 않고 코드에서 바로 JdbcContext 클래스를 사용하고 있다. UserDao와 JdbcContext는 클래스 레벨에서 의존관계가 결정된다. 비록 런타임 시에 DI 방식으로 외부에서 오브젝트를 주입해주는 방식을 사용하긴 했지만, 의존 오브젝트의 구현 클래스를 변경할 수는 없다.

#### 스프링 빈으로 DI

이렇게 인터페이스를 사용하지 않고 DI를 적용하는 것은 문제가 있지 않을까? 스프링 DI의 기본 의도에 맞게 JdbcContext의 메서드를 인터페이스를 뽑아내어 정의해두고, 이를 UserDao에서 사용하게 해야 하지 않을까? 물론 그렇게 해도 상관은 없다. 하지만 꼭 그럴 필요는 없다.

의존관계 주입이라는 개념을 충실히 따르자면, 인터페이스를 사이에 둬서 클래스 레벨에서는 의존관계가 고정되지 않게 하고, 런타임 시에 의존할 오브젝트와의 관계를 다이내믹하게 주입해주는 것이 맞다. 따라서 인터페이스를 사용하지 않았다면 온전한 DI라고 볼 수 없다. 그러나 스프링의 DI는 넓게 보자면 객체의 생성과 관계 설정에 대한 제어권한을 오브젝트에서 제거하고 외부로 위임했다는 IoC라는 개념을 포괄한다. 그런 의미에서 JdbcContext를 스프링을 이용해서 UserDao 객체에서 사용하게 주입했다는 건 DI의 기본을 따르고 있다고 볼 수 있다.

인터페이스를 사용해서 클래스를 자유롭게 변경할 수 있게 하지는 않았지만, JdbcContext를 UserDao와 DI 구조로 만들어야 할 이유를 생각해보자.

1. JdbcContext가 스프링 컨테이너의 싱글톤 레지스트리에서 관리되는 싱글톤 빈이 되기 때문이다. <br>
    JdbcContext는 그 자체로 변경되는 상태정보를 가지고 있지 않다. 내부에서 사용할 dataSource라는 인스턴스 변수는 있지만, dataSource는 읽기전용이므로 JdbcContext가 실글톤이 되는데 문제가 없다. JdbcContext는 JDBC 컨텍스트 메서드를 제공해주는 일종의 서비스 오브젝트로서 의미가 있고, 그래서 싱글톤으로 등록돼서 여러 오브젝트에서 공유해 사용되는 것이 이상적이다.
2. JdbcContext가 DI를 다른 빈에 의존하고 있기 때문이다 이 두 번째 이유가 중요하다. <br>
   JdbcContext는 dataSource 프로퍼티를 통해 DataSource 오브젝트를 주입받도록 되어 있다. DI를 위해서는 주입되는 오브젝트와 주입받는 오브젝트 양쪽 모두 스프링 빈으로 등록돼야 한다. 스프링이 생성하고 관리하는 IoC 대상이어야 DI에 참여할 수 있기 때문이다. JdbcContext는 다른 빈을 DI 받기 위해서라도 스프링 빈으로 등록돼야 한다.

실제로 스프링에는 드물지만 이렇게 인터페이스를 사용하지 않는 클래스를 직접 의존하는 DI가 등장하는 경우도 있다.여기서 중요한 것은 인터페이스 사용 여부이다.

인터페이스가 없다는 건 UserDao와 JdbcContext가 매우 긴밀한 관계를 가지고 강하게 결합되어 있다는 의미다.
UserDao는 항상 JdbcContext 클래스와 함께 사용돼야 한다. 비록 클래스는 구분되어 있지만 이 둘은 강한 응집도를 가지고 있다. UserDao가 JDBC 방식에서 JPA 같은 ORM 방식으로 바뀌게 된다면 JdbcContext도 통째로 바뀌어야 한다. JdbcContext는 DataSource와 달리 테스트에서도 다른 구현으로 대체해서 사용할 이유가 없다. 이런 경우는 굳이 인터페이스를 두지 말고 강력한 결합을 가진 관계를 허용하면서 위에서 말한 두 가지 이유인, 싱글톤으로 만드는 것과 JdbcContext 에 대한 DI 필요성을 위해 스프링의 빈으로 등록해서 DI 되도록 만들어도 좋다.

단, 이런 클래스를 바로 사용하는 코드 구성을 DI에 적용하는 것은 가장 마지막 단계에서 고려해볼 사항임을 잊지말자. 굳이 원한다면 JdbcContext에 인터페이스를 두고 UserDao에서 인터페이스를 사용하도록 만들어도 문제 되진 않는다.

```java
public interface JdbcContextInterface {
    public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException;
}


public class JdbcContextImpl implements JdbcContextInterface {
    private DataSource dataSource;

    public JdbcContextImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();
            ps = strategy.makePrepareStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) { try { ps.close(); } catch (SQLException e) {} }
            if (c != null) { try { c.close(); } catch (SQLException e) { } }
        }
    }
}

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
    public JdbcContext jdbcContext() {
        JdbcContext jdbcContext = new JdbcContext();
        jdbcContext.setDataSource(dataSource());
        return jdbcContext;
    }

    @Bean
    public JdbcContextInterface jdbcContextInterface() {
        JdbcContextInterface jdbcContextInterface = new JdbcContextImpl(dataSource());
        return jdbcContextInterface;
    }

    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setDataSource(dataSource());
        userDao.setJdbcContext(jdbcContext());
        userDao.setJdbcContextInterface(jdbcContextInterface());
        return userDao;
    }
}

class UserDao {
    private User user;

    private DataSource dataSource;
    private JdbcContext jdbcContext;
    private JdbcContextInterface jdbcContextInterface;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setJdbcContext(JdbcContext jdbcContext) {
        this.jdbcContext = jdbcContext;
    }

    public void setJdbcContextInterface(JdbcContextInterface jdbcContextInterface) {
        this.jdbcContextInterface = jdbcContextInterface;
    }

    public void add(final User user) throws SQLException {
        this.jdbcContextInterface.workWithStatementStrategy(
                new StatementStrategy() {

                    @Override
                    public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
                        // ...
                    }
                }
        );
    }

    public void deleteAll() throws SQLException {
        this.jdbcContextInterface.workWithStatementStrategy(
                new StatementStrategy() {
                    // ...
                }
        );
    }
}
```

인터페이스 방식은 위와 같이 생성자 주입을 이용해서 DI를 해주면 된다.

#### 코드를 이용하는 수동 DI