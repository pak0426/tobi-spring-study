# 2.1 UserDaoTest 다시 보기

## 2.1.1 테스트의 유용성

이전에 만들었던 테스트 코드는 main() 메서드를 이용해 UserDao 오브젝트의 add(), get() 메서드를 호출하고 그 결과를 화면에 출력해서 그 값을 눈으로 확인시켜준다.

코드를 수정함에 따라 그것이 처음과 동일한 기능을 수행함을 보장해줄 수 있는 방법에는 테스트를 통해 확인하는 방법밖에 없다.

**테스트란 결국 내가 예상하고 의도했던 대로 코드가 정확히 동작하는지를 확인해서, 만든 코드를 확신할 수 있게 해주는 작업이다. 또한 테스트의 결과가 원하는 대로 나오지 않는 경우에는 코드나 설계에 결함이 있다는 것**을 알 수 있다.

## 2.1.2 UserDaoTest의 특징

아래 코드는 지금까지 테스트 했던 코드이다.

```java
class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);

        DaoFactory daoFactory = new DaoFactory();
        UserDao userDao1 = daoFactory.getUserDao();
        UserDao userDao2 = daoFactory.getUserDao();

        System.out.println("userDao1 = " + userDao1);
        System.out.println("userDao2 = " + userDao2);

        UserDao userDao3 = context.getBean("userDao", UserDao.class);
        UserDao userDao4 = context.getBean("userDao", UserDao.class);

        System.out.println("userDao3 = " + userDao3);
        System.out.println("userDao4 = " + userDao4);

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        userDao.add(user);
        System.out.println(user.getId() + " 등록 성공");

        User user2 = userDao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }
}
```
이 테스트 코드의 내용을 정리하면

- 자바에서 가장 손쉽게 실행 가능한 main() 메서드를 이용한다.
- 테스트할 대상인 UserDao의 오브젝트를 가져와 메솓를 호출한다.
- 테스트에 사용할 입력 값(User 오브젝트)를 직접 코드에서 만들어 넣어준다.
- 테스트의 결과를 콘솔에 출력해준다.
- 각 단계의 작업이 에러 없이 끝나면 콘솔에 성공 메시지로 출력해준다.

이 중에서 가장 돋보이는건, main() 메서드의 사용과 UserDao를 직접 호출했다는 것이다.

### 웹을 통한 DAO 테스트 방법의 문제점

보통 웹 프로그갬에서 사용하는 DAO 테스트는 서비스 로직을 모두 구현 후 웹 화면을 띄워 직접 값을 입력해가면서 테스트하는 것이다. 또한 UserDao가 돌려주는 결과를 화면에 출력하는 기능이 만들어져 있어야 확인이 가능하다.

이렇게 웹 화면을 통해 직접 테스트하는 방법이 가장 흔하지만, DAO에 대한 테스트로서는 단점이 너무 많다. 어디서 에러가 났는지 찾는 것도 어렵고 하나의 테스트에 수행하는 클래스와 코드가 너무 많다.

그럼 어떻게 테스트를 만들면 이런 문제를 피할 수 있고 효율적으로 테스트를 활용할 수 있을지 생각해보자.

### 작은 단위의 테스트

테스트하고자 하는 대상이 명확하다면 그 대상에만 집중해서 테스트하는 것이 바람직하다. 테스트는 가능한 작은 단위로 쪼개서 집중해서 할 수 있어야 한다. 관심사의 분리의 원리도 여기에 적용된다. 테스트의 관심이 다르다면 테스트할 대상을 분리하고 집중해서 접근해야 한다.

UserDaoTest는 한 가지 관심에 집중할 수 있게 작은 단위로 만들어진 테스트다. UserDaoTest의 테스트를 수행할 때는 웹 인터페이스나, MVC 클래스, 서비스 오브젝트, 서버에 배포할 필요도 없다.

이렇게 작은 단위의 코드에 대해 테스트를 수행한 것을 **단위 테스트(unit test)** 라고 한다. 여기서 말하는 단위는 그 크기와 범위가 딱 정해진게 아니다. 크게는 사용자 관리기능, 작게 보자면 add() 메서드 하나만을 단위라고 생각할 수 있다.

일반적으로 단위는 작을수록 좋다. 단위를 넘어 다른 코드들은 신경쓰지 않고 테스트가 동작할 수 있으면 좋다.

단위 테스트를 하는 이유는 개발자가 설계하고 만든 코드가 원래 의도한 대로 동작하는지를 개발자 스스로 확인받기 위해서다. 이때 확인의 대상과 단위 조건이 간단하고 명확할수록 좋다.

### 자동수행 테스트 코드

UserDaoTest의 한 가지 특징은 테스트할 데이터가 코드를 통해 제공되고, 테스트 작업 역시 코드를 통해 자동으로 실행한다는 점이다.

DB 연결 준비를 하고, UserDao 오브젝트를 스프링 컨테이너에서 가져와 add() 메서드를 호출하고 등등 IDE 실행 버튼만 클릭해주면 실행이 된다.

이렇게 테스트는 자동으로 수행되도록 코드로 만들어지는 것이 중요하다.

자동으로 수행되는 테스트의 장점은 자주 반복할 수 있다는 것이다. 번거로운 작업이 없고 테스트를 빠르게 할 수 있기 때문에 언제든 코드를 수정하고 나서 테스트해 볼 수 있다.


### 지속적인 개선과 점진적인 개발을 위한 테스트

테스트를 이용하면 새로운 기능도 기대한 대로 동작하는지 확인할 수 있을 뿐만 아니라, 기존에 만들어뒀던 기능들이 새로운 기능을 추가하느라 수정한 코드에 영향을 받지 않고 여전히 잘 동작하는지를 확인할 수도 있다.


## 2.1.3 UserDaoTest의 문제점

### 수동 확인 작업의 번거로움

UserDaoTest는 여전히 사람 눈으로 확인하는 과정이 필요하다. 테스트 수행은 코드에 의해 자동적으로 진행되긴 하지만 테스트의 결과를 확인하는 일은 사람의 책임이므로 완전히 자동으로 테스트뇌는 방법이라고 말할 수 없다.

### 실행 작업의 번거로움

아무리 간단히 실행 가능한 main() 메서드라고 하더라도 매번 그것을 실행하는 것은 번거롭다. 만약 DAO가 수백 개가 된다면 그에 대한 main() 메서드도 그만큼 만들어진다면 수백 번 실행하는 수고가 필요하다.