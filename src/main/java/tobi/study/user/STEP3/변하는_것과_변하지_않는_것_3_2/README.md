## 3.2 변하는 것과 변하지 않는 것

### 3.2.1 JDBC try/catch/finally 코드의 문제점

이제 try/catch/finally 블록도 적용돼서 완성도가 높은 DAO 코드가 됐지만 코드를 보면 복잡하다.

이런 코드를 작성할 때 사용할 수 있는 방법이 코드 복사해서 붙이기이다.

하지만 어느 순간 한 줄을 뺴먹고 복사했거나, 잘못 삭제 했다면 어떻게 될까? 당장 문제는 없더라도 해당 메서드가 하나씩 호출되고 커넥션이 하나씩 반환되지 않고 쌓여가게 된다.

서버에 배치해서 사용하면 언젠가 DB 풀에 설정해놓은 최대 DB 커넥션 개수를 넘어설 것이고, 서버에서 리소스가 꽉 찼다는 에러가 나면서 서비스가 중단하는 상황이 발생한다.

그렇다면 테스트를 통해 DAO마다 예외상황에서 리소스를 반납하는지 체크하게 했으면 어떨까? 이런 코드는 좋은 생각이긴 한데 막상 적용하기는 쉽지 않을 것이다. 예외상황을 처리하는 코드는 테스트하기가 매우 어렵고 모든 DAO 메서드에 대해 이런 테스트를 일일이 한다는 건 매우 번거롭기 때문이다.

이런 코드를 효과적으로 다룰 수 있는 방법은 없을까? 이 문제의 핵심은 변하지 않는, 그러나 많은 곳에서 중복되는 코드와 로직에 따라 자꾸 확장되고 자주 변하는 코드를 잘 분리해내는 작업이다.

## 3.2.2 분리와 재사용을 위한 디자인 패턴 적용

UserDao의 메서드를 개선하는 작업을 해보자.

<img width="683" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/a3ddf429-0b03-45b8-81e4-6a6505d4ba66">

비슷한 기능의 메서드에서 동일하게 나타날 수 있는 변하지 않고 고정되는 부분과, 각 메서드마다 로직에 따라 변하는 부분을 위와 같이 구분해 볼 수 있다.

<img width="637" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/80bc08de-3ade-44e1-af96-b3cb149b797d">

변하는 부분을 위와 같이 바꾸기만하면 나머지 코드는 전혀 수정하지 않아도 된다.

### 메서드 추출

먼저 생각해볼 수 있는 방법은 변하는 부분을 메서드로 빼는 것이다. 변하지 않는 부분이 변하는 부분을 감싸고 있어서 변하지 않는 부분을 추출하기가 어려워 보이기 때문에 반대로 해봤다.

<img width="611" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/1d0c591f-f15e-4156-b3aa-374f2223d0b3">

자주 바뀌는 부분을 메서드로 독립시켰는데 당장 봐서는 별 이득이 없어 보인다. 왜냐하면 보통 메서드 추출 리팩토링을 적용하는 경우에는 분리시킨 메서드를 다른 곳에서 재사용할 수 있어야 하는데, 이건 반대로 분리시키고 남은 메서드가 재사용이 필요한 부분이고, 분리된 메서드는 DAO 로직마다 새롭게 만들어서 확장돼야 하는 부분이기 때문이다.

### 템플릿 메서드 패턴의 적용

다음은 템플릿 메서드 패턴을 이용해서 분리해보자. 템플릿 메서드 패턴은 상속을 통해 기능을 확장해서 사용하는 부분이다. 변하지 않는 부분은 슈퍼클래스에 두고 변하는 부분은 추상 메서드로 정의해둬서 서브클래스에서 오버라이드하여 새롭게 정의하도록 쓰는 것이다.

```java
abstract protected PreparedStatement makeStatement(Connection c) throws SQLException;
```

그리고 이를 상속하는 서브클래스를 만들어서 거기서 이 메서드를 구현한다. 고정된 JDBC try/catch/finally 블록을 가진 슈퍼클래스 메서드와 필요에 따라서 상속을 통해 구체적인 PreparedStatement 를 바꿔서 사용할 수 있게 만드는 서브클래스로 깔끔하게 분리할 수 있다.

```java
public class UserDaoDeleteAll extends UserDao {
    @Override
    protected PreparedStatement makeStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("DELETE FROM user");
        return ps;
    }
}
```

