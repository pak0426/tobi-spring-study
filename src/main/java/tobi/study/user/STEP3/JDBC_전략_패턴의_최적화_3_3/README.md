## 3.3 JDBC 전략 패턴의 최적화

지금까지 기존의 deleteAll() 메서드에 담겨 있던 변하지 않는 부분, 자주 변하는 부분을 전략 패턴을 사용해 깔끔하게 분리해냈다. 독립된 JDBC 작업 흐름이 담긴 jdbcContextWithStatementStrategy()는 DAO 메서드들이 공유할 수 있게 됐다. DAO 메서드는 전략 패턴의 클라이언트로서 컨텍스트에 해당하는 jdbcContextWithStatementStrategy() 메서드에 적절한 전략, 즉 바뀌는 로직을 제공해주는 방법으로 사용할 수 있다. 여기서 컨텍스트는 PreparedStatement 를 실행하는 JDBC의 작업 흐름이고, 전략은 PreparedStatement 를 생성하는 것이다.

### 3.3.1 전략 클래스의 추가 정보

이번엔 add() 메서드에도 적용해보자.

```java
public class AddStatement implements StatementStrategy {
    private User user;

    public AddStatement(User user) {
        this.user = user;
    }

    @Override
    public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) values (?, ?, ?)"
        );
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());
        return ps;
    }
}
```

deleteAll()과는 달리 add()에서는 PreparedStatement 를 만들 때 user라는 부가적인 정보가 필요하기 때문이다. 등록할 사용자 정보는 클라이언트에 해당하는 add() 메서드가 갖고 있다. 따라서 클라이언트가 AddStatement의 전략을 수행하려면 부가정보인 user를 제공해줘야 한다.

클라이언트로부터 User 타입 오브젝트를 받을 수 있도록 AddStatement의 생성자를 통해 제공받게 했다.

```java
public void add(User user) throws SQLException, ClassNotFoundException {
    StatementStrategy strategy = new AddStatement(user);
    jdbcContextWithStatementStrategy(strategy);
}
```

테스트를 돌려보면 정상적으로 성공한다. 미리 준비해둔 테스트가 있기 때문에 DAO 코드를 자유롭게 개선할 수 있다는 사실을 잊지 말자.

### 3.3.2 전략과 클라이언트의 동거

좀 더 개선할 부분을 찾아보자.

1. DAO 메서드마다 새로운 StatementStrategy 구현 클래스를 만들어야 한다는 점이다. 이렇게 되면 기존 UserDao 때보다 클래스 파일의 개수가 많이 늘어난다. 이래서는 런타임 시에 다이내믹하게 DI 해준다는 점을 제외하면 로직마다 상속을 사용하는 템플릿 메서드 패턴을 적용했을 때보다 그다지 나을 게 없다. 
2. DAO 메서드에서 StatementStrategy 에 전달할 User 와 같은 부가적인 정보가 있는 경우, 이를 위해 오브젝트를 전달받는 생성자와 이를 저장해둘 인스턴스 변수를 번거롭게 만들어야 한다는 점이다. 이 오브젝트가 사용되는 시점은 컨텍스트가 전략 오브젝트를 호출할 때이므로 잠시라도 어딘가에 다시 저장해둘 수 밖에 없다.

이 두 가지 문제를 해결할 방법을 생각해보다.

#### 로컬 클래스

클래스 파일이 많아지는 문제는 간단한 해결 방법이 있다. StatementStrategy 전략 클래스를 매번 독립된 파일로 만들지 말고 UserDao 클래스 안에 내부 클래스로 정의해버리는 것이다. DeleteAllStatement 와 AddStatement 는 UserDao 밖에서는 사용되지 않는다. 둘 다 UserDao 에서만 사용되고, UserDao의 메서드 로직에 강하게 결합되어 있다.

```java
public void add(User user) throws SQLException, ClassNotFoundException {
    class AddStatement implements StatementStrategy {
        private User user;

        public AddStatement(User user) {
            this.user = user;
        }

        @Override
        public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
            PreparedStatement ps = c.prepareStatement(
                    "insert into users(id, name, password) values (?, ?, ?)"
            );
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPassword());
            return ps;
        }
    }
    
    StatementStrategy strategy = new AddStatement(user);
    jdbcContextWithStatementStrategy(strategy);
}
```

