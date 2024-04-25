## 2.2 UserDaoTest 개선

### 2.2.1 테스트 검증의 자동화

첫 번째 문제점인 테스트 결과의 검증 부분을 코드로 만들어보자.

모든 테스트는 성공과 실패의 두 가지 결과를 가질 수 있다. 또 실패는 테스트가 진행되는 동안에 에러가 발생해서 실패하는 경우와, 테스트 작업 중에 에러가 발생하진 않았지만 그 결과가 기대한 것과 다르게 나오는 경우로 구분해 볼 수 있다. 여기서 전자를 테스트 에러, 후자를 테스트 실패로 부르겠다.

테스트 주에 에러가 발생하는 것은 쉽게 확인 가능하다. 콘솔에 에러 메시지와 긴 호출 스택 정보기 출력되기 떄문이다. 하지만 테스트가 실패하는 것은 별도의 확인 작입과 그 결과가 있어야만 알 수 있다.

이번에는 테스트 실패한 경우 "테스트 실패"라는 메시지를 출력하도록 만들어보자.

```java
if (!user.getName().equals(user2.getName())) {
    System.out.println("테스트 실패 (name)");
} else if (user.getPassword().equals(user2.getPassword())) {
    System.out.println("테스트 실패 (password)");
}
else {
    System.out.println("조회 테스트 성공");
}
```
위와 같이 작성 후 코드를 실행하면 성공 메시지가 출력될 것이다.

이렇게 해서 테스트의 수행, 값 적용, 결과 검증까지 모두 자동화했다. 테스트를 수행하고 할 일은 출력 메시지가 "성공"이라고 나오는 걸 확인하느 것 뿐이다.

이 테스트는 코드의 동작에 영향을 미칠 수 있는 어떤 변화가 생기더라도 언제든 다시 실행해볼 수 있다. 혹시 프레임워크가 기술이 전환하는 변화가 있어도 UserDao가 전과 같이 정상적으로 동작하는지 확인하는 것은 이 테스트 한 번이면 충분하다.

## 2.2.2 테스트의 효율적인 수행과 결과 관리

좀 더 편리하게 테스트를 수행하고 확인하려면 main() 메서드로는 한계가 있다. 이미 자바에는 단순하고 실용적인 테스트를 위한 도구들이 존재한다. 그 중에서 JUnit은 유명한 테스트 지원 도구이며 단위 테스트를 만들 때 유용하다.

### JUnit 테스트로 전환

JUnit은 프레임워크다. 1장에서 프레임워크의 기본 동작원리가 바로 제어의 역전이라고 했다. 프레임워크는 개발자가 만든 클래스에 대한 제어 권한을 넘겨받아서 주도적으로 애플리케이션의 흐름을 제어한다. 개발자가 만든 클래스의 오브젝트를 생성하고 실행하는 일은 프레임워크에 의해 진행된다. 따라서 프레임워크에서 동작하는 코드는 main() 메서드도 필요 없고 오브젝트를 만들어서 실행시키는 코드를 만들 필요도 없다.

### 테스트 메서드 전환

새로 만들 테스트 메서드는 JUnit 프레임워크가 요규하는 조건 두 가지를 따라야 한다. 첫째는 메서드가 public으로 선언돼야 하는 것이고, 다른 하나는 메서드에 @Test라는 애노테이션을 붙여주는 것이다. JUit 프레임워크에서 동작하도록 테스트 메서드를 재구성해보자.

```bash
implementation 'org.junit.jupiter:junit-jupiter-engine'
```
추가해준다.

```java
    @Test
    public void addAndGet() throws SQLException, ClassNotFoundException {
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

        if (!user.getName().equals(user2.getName())) {
            System.out.println("테스트 실패 (name)");
        } else if (!user.getPassword().equals(user2.getPassword())) {
            System.out.println("테스트 실패 (password)");
        }
        else {
            System.out.println("조회 테스트 성공");
        }
    }
```

main() 메서드 대신 일반 메서드로 만들고 적절한 이름을 붙여준다. 테스트의 의미를 바로 알 수 있는 이름이 좋다. 그리고 public 액세스 권한을 주는 것을 잊으면 안된다.

### 검증 코드 전환

테스트의 결과를 검증하는 if/else 문장을 JUnit이 제공하는 방법을 이용해 전환해보자.

```java
assertEquals(user2.getName(), user.getName());
assertEquals(user2.getPassword(), user.getPassword());
```

앞서 작성했던 if문들을 위와 같이 작성할 수 있다. 

JUnit은 예외가 발생하거나 assertEquals()에서 실패하지 않고 테스트 메서드의 실행이 완료되면 테스트가 성공했다고 인식한다.

### JUnit 테스트 실행

<img width="802" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/ed145b13-34ba-4aa2-bd50-192e968730f0">

<img width="737" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/606317f3-ebbf-49f6-a2ea-52873adac61b">

마우스 오른쪽 키를 눌러서 Run을 하거나 코드 좌측에 눌러서 실행할 수 있다.