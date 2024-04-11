# DAO의 확장

## 1.3.1 클래스의 분리

<img width="512" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/6c114bc5-2310-4e58-888b-e6f5b88e7c70">

위 그림처럼 SimpleConnectionMaker라는 새로운 클래스를 만들고 DB 생성 기능을 넣는다. 그리고 UserDao는 new 키워드를 사용해서 SimpleConnectionMaker 클래스의 오브젝트를 만들고 이를 add(), get() 메서드에서 사용하면 된다. 각 메서드마다 SimpleConnectionMaker 오브젝트를 만들 수도 있지만, 그보다는 한 번만 SimpleConnectionMaker 오브젝트를 만들어서 저장해두고 이를 계속 사용하는 편이 낫다.

성격이 다른 코드를 화끈하게 분리하기는 잘한 것 같은데, N 사와 D 사에 UserDao 클래스만 공급하고 상속을 통해 DB 커넥션 기능을 확장해서 사용하게 했던 게 다시 불가능해졌다. 왜냐하면 UserDao의 코드가 SimpleConnectionMaker 라는 특정 클래스에 종속되어 있기 때문에 상속을 사용했을 때처럼 UserDao 코드의 수정 없이 DB 커넥션 생성 기능을 변경할 방법이 없다.

이렇게 클래스를 분리한 경우에도 상속을 이용했을 때와 마찬가지로 자유로운 확장이 가능하게 하려면 두 가지 문제를 해결해야 한다.

1. SimpleConnectionMaker 의 메서드가 문제다. 만약 simpleConnectionMaker.makeNewConnection() 의 메서드명이 openConnection() 으로 바뀐다면 UserDao의 add(), get() 메서드 내용이 수정되어야 한다.

2. DB 커넥션을 제공하는 클래스가 어떤 것인지를 UserDao 가 구체적으로 알고 있어야 한다는 점이다. UserDao에 **simpleConnectionMaker** 라는 클래스 타입의 인스턴스 변수까지 정의하고 있으니, N 사에서 다른 클래스를 구현하면 UserDao 자체를 수정해야 한다. 이렇게 UserDao는 바뀔 수 있는 정보 DB 커넥션을 가져오는 클래스, 메서드명 등 구체적인 방법에 종속되어 버린다. 

## 1.3.2 인터페이스의 도입

가장 좋은 해결책은 두 개의 클래스가 긴밀하지 않도록 중간에 추상적인 느슨한 연결고리를 만들어주는 것이다. 추상화란 어떤 것들의 공통적인 성격을 뽑아내어 이를 따로 분리해내는 작업이다. 자바가 추상화를 위해 제공하는 가장 유용한 도구는 인터페이스이다.

결국 오브젝트를 만드려면 구체적인 클래스 하나를 선택해야 하지만 인터페이스로 추상화해놓으면 오브젝트를 만들 때 사용할 클래스가 무엇인지 몰라도 된다.

<img width="652" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/42ae1522-93ea-44a7-a2b9-35119cf9e205">

위 그림에서 UserDao 는 자신이 사용할 클래스가 어떤 것인지 몰라도 된다.

인터페이스는 어떤 기능만 하겠다라고 기능만 정의해놓은 것이다. 따라서 구현 방법은 나타나 있지 않다. 그것은 인터페이스를 구현한 클래스들이 할 일이다.

```java
public class DConnectionMaker implements ConnectionMaker {
    @Override
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        // D사의 독자적인 방법으로 Connection을 생성
        ...
    }
}
```
D사의 개발자는 위와 같이 인터페이스를 구현할 클래스를 만들고, 자신의 DB 연결 기술에 관련된 코드를 작성할 것이다.

그럼 UserDao 코드는 아래와 같이 된다.

```java
class UserDao {
    private ConnectionMaker connectionMaker;

    public UserDao() {
        this.connectionMaker = new DConnectionMaker();
    }

    public void add(User user) throws SQLException, ClassNotFoundException {
        Connection c = connectionMaker.makeNewConnection();

        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) values (?, ?, ?)"
        );
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public User get(String id) throws SQLException, ClassNotFoundException {
        Connection c = connectionMaker.makeNewConnection();

        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?"
        );
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }
}
```

그러나 코드를 보면 Dconnection 이라는 클래스 이름이 보인다. Dconnection 클래스의 생성자를 호출해서 오브젝트를 생성하는 코드가 다음과 같이 여전히 UserDao에 남아 있다.

```java
connectionMaker = new DConnectionMaker();
```

인터페이스를 만들어서 DB 커넥션을 제공하는 클래스에 대한 구체적인 정보는 모두 제거했지만, 초기에 어떤 클래스를 사용할지에 대한 생성자의 코드는 남아있다.

## 1.3.3 관계설정 책임의 분리

UserDao와 ConnectionMaker라는 두 개의 관심을 인터페이스를 써가면서 분리했는데도 왜 구체적인 클래스까지 알아야 할까? 그 이유는 UserDao 안에 분리되지 않은, 또 다른 관심사항이 존재하고 있기 때문이다.

UserDao에는 어떤 ConnectionMake 어던 구현 클래스를 사용할지 결정하는 new DConnectionMaker()라는 코드가 있다. 간단히 말하자면 UserDao와 UserDao가 사용할 ConnectionMaker의 특정 구현 클래스 사이의 관계를 설정해주는 것에 대한 관심이다.

<img width="646" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/0dbac70c-0d78-4884-a07e-1decb645cdff">