이제 UserDao 클래스의 기능을 확장하고 싶을 때마다 상속을 통해 자유롭게 확장할 수 있고, 확장 때문에 기존의 상위 DAO 클래스에 불필요한 변화가 생기지 않도록 할 수 있으니 객체지향 설계 원리인 개방 폐쇄 원칙을 그럭저럭 지키는 구조를 만들어 낼 수 있다. 하지만 템플릿 메서드 패턴으로의 접근은 제한이 많다.

**가장 큰 문제는 DAO 로직마다 상속을 통해 새로운 클래스를 만들어야 한다는 점이다.** 만약 이 방식을 사용한다면 UserDao JDBC 메서드가 4개일 경우 4개의 서브클래스를 만들어서 사용해야 한다. 이래서는 장점보다 단점이 더 많아보인다.

<img width="1079" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/aa928420-7161-4d8e-94ac-a7b2c87adbe1">

또 확장구조가 이미 클래스를 설계하는 시점에서 고정되어 버린다는 점이다. 변하지 않는 코드를 가진 UserDao의 JDBC try/catch/finally 블록과 변하는 PreparedStatement를 담고 있는 서브클래스들이 이미 클래스 레벨에서 컴파일 시점에 이미 그 관계가 결정되어 있다. 따라서 그 관계애 대한 유연성이 떨어져 버린다. 상속을 통해 확장을 꾀하는 템플릿 메서드의 단점이 보인다.

#### 장점
- 상속을 통해 변하지 않는 것은 상위 클래스에 변하는 것은 서브클래스에서 상속받아 구체화하여 자유롭고 상위 클래스에 불필요한 변화가 생기지 않는다.
- 객체지향 설계 원칙인 개방 폐쇄 원칙을 지키며 확장엔 개방되고 수정엔 닫혀 있다.

#### 단점
- DAO 로직마다 상속을 통해 새로운 클래스를 만들어야 한다. 메서드가 +N개가 추가된다면 그를 구현하는 서브 클래스가 +N개 생성되어야 한다.
- 확장구조가 클래스를 설계하는 시점에 고정되어 버린다.
- 클래스 레벨과 컴파일 시점에 이미 그 관계가 결정되어 있다. 따라서 유연성이 떨어진다.

### 전략 패턴의 적용

개방 폐쇄 원칙을 잘 지키는 구조이면서도 템플릿 메서드 패턴보다 유연하고 확장성이 뛰어난 것이, 오브젝트를 아예 둘로 분리하고 클래스 레벨에서는 인터페이스를 통해서만 의존하도록 만드는 전략 패턴이다. 전략 패턴은 OCP 관점에 보면 확장에 해당하는 변하는 부분을 별도의 클래스로 만들어 추상화된 인터페이스를 통해 위임하는 방식이다.

<img width="645" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/a752d049-1d89-4c42-8d56-98d0de8b9a1c">

좌측에 있는 Context의 contextMethod()에서 일정한 구조를 가지고 동작하다가 특정 확장 기능은 Strategy 인터페이스를 통해 외부의 독립된 전략 클래스에 위임하는 것이다.

deleteAll() 메서드에서 변하지 않는 부분이라고 명시한 것이 바로 이 contextMethod()가 된다. deleteAll() JDBC를 이용해 DB를 업데이트하는 작업이라는 변하지 않는 맥락을 갖는다. 

deleteAll()의 컨텍스트를 정리해보면 다음과 같다.
- DB 커넥션 가져오기
- PreparedStatement 를 만들어줄 외부 기능 호출하기
- 전달받은 PreparedStatement 실행하기
- 예외가 발생하면 이를 메서드 밖으로 던지기
- 모든 경우에 만들어진 PreparedStatement와 Connection을 적절히 닫아주기

두 번째 작업에서 사용하는 PreparedStatement를 만들어주는 외부 기능이 바로 전략 패턴에서 말하는 전략이라고 볼 수 있다. 전략 패턴의 구조를 따라 이 기능을 인터페이스로 만들어두고 인터페이스의 메서드를 통해 PreparedStatement 생성 전략을 호출해주면 된다. 

여기서 눈여겨볼 것은 이 PreparedStatement를 생성하는 전략을 호출할 때는 이 컨텍스트 내에서 만들어둔 DB 커넥션을 전달해야 한다는 점이다.


```java
public interface StatementStrategy {
    PreparedStatement makePrepareStatement(Connection c) throws SQLException;
}
```

이 인터페이스를 상속해서 실제 전략, 즉 바뀌는 부분인 PreparedStatement를 생성하는 클래스를 만들어보자.

```java
public class DeleteAllStatement implements StatementStrategy {
    @Override
    public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```
