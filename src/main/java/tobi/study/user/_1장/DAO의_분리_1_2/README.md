# DAO의 분리

## 1.2.1 관심사의 분리

객체지향의 서계도 모든 것이 변한다. 여기서 변한다는 것은 **변수**나 오브젝트의 필드의 값이 변한다는게 아니다. 오브젝트에 대한 설계와 이를 구현한 코드가 변한다.

소프트웨어 개발에서 끝이란 없다.

그래서 개발자가 객체를 설계할 때 미래의 변화를 대비하는 것이 중요하다.

객체지향 기술의 장점인 변경, 발전, 확장시킬 수 있다는데 더 의미가 있다.

변경이 일어날 때 필요한 작업을 최소화하고, 그 변경이 다른 곳에 문제를 일으키지 않게 하는 것이 **분리**와 **확장**을 고려한 설계가 있었기 때문이다.

모든 변경과 발전은 한 번에 한 가지 관심사항에 집중해서 일어난다. 문제는, 변화는 대체로 한 가지 관심에 대해 일어나지만 그에 따른 작업은 한 곳에 집중되지 않는 경우가 많다는 점이다.

변화가 한 번에 한 가지 관심에 집중돼서 일어난다면, 한 가지 관심이 한 군데에 집중되게 하는 것이다. (**관심사의 분리**)

## 1.2.2 커넥션 만들기의 추출

### UserDAO의 관심사항

1. DB와 연결을 위한 커넥션을 어떻게 가져올까
2. 사용자 등록을 위해 DB에 보낼 SQL 문장을 담을 Statement를 만들고 실행하는 것이다.
3. 작업이 끝나면 사용한 리소스를 시스템에 돌려준다.

가장 큰 문제는 첫째 관심사의 DB연결을 위한 Connection 오브젝트를 가져오는 부분이다.

```java
Class.forName("org.h2.Driver");
Connection c = DriverManager.getConnection(
"jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
);
```
더 큰 문제는 add() 메서드에 있는 DB 커넥션을 가져오는 코드와 동일한 코드가 get() 메서드에도 중복되어 있다는 점이다.

이렇게 하나의 관심사가 방만하게 중복되어 있고 여기저기 흩어져 있어서 다른 관심의 대상과 얽혀 있으면, 변경이 일어날때 엄청난 문제가 된다.

```java
private static Connection getConnection() throws ClassNotFoundException, SQLException {
    Class.forName("org.h2.Driver");
    Connection c = DriverManager.getConnection(
            "jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
    );
    return c;
}
```

위와 같이 메서드로 중복된 코드를 뽑아내는 것을 리팩토링에서는 메서드 추출이라고 한다.

### 상속을 통한 확장

만약 UserDao의 Connection 정보가 달라져야 한다면 어떻게 해야할까? 상속을 통해 해결할 수 있다.
기존에는 같은 클래스에서 다른 메소드로 분리됐던 DB 커넥션 연결이라는 관심을 이번에는 상속을 통해 서브클래스로 분리해버리는 것이다.

![image](https://github.com/pak0426/pak0426/assets/59166263/b8c38260-731f-4630-923c-2f6e88b1a45c)

```java
@Override
public Connection getConnection() throws ClassNotFoundException, SQLException {
    // N사 DB Connection 생성 코드
    Class.forName("org.h2.Driver");
    Connection c = DriverManager.getConnection(
            "jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
    );
    return c;
}
```

위의 코드를 보면 DAO의 핵심 기능인 어떻게 데이터를 등록하고 가져올 것인가(SQL 작성, 파라미터 바인딩, 쿼리 실행, 검색정보 전달)라는 관심을 담당하는 UserDao와, DB 연결 방법은 어떻게 할 것인가라는 관심을 담고 있는 NUserDao, DUserDao가 클래스 레벨로 구분이 되고 있다.

이렇게 슈퍼클래스에서 기본적인 로직의 흐름(커넥션 가져오기, SQL 생성, 실행, 반환)을 만들고, 그 기능의 일부를 추상 메서드나 오버라이딩이 가능한 protected 메서드 등으로 만든 뒤 서브클래스에서 이런 메서드를 필요에 맞게 구현해서 사용하도록 하는 방법을 디자인 패턴에서 **템플릿 메서드 패턴**이라고 한다.

UserDao의 getConnection() 메서드는 Connection 타입의 객체를 생성한다는 기능을 가진 추상 메서드이다. 그리고 서브클래스의 getConnection() 메서드는 어떤 Connection 클래스의 오브젝트를 어떻게 생성할 것인지 결정하는 메서드라고 볼 수 있다. 이렇게 서브클래스에서 구체적인 객체 생성 방법을 결정하게 하는 것을 **팩토리 메서드 패턴**이라고 부르기도 한다.

UserDao는 어떤 기능을 사용한다는 것만 관심을 두고 있는 것이고 구체적으로 어떤 Connection 오브젝트를 만들어내는지는 NUserDao DUserDao의 관심사항이다.

![IMG_E87AB4F1219B-1](https://github.com/pak0426/pak0426/assets/59166263/ea805d1a-6307-4d5e-ab7d-81aa152fa8e2)

하지만 상속을 사용했다는 단점이 있다. 상속은 많은 한계점이 있다.

1. UserDao는 다른 클래스를 상속받지 못한다.
2. 상속을 통한 상하위 클래스의 관계는 생각보다 밀접하다. 슈퍼클래스 내부의 변경이 있을 때 모든 서브클래스를 함께 수정하거나 개발해야 한다.