UserDao 오브젝트가 DConnectionManager 오브젝트를 사용하게 하려면 두 클래스의 오브젝트 사이에 런타임 사용관계 또는 링크, 또는 의존관계라고 불리는 관계를 맺어주면 된다.

<img width="606" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d6bd33ac-d93e-46c5-b86e-414ab443ec82">

위 그림은 이 두 오브젝트 사이에 의존관계가 만들어진 것을 보여준다. 그리고 위 그림은 모델링 시점의 클래스 다이어그램이 아니라 **런타임 시점의 오브젝트 간 관계를 나타내는 오브젝트 다이어그램**임을 주의하자.

클라이언트는 자기가 UserDao를 사용해야 할 입장이기 때문에 UserDao의 세부 전략이라고도 볼 수 있는 ConnectionMaker의 구현 클래스를 선택하고, 선택한 클래스의 오브젝트를 생성해서 UserDao와 연결해줄 수 있다. 기존의 UserDao에서는 생성자에게 이 책임이 있었다. 자신이 사용할 오브젝트를 직접 만들어서, 자신과 관계를 만들어버리는 것이 기존 UserDao 생성자가 한 일이다. 다시 말하자면 이것은 UserDao의 관심도 아니고 책임도 아니다.

```java
public UserDao(ConnectionMaker connectionMaker) {
    this.connectionMaker = connectionMaker;
}
```

DConnectionMaker가 사라진 이유는 DconnectionMaker를 생성하는 코드는 UserDao와 특정 ConnectionMaker 구현 클래스의 오브젝트 간 관계를 맺는 책임을 담당하는 코드였는데, 그것을 UserDao의 클라이언트에게 넘겨버렸기 때문이다. 이제 클라이언트로서 새로운 책임을 맞게된 UserDaoTest는 아래와 같이 수정한다.

```java
public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao dao = new UserDao(connectionMaker);

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        dao.add(user);
        System.out.println(user.getId() + " 등록 성공");

        User user2 = dao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }
}
```

UserDaoTest는 UserDao와 ConnectionMaker 구현 클래스와의 런타임 의존관계를 설정하는 책임을 담당해야 한다. 그래서 ConnectionMaker의 구현 클래스 오브젝트를 만들고 UserDao 생성자 파라미터에 넣어 두 개의 오브젝트를 연결해준다. 그리고 원래 자기 책임이던 UserDao에 대한 테스트 작업을 수행한다.

만약에 다른 ConnectionMaker의 구현 클래스가 필요하다면 아래와 같이 만들어주면 된다.

```java
public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ConnectionMaker connectionMaker = new NConnectionMaker();
        UserDao dao = new UserDao(connectionMaker);

        // ...
    }
}
```

UserDao는 자신의 관심사이자 책임인 사용자 데이터 액세스 작업을 위해 SQL을 생성하고, 이를 실행하는 데만 집중할 수 있게 됐다. DB 커넥션을 가져오는 방법을 어떻게 변경하든 UserDao 코드는 아무런 영향을 받지 않는다.

<img width="639" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/8f2a13f9-e5ab-493e-8a9e-4729088dd501">

위 그림은 UserDao가 사용할 ConnectionMaker 클래스를 선정하는 책임을 UserDaoTest가 담당하고 있는 구조를 보여준다.

## 1.3.4 원칙과 패턴

### 개방 폐쇄 원칙

개방 폐쇄 원칙(OCP, Open-Closed Principle)을 이용하면 지금까지 해온 리팩토링 작업의 특징과 최종적으로 개선된 설계와 코드의 장점이 무엇인지 효과적으로 설명할 수 있다. 개방 폐쇄 원칙은 깔끔한 설계를 위해 적용한 객체지향 설계 원칙 중 하나이다.

개방 폐쇄 원칙의 정의는 **클래스나 모듈은 확장에 열려있어야 하고 변경에는 닫혀 있어야 한다.**라고 정의할 수 있다.

UserDao는 DB 연결 방법이라는 기능을 확장하는 데는 열려 있다.


### 높은 응집도와 낮은 결합도

개방 폐쇄 원칙은 **높은 응집도**와 **낮은 결합도**라는 소프트웨어 개발의 고전적인 원리로도 설명이 가능하다. 응집도가 높다는 건 **하나의 모듈, 클래스가 하나의 책임 또는 관심사에만 집중되어 있다는 뜻**이다.

**높은 응집도** <br>
응집도가 높다는 것은 변화가 일어날 때 해당 모듈에서 변하는 부분이 크다는 것으로 설명할 수 있다. 즉 변경이 일어날 때 모듈의 많은 부분이 바뀐다면 응집도가 높다고 할 수 있다.

**낮은 결합도** <br>
낮은 결합도는 높은 응집도보다 더 민감한 원칙이다. 책임과 관심사가 다른 오브젝트 또는 모듈과는 낮은 결합도, 즉 느슨하게 열결된 형태를 유지하는 것이 바람직하다.

결합도란 **하나의 오브젝트가 변경이 일어날 때에 관계를 맺고 있는 다른 오브젝트에게 변화를 요구하는 정도**라고 설명할 수 있다.

**전략 패턴**

개선한 UserDaoTest - UserDao - ConnectionTest 구조를 디자인 패턴의 시각으로 보면 **전략 패턴**에 해당한다고 볼 수 있다.

전략 패턴은 자신의 기능 맥락에서, 필요에 따라 변경이 필요한 알고리즘을 인터페이스를 통해 통째로 외부로 분리시키고, 이를 구현한 구체적인 알고리즘 클래스를 필요에 따라 바꿔서 사용할 수 있게 하는 디자인 패턴이다.