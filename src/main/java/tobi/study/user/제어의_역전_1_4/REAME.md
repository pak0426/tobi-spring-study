# 1.4 제어의 역전(IoC)

## 1.4.1 오브젝트 팩토리

UserDaoTest는 기존에 UserDao가 직접 담당하던 기능, 즉 어떤 ConnectionMaker 구현 클래스를 사용할지를 결정하는 기능을 맡게되었다. UserDao 역할을 클라이언트인 UserDaoTest가 그 수고를 담당하게 된 것이다.

UserDaoTest는 UserDao 기능이 잘 동작하는지를 테스트하려고 만든 것이다. 그런데 지금은 또 다른 책임까지 떠맡고 있는 것이다. 성격이 다른 책임이나 관심사는 분리하는 것이 지금까지 했던 일이다. 그러니 이것도 분리해보자.

### 팩토리

분리시킬 기능을 담당할 클래스를 만들 것이다. 이 클래스의 역할은 객체의 생성 방법을 결정하고 그렇게 만들어진 오브젝트를 돌려주는 것인데, 이런 일을 하는 오브젝트를 **팩토리**라고 부른다.

> 추상 팩토리 패턴이나 팩토리 메서드 패턴과는 다르다.

UserDao, ConnectionMaker 관련 생성 작업을 DaoFactory로 옮기고, UserDaoTest에서는 DaoFactory에 요청해서 미리 만들어진 UserDao 오브젝트를 가져와 사용하게 만든다.

```java
class DaoFactory {
    public UserDao userDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao userDao = new UserDao(connectionMaker);
        return userDao;
    }
}
```

DaoFactory의 userDao 메서드를 호출하면 DConnectionMaker를 사용해 DB 커넥션을 가져오도록 이미 설정된 UserDao 오브젝트를 돌려준다. UserDaoTest는 이제 UserDao가 어떻게 만들어지는지 어떻게 초기화되어 있는지 신경쓰지 않고, 자신의 관심사인 테스트를 위해 활용하기만 하면 그만이다.

```java
class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        UserDao userDao = new DaoFactory().userDao();
        
        // ...
    }
}
```

### 설계도로서의 팩토리

<img width="513" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d2545bb1-422d-41f3-8de1-bdd4ab8bbd91">

이렇게 분리된 오브젝트의 역할과 관계를 분석하면, UserDao와 ConnectionMaker는 각각 어플리케이션의 핵심 데이터 로직과 기술 로직을 담당하고 있고, DaoFactory는 이런 어플리케이션의 오브젝트들을 구성하고 그 관계를 정의하는 책임을 맡고 있음을 알 수 있다.

## 1.4.2 오브젝트 팩토리의 활용

DaoFactory를 좀 더 살펴보면, DaoFactory에 UserDao가 아닌 다른 DAO의 생성 기능을 넣으면 어떻게 될까? 이 경우에 UserDao를 생성하는 userDao() 메서드를 복사해서 메서드들을 계속 새로 만든다면 동일한 역할을 하는 메서드가 중복된다는 문제가 발생한다.

아래의 그림과 같을 것이다.

<img width="534" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/2fefbf4f-81d1-4be6-90e0-4772adf7e107">

중복 문제를 해결하려면 역시 분리해내는 게 가장 좋은 방법이다. ConnectionMaker의 구현 클래스를 결정하고 오브젝트를 만드는 코드를 별도의 메서드로 뽑아내자. DAO를 생성하는 각 메서드에서는 새로 만든 ConnectionMkaer 생성용 메서드를 이용하도록 수정한다.

## 1.4.3 제어권의 이전을 통한 제어관계 역전

제어의 역전이라는 건, 간단히 **프로그램의 제어 흐름 구조가 뒤바뀌는 것**이라고 설명할 수 있다.

일반적으로 프로그램의 흐름은 main() 메서드에서 사용할 오브젝트를 결정하고, 생성하고, 호출하는 식의 작업이 반복된다.

모든 오브젝트가 직접 자신이 사용할 클래스를 정하고 필요한 시점에서 생성해두고, 각 메서드에서 사용한다. 모든 종류의 작업을 사용하는 쪽에서 제어하는 구조이다.

**제어의 역전이란 이런 제어 흐름의 개념을 거꾸로 뒤집는 것이다.**

프레임워크도 제어의 역전 개념이 적용된 대표적인 기술이다.

라이브러리를 사용하는 애플리케이션 코드는 애프리케이션 흐름을 직접 제어한다. 단지 동작하는 중에 필요한 기능이 있을 때 능동적으로 라이브러리를 사용할 뿐이다. 반면에 프레임워크는 거꾸로 애플리케이션 코드가 프레임워크에 의해 사용된다.

우리가 만든 UserDao 와 DaoFactory 에도 제어의 역전이 적용되어 있다. 원래 ConnectionMaker의 구현 클래스를 만들고 오브젝트를 만드는 제어권은 UserDao에게 있었다. 그런데 지금은 DaoFactory에게 있다. 

자신이 어떤 ConnectionMaker 구현 클래스를 만들고 사용할지를 결정할 권한을 DaoFactory에게 있다. 이제 UserDao는 수동적인 존재가 되었다. DaoFactory 가 만들고 초기화해서 자신에게 사용하도록 공금해주는 ConnectionMaker를 사용할 수 밖에 없다. 

더욱이 UserDao와 ConnectionMaker의 구현체를 생성하는 책임도 DaoFactory가 맡고 있다. 바로 이것이 제어의 역전이 일어난 상황이다.