AddStatement 클래스를 로컬 클래스로서 add() 메서드 안에 집어넣은 것이다. 이런 식으로 클래스를 정의하는 방식이 생소할지 모르겠지만 자바 언어에서 허용하는 클래스 선언 방법의 하나다. 마치 로컬 변수를 선언하듯이 선언하면 된다. 로컬 클래스는 선언된 메서드 내에서만 사용할 수 있다. AddStatement 가 사용될 곳이 add() 메서드 뿐이라면, 이렇게 바로 정의해서 쓰는 것도 나쁘지 않다. **덕분에 클래스 파일 하나 줄었고, add() 메서드 안에서 PreparedStatement 생성 로직을 함께 볼 수 있으니 코드를 이해하기도 좋다.**

로컬 클래스의 또 다른 장점은 **로컬 클래스는 클래스가 내부 클래스이기 때문에 자신이 선언된 곳의 정보에 접근할 수 있다는 점이다.** AddStatement는 User 정보를 필요로 한다. 이를 생성자를 만들어 add() 메서드에 전달하도록 했다. 그런데 이렇게 add() 메서드 내에 AddStatement 클래스를 정의하면 생성자를 통해 User 오브젝트를 전달할 필요가 없다.

내부 메서드는 자신이 정의한 메서드의 로컬 변수에 직접 접근할 수 있기 때문이다. 다만 내부 클래스에서 외부의 변수를 사용할 때는 외부 변수는 반드시 final로 선언해줘야 한다. user 파라미터는 메서드 내부에서 변경될 일이 없으므로 final로 선언해도 무방하다.

```java
public void add(final User user) throws SQLException, ClassNotFoundException {
    class AddStatement implements StatementStrategy {

        @Override
        public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
            PreparedStatement ps = c.prepareStatement(
                    "insert into users(id, name, password) values (?, ?, ?)"
            );
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPassword());
            return ps;
        }
    }

    StatementStrategy strategy = new AddStatement();
    jdbcContextWithStatementStrategy(strategy);
}
```

로컬 클래스의 장점
- 클래스 파일 하나를 줄일 수 있다.
- 내부 클래스 특징을 이용해 로컬 변수를 바로 가져다가 사용할 수 있다.

#### 익명 내부 클래스

AddStatement 클래스는 add() 메서드에서만 사용할 용도로 만들어졌다. 그렇다면 좀 더 간결하게 클래스 이름도 제거할 수 있다. 자바에서는 이름 조차 필요 없는 익명 내부 클래스가 있다.

익명 내부 클래스는 선언과 동시에 오브젝트를 생성한다. 이름이 없기 때문에 클래스 자신의 타입을 가질 수 없고, 구현한 인터페이스 타입의 변수에만 저장할 수 있다.

```java
public void add(final User user) throws SQLException, ClassNotFoundException {
    StatementStrategy strategy = new StatementStrategy() {

        @Override
        public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
            PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPassword());
            return ps;
        }
    };
    jdbcContextWithStatementStrategy(strategy);
}
```

만들어진 익명 내부 클래스의 오브젝트는 딱 한 번만 사용할 테니 굳이 변수는 담아두지 말고 jdbcContextWithStatementStrategy() 메서드의 파라미터에서 바로 생성하는 편이 낫다.

```java
public void add(final User user) throws SQLException, ClassNotFoundException {
    jdbcContextWithStatementStrategy(
            new StatementStrategy() {

                @Override
                public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
                    PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
                    ps.setString(1, user.getId());
                    ps.setString(2, user.getName());
                    ps.setString(3, user.getPassword());
                    return ps;
                }
            }
    );
}
```

마찬가지로 DeleteAllStatement도 deleteAll() 메서드로 가져와서 익명 내부 클래스로 처리해보자.

```java
public void deleteAll() throws SQLException {
    jdbcContextWithStatementStrategy(
            new StatementStrategy() {
                @Override
                public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
                    PreparedStatement ps = c.prepareStatement("delete from users");
                    return ps;
                }
            }
    );
}
```