# 3장 템플릿

1장에서는 초난감 DAO 코드에 DI를 적용해나가는 과정을 통해서 관심이 다른 코드를 다양한 방법으로 분리하고, 확장과 변경에 용이하게 대응할 수 있는 설계구조로 개선하는 작업을 했다.

확장에는 자유롭게 열려 있고 변경에는 굳게 닫혀 있다는 객체지향 설계의 핵심 원칙인 개방 폐쇄 원칙 (OCP)을 다시 한번 생각해보자.

템플릿이란 이렇게 바뀌는 성질을 다른 코드 중에서 변경이 거의 일어나지 않으며 일정한 패턴으로 유지되는 특성을 가진 부분을 자유롭게 변경되는 성질을 가진 부분으로부터 독립시켜서 효과적으로 활용할 수 있도록 하는 방법이다.

## 3.1 다시 보는 초난감 DAO

예외 상황에 대한 처리가 안되어 있다. 처리해보자.

### 3.1.1 예외처리 기능을 갖춘 DAO

DB 커넥션이라는 제한적인 리소스를 공유해 사용하는 서버에서 동작하는 JDBC 코드에는 반드시 지켜야할 원칙이 있다. 바로 예외처리이다. 정상적인 JDBC 코드의 흐름을 따르지 않고 중간에 어떤 이유로든 예외가 발생했을 경우에도 사용한 리소스를 반드시 반환하도록 만들어야 하기 때문이다. 그렇지 않으면 시스템에 심각한 문제를 일으킬 수 있다.

JDBC 수정 기능의 예외처리 코드

리스트 3-1에 나온 UserDao의 가장 단순한 메서드임 deleteAll()을 살펴보자.

<img width="666" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/f5d707c9-b78a-43da-86ee-dc3e8d78805a">

이 메서드에서는 Connection과 PreparedStatement라는 두 개의 공유 리소스를 가져와서 사용한다. 물론 정상적으로 처리되면 메서드를 마치기 전에 각각 close()를 호출해서 리소스를 반환한다.

하지만 PreparedStatement 를 처리하는 중에 예외가 발생하면 어떻게 될까? 이때는 메서드 실행을 끝마치지 못하고 바로 메서드를 빠져나가게 된다. 이때 문제는 Connection 과 PreparedStatement 의 close() 메서드가 실행되지 않아서 제대로 리소스가 반환되지 않을 수 있다는 점이다.

일반적으로 서버에서는 제한된 개수의 DB 커넥션을 만들어서 재사용 가능한 풀로 관리한다. DB 풀은 배번 getConnection()으로 가져간 커넥션을 명시적으로 close()해서 되덜려줘야 하지만 이런 시긍로 오류가 나면 미쳐 반환하지 못한 Connection 들이 쌓이면서 어느 순간 커넥션 풀에 여유가 없어지고 리소스가 모자란다는 심각한 오류를 내며 서버가 중단될 수 있다.

위 에러는 장시간 운영되는 다중 사용자를 위한 서버에 적용하기에는 치명적인 위험을 내포하고 있다.

그래서 이런 상황에서도 가져온 리소스를 반환하도록 try/catch/finally 구문 사용을 권장하고 있다. 예외상황에서도 리소스를 제대로 반환할 수 있도록 try/catch/finally를 적용해보자.

```java
public void deleteAll() throws SQLException {
    Connection c = null;
    PreparedStatement ps = null;

    try {
        c = dataSource.getConnection();
        ps = c.prepareStatement("delete from users");
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

이제 예외상황에서도 안전한 코드가 됐다. finally는 try 블록을 수행한 후에 예외가 발생하든 정상적으로 처리되든 상관없이 반드시 실행되는 코드를 넣을 때 사용한다. 일반 try 블록으로 갔으면 반드시 Connection이나 PreparedStatement의 close()를 호출을 통한 가져온 리소스를 반환해야 한다.

그런제 문제는 예외가 어느 시점에 나는가에 따라서 Connection과 PrepareStatement의 close() 메서드를 호출해야 할지가 달라진다는 점이다.

만약에 getConnection() 에서 DB 커넥션을 가져오다가 일시적인 DB 서버 문제나, 네트워크 문제 또는 그 밖의 예외상황 때문에 예외가 발생했다면 ps는 물론이고 변수 c도 아직 null 상태다. null 상태의 변수에 close() 메서드를 호출하면 NullPointException 에러가 발생할 테니 이럴 땐 close()를 호출하면 안된다.

만약 ps를 실행하다가 예외가 발생한 경우라면, ps와 c 모두 close() 메서드를 호출해줘야 한다. 어느 시점에 예외가 발생했는지에 따라 close()를 사용할 수 있는 변수가 달라질 수 있기 떄문에 finally에서 반드시 c와 ps가 null이 아닌지 확인한 후에 close()를 호출해줘야 한다.

문제는 close() 메서드도 SQLException 이 발생할 수 있는 메서드라는 점이다. 따라서 try/catch 문으로 처리해줘야 한다.

### JDBC 조회 기능의 예외처리

```java
    public int getCount() throws SQLException {
    Connection c = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
        c = dataSource.getConnection();
        ps = c.prepareStatement(
                "select count(*) from users"
        );

        rs = ps.executeQuery();

        int count = 0;
        if (rs.next()) {
            count = rs.getInt(1);
        }
        return count;
    } catch (SQLException e ) {
        throw e;
    } finally {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {

            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {

            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {

            }
        }
    }
}
```

위 코드에서는 ResultSet도 close()를 해주어야 한다.