이제 확장된 PreparedStrategy 전략인 DeleteAllStatement 가 만들어졌다. 이것을 아래와 같이 contextMethod()에 해당하는 UserDao의 deleteAll() 메서드에서 사용하면 그럭저럭 전략 패턴을 적용했다고 볼 수 있다.

```java
public void deleteAll() throws SQLException {
    Connection c = null;
    PreparedStatement ps = null;

    try {
        c = dataSource.getConnection();
        StatementStrategy strategy = new DeleteAllStatement();
        ps = strategy.makePrepareStatement(c);
        ps.executeUpdate();
    } catch (SQLException e) {
        throw e;
    } finally {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {

            }
        }
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {

            }
        }
    }
}
```

하지만 전략 패턴은 필요에 따라 컨텍스트는 그대로 유지되면서 전략을 바꿔 쓸 수 있다는 것인데, 이렇게 컨텍스트안에서 이미 구체적인 전략 클래스인 DeleteAllStatement 를 사용하도록 고정되어 있다면 뭔가 이상하다'
```java
StatementStrategy strategy = new DeleteAllStatement();
```

하지만 전략 패턴은 필요에 따라 컨텍스트는 그대로 유지되면서 전략을 바꿔 쓸 수 있다는 것인데 이렇게 고정되어 있다면 이상하다. 컨텍스트가 DeleteAllStatement 를 사용하도록 고정되어 있으면 뭔가 이상하다. 컨텍스트가 StatementStrategy 인터페이스뿐 아니라 특정 구현 클래스인 DeleteAllStatement를 직접 알고 있다는 건, 전략 패턴에도 OCP에도 잘 맞는다고 볼 수 없기 때문이다.

### DI 적용을 위한 클라이언트 / 컨텍스트 분리

전략 패턴에 따르면 Context가 어떤 전략을 사용하게 할 것인가는 Context를 사용하는 앞단의 Client가 결정하는게 일반적이다. Client가 구체적인 전략의 하나의 선택하고 오브젝트로 만들어서 Context에 전달하는 것이다. Context는 전달받은 그 Strategy 구현 클래스의 오브젝트를 사용한다.

<img width="933" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/c62add7b-bef4-4bd3-828a-3ed61f01bc94">

이 패턴 구조를 코드에 적용해보자.

중요한 것은 이 컨텍스트에 해당하는 JDBC try/catch/finally 코드를 클라이언트 코드인 StatementStrategy 를 만드는 부분에서 독립시켜야 한다는 점이다. 현재 deleteAll() 메서드에서 다음 코드는 클라이언트에 들어가야할 코드이다. deleteAll()의 나머지 코드는 컨텍스트 코드이므로 분리해야 한다.

```java
StatementStrategy strategy = new DeleteAllStatement();
```

```java
public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
    Connection c = null;
    PreparedStatement ps = null;

    try {
        c = dataSource.getConnection();
        ps = stmt.makePrepareStatement(c);
        ps.executeUpdate();
    } catch (SQLException e) {
        throw e;
    } finally {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {

            }
        }
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {

            }
        }
    }

}
```

이 메서드는 컨텍스트의 핵심적인 내용을 잘 담고 있다. 클라이언트로부터 StatementStrategy 타입의 전략 오브젝트를 제공받고 JDBC try/catch/finally 구조로 만들어진 컨텍스트 내에서 작업을 수행한다. 제공받은 전략 오브젝트는 PreparedStatement 생성이 필요한 시점에 호출해서 사용한다. 모든 JDBC 코드의 틀에 박힌 작업은 이 컨텍스트 메서드 안에 잘 담겨 있다.

다음은 클라이언트에 해당하는 부분을 살펴보자. 컨텍스트를 별도의 메서드로 분리했으니 deleteAll() 메서드가 클라이언트가 된다. deleteAll()은 전략 오브젝트를 만들고 컨텍스트를 호출하는 책임을 지고 있다. 사용할 전략 클래스는 DeleteAllStatement 이므로 이 클래스의 오브젝트를 생성하고, 컨텍스르로 분리한 jdbcContextWithStatementStrategy() 메서드를 호출해주면 된다.

```java
public void deleteAll() throws SQLException {
    StatementStrategy st = new DeleteAllStatement();
    jdbcContextWithStatementStrategy(st);
}
```

이제 구조로 볼 때 완벽한 전략 패턴의 모습을 갖췄다. 비록 클라이언트와 컨텍스트는 클래스를 분리하진 않았지만, 의존관계와 책임으로 볼 때 이상적인 클라이언트/컨텍스트 관계를 갖고 있다. 특히 클라이언트가 컨텍스트가 사용할 전략을 정해서 전달하는 면에서 DI 구조라고 이해할 수도 있다.