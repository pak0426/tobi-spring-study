## 6.3 다이내믹 프록시와 팩토리 빈

### 6.3.1 프록시와 프록시 패턴, 데코레이터 패턴

트랜잭션 경계설정 코드를 비즈니스 로직 코드에서 분리해낼 때 적용했던 기법을 다시 검토해보자.

단순히 확장성을 고려해서 한 가지 기능을 분리한다면 전형적인 전략패턴을 사용하면 된다. 트랜잭션 기능에는 추상화 작업을 통해 이미 전략 패턴이 적용되어 있다. 하지만 전략 패턴으로는 트랜잭션 기능의 구현 내용을 분리해냈을 뿐이다. 트랜잭션을 적용해야 한다는 사실은 코드에 그대로 남아 있다. 아래 그림은 트랜잭션과 같은 부가적인 기능을 위임을 통해 외부로 분리했을 때의 결과를 보여준다. 구체적인 구현 코드는 제거했을지라도 위임을 통해 기능을 사용하는 코드는 핵심 코드와 함께 남아있다.

<img width="698" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/4462748a-dc3b-4c8a-9f9d-624a1200af6a">

**트랜잭션이라는 기능은 사용자 관리 비즈니스 로직과 성격이 다르기 때문에 아예 그 적용 사실 자체를 밖으로 분리할 수 있다.** 아래 그림과 같이 부가기능 전부를 핵심 코드가 담긴 클래스에서 독립시킬 수 있다. 이 방법을 이용해 UserServiceTx 를 만들었고, UserServiceImpl 에는 트랜잭션 관련 코드가 하나도 남지 않게 됐다.

<img width="672" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/5a5c4465-abf7-4084-a326-a5d1106342aa">

이렇게 분리된 부가기능을 담은 클래스는 중요한 특징이 있다. 부가기능 외의 나머지 모든 기능은 원래 핵심기능을 가진 클래스로 위임해줘야 한다. 핵심기능은 부가기능을 가진 클래스의 존재 자체를 모른다. 따라서 부가기능이 핵심기능을 사용하는 구조가 되는 것이다.  
문제는 이렇게 구성했더라도 클라이언트가 핵심기능을 가진 클래스를 직접 사용해버리면 부가기능이 적용될 기회가 없다는 점이다. 그래서 부가기능은 마치 자신이 핵심기능을 가진 클래스인 것처럼 꾸며서, 클라이언트가 자신을 거쳐서 핵심기능을 사용하도록 만들어야 한다. 그러기 위해서는 클라이언트는 인터페이스를 통해서만 핵심기능을 사용하게 하고, 부가기능 자신도 같은 인터페이스를 구현한 뒤에 자신이 그 사이에 끼어들어야 한다. 그러면 클라이언트는 인터페이스만 보고 사용을 하기 때문에 자신은 핵심기능을 가진 클래스를 사용할 것이라고 기대하지만, 사실은 아래 그림처럼 부가기능을 통해 핵심기능을 이용하게 되는 것이다.

<img width="707" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/db1d1cb3-9acc-4db1-93bd-60a972d88ba6">

부가기능 코드에서는 핵심기능으로 요청을 위임해주는 과정에서 자신이 가진 부가적인 기능을 적용해줄 수 있다. 비즈니스 로직 코드에 트랜잭션 기능을 부여해주는 것이 바로 그런 대표적인 경우다.  
이렇게 마치 자신이 클라이언트가 사용하려고 하는 실제 대상인 것처럼 위장해서 클라이언트의 요청을 받아주는 것을 대리자, 대리인과 같은 역할을 한다고 해서 **프록시**라고 부른다. 그리고 프록시를 통해 최종적으로 요청을 위임받아 처리하는 실제 오브젝트를 **타깃** 또는 **실체**라고 부른다. 아래 그림은 클라이언트가 프록시를 통해 타깃을 사용하는 구조를 보여주고 있다.

<img width="714" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/8cdb6e3d-c1ad-481b-b3e8-da046c378118">

프록시의 특징은 타깃과 같은 인터페이스를 구현했다는 것과 프록시가 타깃을 제어할 수 있는 위치에 있다는 것이다.  
프록시는 사용 목적에 따라 2가지로 구분할 수 있다.

1. 클라이언트가 타깃에 접근하는 방법을 제어하기 위해서
2. 타깃에 부가적인 기능을 부여해주기 위해서

**두 가지 모두 대리 오브젝트라는 개념의 프록시를 두고 사용하는 점을 동일하지만, 목적에 따라서 디자인 패턴에서는 다른 패턴으로 구분한다.**

### 데코레이터 패턴

데코레이터 패턴은 타깃에 **부가적인 기능을 런타임 시 다이내믹하게 부여해주기 위해 프록시를 사용하는 패턴**을 말한다. 다이내믹하게 기능을 부가한다는 의미는 컴파일 시점, 즉 코드상에서는 어떤 방법과 순서로 프록시와 타깃이 연결되어 사용되는지 정해져 있지 않다는 뜻이다. 이 패턴의 이름이 데코레이터라고 불리는 이유는 마치 제품이나 케익 등을 여러 겹으로 포장하고 그 위에 장식을 붙이는 것처럼 실제 내용물은 동일하지만 부가적인 효과를 부여해줄 수 있기 때문이다. 따라서 데코레이터 패턴에서는 프록시가 꼭 한 개로 제한되지 않는다. 프록시가 직접 타깃을 사용하도록 고정시킬 필요도 없다. 이를 위해 데코레이터 패턴에서는 같은 인터페이스를 구현한 타켓과 여러 개의 프록시를 사용할 수 있다. 프록시가 여러 개인 만큼 순서를 정해서 단계적으로 위임하는 구조로 만들면 된다.

예를 들어 소스코드를 출력하는 기능을 가진 핵심기능이 있다고 생각해보자. 이 클래스에 데코레이터 개념을 부여해서 타깃과 같은 인터페이스를 구현하는 프록시를 만들 수 있다. 예를 들어 소스코드에 라인넘버를 붙여준다거나, 문법에 따라 색을 변경해주거나, 특정 폭으로 소스를 잘라주거나, 페이지를 표시해주는 등의 부가적인 기능을 프록시로 만들어두고 아래 그림과 같이 런타임 시에 이를 적절한 순서로 조합해서 사용하면 된다.

<img width="720" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/4d93fc43-c971-4eb0-a4ea-260b78d16f1f">

프록시로서 동작하는 각 데코레이터는 위임하는 대상에도 인터페이스로 접근하기 때문에 자신이 최종 타깃으로 위임하는지, 아니면 다음 단계의 데코레이터 프록시로 위임하는지 알지 못한다. 그래서 데코레이터의 다음 위임 대상은 인터페이스로 선언하고 생성자나 수정자 메서드를 통해 위임 대상을 외부에서 런타임 시에 주입받을 수 있도록 만들어야 한다.

자바 IO 패키지의 InputStream 과 OutputStream 구현 클래스는 데코레이터 패턴이 사용된 대표적인 예다. 다음 코드는 InputStream 이라는 인터페이스를 구현한 타깃인 FileInputStream 에 버퍼 읽기 기능을 제공해주는 BufferedInputStream 이라는 데코레이터를 적용한 예다.

```java
InputStream is = new BufferedInputStream(new FileInputStream("a.txt"));
```

UserService 인터페이스를 구현한 타깃인 UserServiceImpl에 트랜잭션 부가기능을 제공해주는 UserServiceTx 를 추가한 것도 데코레이터 패턴을 적용한 것이라고 볼 수 있다. 이 경우는 수정자 메서드를 통해 데코레이터인 UserServiceTx 에 위임할 타깃인 UserServiceImpl 을 주입해줬다.  
인터페이스를 통한 데코레이터 정의와 런타임 시의 다이내믹한 구성 방법은 스프링의 DI를 이용하면 아주 편리하다. 데코레이터 빈의 프로퍼티 같은 인터페이스를 구현한 다른 데코레이터 또는 타깃 빈을 설정하면 된다.  
스프링의 설정을 다시 살펴보자. UserServiceTx 클래스로 선언된 userService 빈은 데코레이터다. UserServiceTx 는 UserService 타입의 오브젝트를 DI 받아서 기능은 위임하지만, 그 과정에서 트랜잭션 경계설정 기능을 부여해준다. 아래 리스트에 나타난 대로 현재는 UserServiceImpl 클래스로 선언된 타깃 빈이 DI 를 통해 데코레이터인 userService 빈에 주입되도록 설정되어 있다. 다이내믹한 부가기능의 부여라는 데코레이터 패턴의 전형적인 적용 예다.

<img width="590" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d078c6b3-2d56-4633-96da-ca8c9883b9f0">

데코레이터 패턴은 **인터페이스를 통해 위임하는 방식이기 때문에 어느 데코레이터에서 타깃으로 연결될지 코드 레벨에선 미리 알 수 없다.** 구성하기에 따라서 여러 개의 데코레이터를 적용할 수도 있다. UserServiceTx 도 UserService 라는 인터페이스를 통해 다음 오브젝트로 위임하도록 되어 있지만 UserServiceImpl 이라는 특정 클래스로 위임하도록 되어 있지 않다.  
데코레이터 패턴은 타깃의 코드에 손대지 않고, 클라이언트가 호출하는 방법도 변경하지 않은 채로 새로운 기능을 추가할 때 유용한 방법이다.

#### 프록시 패턴

일반적으로 사용하는 프록시라는 용어와 디자인 패턴에서 말하는 프록시 패턴은 구분할 필요가 있다.

1. 전자는 클라이언트와 사용 대상 사이에 대리 역할을 맡은 오브젝트를 두는 방법
2. 후자는 프록시를 사용하는 방법 중에서 타깃에 대한 접근 방법을 제어하려는 목적을 가진 경우

프록시 패턴의 프록시는 타깃의 기능을 확장, 추가하지 않는다. 대신 클라이언트가 타깃에 접근하는 방식을 변경해준다. 타깃 오브젝트를 생성하기가 복잡하거나 당장 필요하지 않은 경우에는 꼭 필요한 시점까지 오브젝트를 생성하지 않는 편이 좋다. 그런데 타깃 오브젝트에 대한 레퍼런스가 미리 필요할 수 있다. 이럴 때 프록시 패턴을 적용하면 된다. **클라이언트에게 타깃에 대한 레퍼런스를 넘겨야 하는데, 실제 타깃 오브젝트는 만드는 대신 프록시를 넘겨주는 것이다.** 그리고 프록시의 메서드를 통해 타깃을 사용하려고 시도하면, 그때 **프록시가 타깃 오브젝트를 생성하고 요청을 위임해주는 식이다.**  
만약 레퍼런스를 가지고 있어도 프록시를 통해 생성을 최대한 늦춤으로써 얻는 장점이 많다.  
또는 원격 오브젝트를 이용할때 RMI나 EJB 똔느 각종 리모팅 기술을 이용해 다른 서버에 존재하는 오브젝트를 사용해야 한다면 원격 오브젝트에 대한 프록시를 만들고, 클라이언트는 마치 로컬에서 사용하는 것처럼 프록시를 사용할 수 있다. (프록시는 클라이언트 요청 발생 시 네트워크를 통해 원격 오브젝트를 실행 후 결과를 받아 돌려준다.)

이렇게 프록시 패턴은 타깃의 기능 자체에는 관여하지 않으면서 젖ㅂ근하는 방법을 제어해주는 프록시를 이용하는 것이다. 구조적으로 보자면 프록시와 데코레이터는 유사하다. 다만 프록시는 코드에서 자신이 만들거나 접근할 타깃 클래스 정보를 알고 있는 경우가 많다. 생성을 지연하는 프록시라면 구체적인 생성 방법을 알아야 하기 때문에 타깃 클래스에 대해 직접적인 정보를 알아야 한다. 물론 프록시 패턴이라고 하더라도 인터페이스를 통해 위임하도록 만들 수도 있다. 인터페이스를 통해 다음 호출 대상으로 접근하게 하면 그 사이에 다른 프록시나 데코레이터가 추가도리 수 있기 때문이다. 아래 그림은 접근 제어를 위한 프록시를 두는 프록시 패턴과 컬러, 페이징 기능을 추가하기 위한 프록시를 두는 데코레이터 패턴을 함께 적용한 예다.

<img width="708" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/bea6be3a-b967-4aeb-9efb-aea41132df8f">

앞으로는 타깃과 동일한 인터페이스를 구현하고 클라이언트와 타깃 사이에 존재하면서 기능의 부가 또는 접근 제어를 담당하는 오브젝트를 모두 프록시라고 부르겠다.

### 6.3.2 다이나믹 프록시

프록시는 기존 코드에 영향을 주지 않으면서 타깃의 기능을 확장하거나 접근 방법을 제어할 수 있는 유용한 방법이다. 그럼에도 불구하고 많은 개발자는 타깃 코드를 직접 고치고 말지 번거롭게 프록시를 만들지는 않겠다고 생각한다. 왜냐하면 프록시를 만드는 일이 상당히 번거롭게 느껴지기 때문이다. 매번 새로운 클래스를 정의해야 하고, 인터페이스의 구현해야 할 메서드는 많으면 모든 메서드를 일일이 구현해서 위임하는 코드를 넣어야 하기 때문이다.

프록시도 일일이 모든 인터페이스를 구현해서 클래스를 새로 정의하지 않고도 편리하게 만들어서 사용할 방법은 없을까?

java.lang.reflect 패키지 안에 프록시를 손쉽게 만들 수 있도록 지원해주는 클래스들이 있다.

#### 프록시의 구성과 프록시 작성의 문제점

프록시의 기능
- 타깃과 같은 메서드를 구현하고 있다가 메서드가 호출되면 타깃 오브젝트로 위임한다.
- 지정된 요청에 대해서는 부가기능을 수행한다.

트랜잭션 부가기능을 위해 만든 UserTx는 기능 부가를 위한 프록시다. 아래의 UserTx 코드에서 이 두 가지 기능을 구분해보자.

```java
public class UserServiceTx implements UserService {
    private UserService userService; // 타깃 오브젝트

    // 메서드 구현과 위임
    @Override
    public void add(User user) {
        userService.add(user);
    }

    // 메서드 구현
    @Override
    public void upgradeLevels() {
        // 부가기능 수행
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try { // 부가기능 수행
            userService.upgradeLevels(); // 위임
            this.transactionManager.commit(status); // 부가기능 수행
        } catch (RuntimeException e) { // 부가기능 수행
            this.transactionManager.rollback(status); // 부가기능 수행
            throw e;
        }
    }
}
```

UserServiceTx 코드는 UserService 인터페이스를 구현하고 타깃으로 요청을 위임하는 트랜잭션 부가기능을 수행하는 코드로 구분할 수 있다. 이렇게 프록시의 역할은 **위임과 부가작업**이라는 두 가지로 구분할 수 있다. 그렇다면 프록시를 만들기 어려운 이유는 무엇일까?

- 첫째는 타깃의 인터페이스를 구현하고 위임하는 코드를 작성하기가 번거롭다는 점이다. 부가기능이 필요 없는 메서드도 구현해서 타깃으로 위임하는 코드를 일일이 만들어줘야 한다. 복잡하진 않지만 인터페이스의 메서드가 많아지고 다양해지면 상당히 부담스러운 작업이 될 것이다. 또, 타깃 인터페이스의 메서드가 추가되거나 변경될 때마다 함께 수정해줘야 한다는 부담도 있다.
- 두 번째 문제점은 부가기능 코드가 중복될 가능성이 많다는 점이다. 트랜잭션은 DB를 사용하는 대부분의 로직에 적용될 필요가 있다. 아직까지 add() 메서드에는 트랜잭션 부가기능을 적용하지 않았지만, 사용자를 추가하는 과정에서 다른 작업이 함께 진행돼야 한다면 add() 메서드에도 트랜잭션 경계설정 부가기능이 적용돼야 한다. 메서드가 많아지고 트랜잭션 적용의 비율이 높아지면 트랜잭션 기능을 제공하는 유사한 코드가 여러 메서드에 중복돼서 나타날 것이다.

사용자 관리직 외에도 다양한 비즈니스 로직을 담은 클래스가 만들어질 것이다. 따라서 다양한 타깃 클래스와 메서드에 중복돼서 나타날 가능성이 높다.  
두 번째 문제인 부가기능의 중복 문제는 중복되는 코드를 분리해서 어떻게든 해결해보면 될 것 같지만, 첫 번째 문제인 인터페이스 메서드의 구현과 위임 기능 문제는 간단해 보이지 않는다. 
바로 이런 문제를 해결하는데 유용한 것이 JDK의 다이내믹 프록시다.

#### 리플렉션
다이내믹 프록시는 리플랙션 기능을 이용해 프록시를 만든다. 리플렉션은 자바의 코드 자체를 추상화해서 접근하도록 만든 것이다.

```java
String name = "Spring";
```

이 스트링의 길이를 알고 싶으면 length() 메서드를 호출하면 된다. 일반적으로 name.length() 같이 직접 메서드를 호출하는 코드를 만드는 것이다.  
자바의 모든 클래스는 그 클래스 자체의 구성정보를 담은 Class 타입의 오브젝트를 가지고 있다. 클래스 오브젝트를 이용하면 클래스 코드에 대한 메타정보를 가져오거나 오브젝트를 조작할 수 있다.  
어떤 클래스를 상속하는지, 어떤 인터페이스를 구현했는지, 어떤 필드를 갖고 있고, 타입은 뭐고, 메서드는 어떤게 있고, 더 나아가 필드의 값을 읽고 수정할 수도 있고, 원하는 파라미터 값을 이용해서 메서드를 호출할 수 있다.

리플렉션 API 중에서 메서드에 대한 정의를 담은 Method 라는 인터페이스를 이용해 메서드 호출하는 방법을 알아보자. String 클래스의 정보를 담은 Class 타입의 정보는 String.class 라고 하면 가져올 수 있다. 또는 스트링 오브젝트가 있으면 name.getClass() 라도 해도 된다.

```java
Method lengthMethod = String.class.getMethod("length");
```

스트링이 가진 메서드 중에서 "length" 라는 이름을 갖고, 파라미터는 없는 메서드의 정보를 가져오는 것이다.  Method 인터페이스에 정의된 invoke() 메서드를 사용하면 메서드를 실행 가능하다. 메서드를 실행시킬 대상 오브젝트와 파라미터 목로글 받아 메서드를 호출한 뒤에 그 결과를 Object 타입으로 돌려준다.

```java
public Object invoke(Object object,  Obejct... args)
```

이를 통해 length()를 실행하면

```java
int length =  lengthMethod.invoke(name);  // int length = name.length();
```

```java
class ReflectionTest {
    @Test
    public void invokeMethod() throws Exception {
        String name = "Spring";

        // length()
        assertThat(name.length()).isEqualTo(6);

        Method lengthMethod = String.class.getMethod("length");
        assertThat(lengthMethod.invoke(name)).isEqualTo(6);

        // charAt()
        assertThat(name.charAt(0)).isEqualTo('S');

        Method charAtMethod = String.class.getMethod("charAt", int.class);
        assertThat((Character) charAtMethod.invoke(name, 0)).isEqualTo('S');
    }
}
```

String 클래스의 length() 메서드와 파라미터가 있는 charAt() 클래스를 Method 인터페이스를 이용한 리플렉션 방식으로 호출한 방법을 비교한 것이다.

#### 프록시 클래스

다이내믹 프록시를 이용한 프록시를 만들어보자. 프록시를 적용할 간단한 타깃 클래스와 인터페이스를 아래와 같이 정의한다.

```java
public interface Hello {
    String sayHello(String name);
    String sayHi(String name);
    String sayThankYou(String name);
}

public class HelloTarget implements Hello {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }

    @Override
    public String sayThankYou(String name) {
        return "Thank you " + name;
    }
}
```

이제 Hello 인터페이스를 통해 HelloTarget 오브젝트를 사용하는 클라이언트 역할을 하는 간단한 테스트를 만든다.

```java
class HelloTest {
    @Test
    public void simpleProxy() {
        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("mini")).isEqualTo("Hello mini");
        assertThat(hello.sayHi("mini")).isEqualTo("Hi mini");
        assertThat(hello.sayThankYou("mini")).isEqualTo("Thank you mini");
    }
}
```

이제 Hello 인터페이스를 구현한 프록시를 만들어보자. 프록시에는 **데코레이터 패턴**을 적용해서 타깃인 HelloTarget에 부가기능을 추가하겠다. 위임과 기능 부가라는 두 가지 프록시의 기능을 모두 처리하는 전형적인 프록시 클래스를 만들자.

```java
public class HelloUppercase implements Hello {
    /**
     * 위임할 타깃 오브젝트.
     * 여기서는 타깃 클래스의 오브젝트 인것은 알지만 다른 프록시를 추가할 수도 있으므로 인터페이스로 접근한다.
     */
    Hello hello;
    
    public HelloUppercase(Hello hello) {
        this.hello = hello;
    }
    
    @Override
    public String sayHello(String name) {
        //toUpperCase() -> 위임과 부가기능 적용
        return hello.sayHello(name).toUpperCase();
    }

    @Override
    public String sayHi(String name) {
        return hello.sayHi(name).toUpperCase();
    }

    @Override
    public String sayThankYou(String name) {
        return hello.sayThankYou(name).toUpperCase();
    }
}
```
테스트 코드를 추가해보자.

```java
    @Test
    public void upperProxy() {
        Hello proxiedHello = new HelloUppercase(new HelloTarget());
        assertThat(proxiedHello.sayHello("mini")).isEqualTo("HELLO MINI");
        assertThat(proxiedHello.sayHi("mini")).isEqualTo("HI MINI");
        assertThat(proxiedHello.sayThankYou("mini")).isEqualTo("THANK YOU MINI");
    }
```

이 프록시는 프록시 적용의 일반적인 문제점 두 가지를 모두 가지고 있다. 인터페이스의 모든 메서드를 구현해 위임하도록 코드를 만들어야 하며, 부가기능인 리턴 값을 대문자로 바꾸는 기능이 모든 메서드에 중복돼서 나타난다.

#### 다이내믹 프록시 적용

클래스로 만든 프록시인 HelloUppercase 를 다이내믹 프록시를 이용해 만들어보자.

<img width="704" alt="image" src="https://github.com/user-attachments/assets/160c2c4f-2926-4945-a584-ffd5f5507e38">

다이내믹 프록시는 프록시 팩토리에 의해 **런타임 시 다이내믹하게 만들어지는 오브젝트**다. 타깃의 인터페이스와 같은 타입으로 만들어진다. 클라이언트는 다이내믹 프록시 오브젝트를 타깃 인터페이스를 통해 사용할 수 있다. 이 덕분에 프록시를 만들 때 인터페이스를 모두 구현해가면서 클래스를 정의하는 수고를 덜 수 있다. 프록시 팩토리에게 인터페이스 정보만 제공해주면 해당 인터페이스를 구현한 클래스의 오브젝트를 자동으로 만들어주기 때문이다.  

다이내믹 프록시가 인터페이스 구현 클래스의 오브젝트는 만들어주지만, 프록시로서 필요한 부가기능 제공 코드는 직접 작성해야 한다. 부가기능은 프록시 오브젝트와 독립적으로 InvocationHandler를 구현한 오브젝트에 담는다. InvocationHandler 인터페이스는 다음과 같은 메서드 한 개만 가진 간단한 인터페이스다.

```java
public Object invoke(Object proxy, Method method, Object[] args)
```

invoke() 메서드는 리플렉션 Method 인터페이스를 파라미터로 받는다. 메서드를 호출할 때 전달되는 파라미터도 args로 받는다. 다이내믹 프록시 오브젝트는 클라이언트의 모든 요청을 리플렉션 정보로 변환해서 InvocationHandler 구현 오브젝트의 invoke() 메서드로 넘기는 것이다. 타깃 인터페이스의 모든 메서드 요청이 하나의 메서드로 집중되기 때문에 중복되는 기능을 효과적으로 제공할 수 있다.

남은 것은 각 메서드 요청을 어떻게 처리할지 결정하는 일이다. 리플렉션으로 메서드와 파라미터 정보를 모두 갖고 있으므로 타깃 오브젝트의 메서드를 호출하게 할 수도 있다. 앞에서 리플렉션 학습 테스트를 만들어 Method와 파라미터 정보가 있으면 특정 오브젝트의 메서드를 실행할 수 있음을 확인했다. InvocationHandler 구현 오브젝트가 타깃 오브젝트 레퍼런스를 갖고 있다면 리플렉션을 이용해 간단히 위임코드를 만들어낼 수 있다.

Hello 인터페이스를 제공하면서 프록시 팩토리에게 다이내믹 프록시를 만들어달라고 요청하면 Hello 인터페이스의 모든 메서드를 구현한 오브젝트를 생성해준다. InvocationHandler 인터페이스를 구현한 오브젝트를 제공해주면 다이내믹 프록시가 받는 모든 요청을 InvocationHandler의 invoke() 메서드로 보내준다. Hello 인터페이스의 메서드가 아무리 많더라도 invoke() 메서드 하나로 처리할 수 있다. 아래는 다이내믹 프록시 오브젝트와 InvocationHandler 오브젝트, 타깃 오브젝트 사이의 메서드 호출이 일어나는 과정이다.

<img width="680" alt="image" src="https://github.com/user-attachments/assets/c3cc9366-bcc9-4b0f-bbf2-3c703040c3cd">

다이내믹 프록시를 만들어보자. 먼저 다이내믹 프록시로부터 메서드 호출 정보를 받아서 처리하는 InvocationHandler 를 만들어보자.

```java
public class UppercaseHandler implements InvocationHandler {
    /**
     * 다이내믹 프록시로부터 전달받은 요청을 다시 타깃 오브젝트에 위임해야 하기 때문에 타깃 오브젝트를 주입 받아 둔다.
     */
    Hello target;
    
    public UppercaseHandler(Hello target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 타깃으로 위임. 인터페이스의 메서드 호출에 모두 적용된다.
        String ret = (String) method.invoke(target, args);
        
        // 부가기능 제공
        return ret.toUpperCase();
    }
}
```

다이내믹 프록시로부터 요청을 전달받으려면 InvocationHandler 를 구현해야 한다. 다이내믹 프록시가 클라이언트로부터 받는 모든 요청은 invoke() 메서드로 전달된다. 다이내믹 프록시를 통해 요청이 전달되면 리플렉션 API를 이용해 타깃 오브젝트의 메서드를 호출한다 타깃 오브젝트의 메서드 호출이 끝났으면 프록시가 제공하려는 부가기능을 수행하고 결과를 리턴한다.

이제 이 InvocationHandler 를 사용하고 Hello 인터페이스를 구현하는 프록시를 만들어 보자. 다이내믹 프록시의 생성은 Proxy 클래스의 newProxyInstance() 스태틱 팩토리 메서드를 이용하면 된다.

```java
@Test
public void dynamicProxy() {
    // 생성된 다이내믹 프록시 오브젝트는 Hello 인터페이스를 구현하고 있으므로 Hello 타입으로 캐스팅해도 안전하다.
    Hello proxiedHello = (Hello) Proxy.newProxyInstance(
            getClass().getClassLoader(), // 동적으로 생성되는 다이내믹 프록시 클래스의 로딩에 사용할 클래스 로더
            new Class[] { Hello.class }, // 구현할 인터페이스
            new UppercaseHandler(new HelloTarget()) // 부가기능과 위임 코드를 담은 invocationHandler
    );

    assertThat(proxiedHello.sayHello("mini")).isEqualTo("HELLO MINI");
    assertThat(proxiedHello.sayHi("mini")).isEqualTo("HI MINI");
    assertThat(proxiedHello.sayThankYou("mini")).isEqualTo("THANK YOU MINI");
}
```

사용 방법을 살펴보자.
- 첫 번째 파라미터는 클래스 로더를 제공. 다이내믹 프록시가 정의하는 클래스 로더를 지정하는 것임
- 두 번째 파라미터는 다이내믹 프록시가 구현해야 할 인터페이스다. 다이내믹 프록시는 한 번에 1개 이상의 인터페이스를 구현할 수도 있다. 따라서 인터페이스 배열을 사용한다.
- 마지막 파라미터는 부가기능과 위임 관련 코드를 담고 있는 InvocationHandler 구현 오브젝트를 제공해야 한다. Hello 타입의 타깃 오브젝트를 생성자로 받고, 모든 메서드 호출의 리턴 값을 대문자로 바꿔주는 UppercaseHandler 오브젝트를 전달했다.

newProxyInstance() 에 의해 만들어지는 다이내믹 프록시 오브젝트는 파라미터로 제공한 Hello 인터페이스를 구현한 클래스의 오브젝트이기 때문에 Hello 타입으로 캐스팅이 가능하다. 이제 UppercaseHandler를 사용하는 Hello 인터페이스를 구현한 다이내믹 프록시가 만들어졌으니 Hello 인터페이스를 통해서 사용하면 된다.

#### 다이내믹 프록시의 확장

다이내믹 프록시 방식이 직접 정의해서 만든 프록시보다 훨씬 유연하고 많은 장점이 있다.  
Hello 인터페이스의 메서드가 3개가 아니라 30개로 늘어나면 어떻게 될까? 인터페이스가 바뀐다면 HelloUppercase 처럼 클래스로 직접 구현한 프록시는 매번 코드를 추가해야 한다. 하지만 UppercaseHandler 와 다이내믹 프록시를 생성해서 사용하는 코드는 전혀 손댈 게 없다. 다이내믹 프록시가 만들어질 때 추가된 메서드가 자동으로 포함될 것이고, 부가기능은 invoke() 메서드에서 처리되기 때문이다.

리플렉션은 매우 유연하고 막강한 기능을 가진 대신에 주의 깊게 사용할 필요가 있다. 그래서 Method 를 이용한 타깃 오브젝트의 메서드 호출 후 리턴 타입을 확인해서 스트링인 경우만 대문자로 바꾸고 나머지는 그대로 넘겨주는 방식으로 수정하는 것이 좋겠다.  
InvocationHandler 방식의 또 다른 장점은 타깃의 종류에 상관없이도 적용이 가능하다는 점이다. 어차피 리플렉션의 Method 인터페이스를 이용해 타깃의 메서드를 호출하는 것이니 Hello 타입의 타깃으로 제한할 필요도 없다.  
어떤 종류의 인터페이스를 구현한 타깃이든 상관없이 재사용할 수 있고, 메서드의 리턴 타입이 스트링인 경우만 대문자로 결과를 바꿔주도록 UppercaseHandler 를 만들 수 있다.

```java
public class UppercaseHandler implements InvocationHandler {
    // 어떤 종류의 인터페이스를 구현한 타깃에도 적용 가능하도록 Object 타입으로 수정
    Object target;

    public UppercaseHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 호출한 메서드의 리턴 타입이 String인 경우만 대문자 변경을 가능하도록 수정
        Object ret = method.invoke(target, args);
        if (ret instanceof String) {
            return ((String) ret).toUpperCase();
        }
        return ret;
    }
}
```

InvocationHandler 는 단일 메서드에서 모든 요청을 처리하기 때문에 어떤 메서드에 어떤 기능을 적용할지를 선택하는 과정이 필요할 수도 있다. 호출하는 메서드의 이름, 파라미터의 개수와 타입, 리턴 타입 등의 정보를 가지고 부가적인 기능을 적용할 메서드를 선택할 수 있다.  
리턴 타입뿐 아니라 메서드의 이름도 조건으로 걸 수 있다. 메서드의 이름이 say로 시작하는 경우에만 대문자로 바꾸는 기능을 적용하고 싶다면 아래와 같이 사용하면 된다.

```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = method.invoke(target, args);

        if (ret instanceof String && method.getName().startsWith("say")) {
            return ((String) ret).toUpperCase();
        }
        return ret;
    }
```

### 6.3.3 다이내믹 프록시를 이용한 트랜잭션 부가 기능

UserServiceTx 를  다이내믹 프록시 방식으로 변경해보자. UserServiceTx 는 서비스 인터페이스의 메서드를 모두 구현해야 하고 트랜잭션이 필요한 메서드마다 트랜잭션 처리코드가 중복돼서 나타나는 비효율적인 방법으로 만들어져 있다. 트랜잭션이 필요한 클래스와 메서드가 증가하면 UserServiceTx 처럼 프록시 클래스를 일일이 구현하는 것은 큰 부담이다.

따라서 트랜잭션 부가기능을 제공하는 다이내믹 프록시를 만들어 적용하는 방법이 효율적이다.


#### 트랜잭션 InvocationHandler

```java
public class TransactionHandler implements InvocationHandler {

    private Object target;
    private PlatformTransactionManager transactionManager;
    private String pattern;

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().startsWith(pattern)) {
            return invokeTransaction(method, args);
        }
        return method.invoke(target, args);
    }

    private Object invokeTransaction(Method method, Object[] args) throws Throwable {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            Object ret = method.invoke(target, args);
            this.transactionManager.commit(status);
            return ret;
        } catch (InvocationTargetException e) {
            this.transactionManager.rollback(status);
            throw e.getTargetException();
        }
    }
}
```

요청을 위임할 타깃을 DI로 제공받도록 한다. 타깃을 저장할 변수는 Object 로 선언했다. 따라서 UserServiceImpl 외에 트랜잭션 적용이 필요한 어떤 타깃 오브젝트에도 적용할 수 있다.  
UserServiceTx와 마찬가지로 트랜잭션 추상화 인터페이스인 PlatformTransactionManager 를 DI 받도록 한다. 타깃 오브젝트의 모든 메서드에 무조건 트랜잭션이 적용되지 않도록 트랜잭션을 적용할 메서드 이름의 패턴을 DI 받는다. 간단히 메서드 이름의 시작 부분을 비교할 수 있게 만들었다. pattern을 "get" 으로 주면 get으로 시작하는 모든 메서드에 트랜잭션이 적용된다.

InvocationHandler 의 invoke() 메서드를 구현하는 방법은 UppercaseHandler 에 적용했던 것과 동일하다. 타깃 오브젝트의 모든 메서드에 트랜잭션을 적용하는 게 아니라 선별적으로 적용할 것이므로 적용할 대상을 선별하는 작업을 먼저 진행한다. DI 받은 이름 패턴으로 시작되는 이름을 가진 메서드인지 확인한다. 패턴과 일치하는 이름을 가진 메서드라면 트랜잭션을 적용 하는 메서드를 호출하고, 아니라면 부가기능 없이 타깃 오브젝트의 메서드를 호출해서 결과를 리턴하게 한다.

트랜잭션을 적용하면서 타깃 오브젝트의 메서드를 호출하는 것은 UserServiceTx 에서와 동일하다. 한 가지 차이점은 롤백을 적용하기 위한 예외는 RumtimeException 대신에 InvocationTargetException 을 잡도록 해야 한다는 점이다. 리플렉션 메서드인 Method.invoke() 를 이용해 타깃 오브젝트의 메서드를 호출할 때는 타깃 오브젝트에서 발생하는 예외가 InvocationTargetException 으로 한 번 포장돼서 전달된다. 따라서 일단 InvocationTargetException 으로 받은 후 getTargetException() 메서드로 중첩되어 있는 예외를 가져와야 한다.

#### TransactionHandler 와 다이내믹 프록시를 이용하는 테스트

앞에서 만든 다이내믹 프록시에 사용되는 TransactionHandler 가 UserServiceTx 대신 할 수 있는지 확인하기 위해 테스트를 작성해보자.

```java
@Test
    public void upgradeAllOrNoting() {
        // ...

        TransactionHandler txHandler = new TransactionHandler();
        txHandler.setTarget(testUserService);
        txHandler.setTransactionManager(platformTransactionManager);
        txHandler.setPattern("upgradeLevels");

        UserService txUserService = (UserService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ UserService.class },
                txHandler
        );

        // ...
    }
```

UserServiceTx 오브젝트 대신 TransactionHandler 를 만들고 타깃 오브젝트와 트랜잭션 맴니저, 메서드 패턴을 주입해준다. 이렇게 준비된 TransactionHandler 오브젝트를 이용해 UserService 타입의 다이내믹 프록시를 생성하면 모든 필요한 작업은 끝이다.

upgradeAllOrNothing() 테스트를 실행해보자. 다이내믹 프록시를 이용한 트랜잭션 프록시가 적용됐으므로 테스트는 깔끔하게 성공한다.

### 6.3.4 다이내믹 프록시를 위한 팩토리 빈

이제 TransactionHandler 와 다이내믹 프록시를 스프링의 DI를 통해 사용할 수 있도록 만들어야 할 차례다.

그런데 문제는 DI의 대상이 되는 다이내믹 프록시 오브젝트는 일반적인 스프링의 빈으로는 등록할 방법이 없다는 점이다. 스프링 빈은 기본적으로 클래스 이름과 프로퍼티로 정의된다. 스프링은 지정된 클래스 이름을 가지고 리플렉션을 이용해서 해당 클래스의 오브젝트를 만든다. 클래스의 이름을 갖고 있다면 다음과 같은 방법으로 새로운 오브젝트를 생성할 수 있다. Class의 newInstance() 메서드는 해당 클래스의 파라미터가 없는 생성자를 호출하고, 그 결과 생성되는 오브젝트를 돌려주는 리플렉션 API다.

```java
Date now = (Date) Class.forName("java.util.Date").newInstance();
```

스프링은 내부적으로 리플렉션 API를 이용해 빈 정의에 나오는 클래스 이름을 가지고 빈 오브젝트를 생성한다. 문제는 다이내믹 프록시 오브젝트는 이런 식으로 프록시 오브젝트가 생성되지 않는다는 점이다. **사실 다이내믹 프록시 오브젝트의 클래스가 어떤 것인지 알 수도 없다. 클래스 자체도 내부적으로 다이내믹하게 새로 정의해서 사용하기 때문이다.** 따라서 사전에 프록시 오브젝트의 클래스 정보를 미리 알아내서 스프링 빈에 정의할 방법이 없다. 다이내믹 프록시는 Proxy 클래스의 newProxyInstance() 라는 스태틱 팩토리 메서드를 통해서만 만들 수 있다.

#### 팩토리 빈

사실 스프링은 클래스 정보를 가지고 디폴트 생성자를 통해 오브젝트를 만드는 방법 외에도 빈을 만들 수 있는 방법을 제공한다. 대표적으로 팩토리 빈을 이용한 빈 생성 방법을 들 수 있다. 팩토리 빈이란 스프링을 대신해서 오브젝트의 생성로직을 담당하도록 만들어진 특별한 빈이다.

팩토리 빈을 만드는 방법에는 여러 가지가 있는데, 가장 간단한 방법은 FactoryBean 이라는 인터페이스를 구현하는 것이다. FactoryBean 인터페이스는 아래에 나와 있는 대로 세 가지 메서드로 구성되어 있다.

<img width="621" alt="image" src="https://github.com/user-attachments/assets/4673792b-9b3f-4ac7-b74e-20b48d4c3db0">

FactoryBean 인터페이스를 구현한 클래스를 스프링의 빈으로 등록하면 팩토리 빈으로 동작한다. 팩토리 빈의 동작 원리를 확인할 수 있도록 만들어진 학습 테스트를 살펴보자.

먼저 스프링에서 빈 오브젝트로 만들어 사용하고 싶은 클래스를 하나 정의해보자. 아래 Message 클래스는 생성자를 통해 오브젝트를 만들 수 없다. 오브젝트를 만들려면 반드시 스태틱 메서드를 사용해야 한다.

```java
public class Message {
    String text;

    private Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static Message newMessage(String text) {
        return new Message(text);
    }
}
```

Message 클래스의 오브젝트를 만드려면 newMessage() 라는 스태틱 메서드를 사용해야 한다. 따라서 이 클래스를 직접 스프링 빈으로 등록해서 사용할 수 없다.

```xml
<bean id="m" class=".spring.factorybean.Message">
```

위와 같이 사용할 수 없다.

사실 스프링은 private 생성자를 가진 클래스도 빈으로 등록해주면 리플렉션을 이용해 오브젝트를 만들어준다. 리플렉션은 private 으로 선언된 접근 규약을 위반할 수 있는 강력한 기능이 있기 때문이다. 하지만 생성자를 private 으로 만들었다는 것은 스태틱 메서드를 통해 오브젝트가 만들어져야 하는 중요한 이유가 있기 때문이므로 이를 무시하고 오브젝트를 강제로 생성하면 위험하다.  
Message 클래스의 오브젝트를 생성해주는 팩토리 빈 클래스를 만들어보자. FactoryBean 인터페이스를 구현해서 아래와 같이 만들면 된다.

```java
public class MessageFactoryBean implements FactoryBean<Message> {

    String text;

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Message getObject() throws Exception {
        return Message.newMessage(text);
    }

    @Override
    public Class<?> getObjectType() {
        return Message.class;
    }

    public boolean isSingleton() {
        return false;
    }
}
```

팩토리 빈은 전형적인 팩토리 메서드를 가진 오브젝트다. 스프링은 FactoryBean 인터페이스를 구현한 클래스가 빈의 클래스로 지정되면, 팩토리 빈 클래스의 오브젝트의 getObject() 메서드를 이용해 오브젝트를 가져오고, 이를 빈 오브젝트로 사용한다. 빈의 클래스로 등록된 팩토리 빈은 빈 오브젝트를 생성하는 과정에서만 사용될 뿐이다.

#### 팩토리 빈의 설정 방법

아래에서 볼 수 있듯이 팩토리 빈의 설정은 일반 빈과 다르지 않다. id와 class 애트리뷰트를 사용해 빈의 아이디와 클래스를 지정한다는 면에서는 차이가 없다.

<img width="626" alt="image" src="https://github.com/user-attachments/assets/fce7a63a-9ef9-4ebb-bacb-e4a21ac84d2b">

여타 빈 설정과 다른 점은 message 빈 오브젝트의 타입이 class 애트리뷰트에 정의된 MessageFactoryBean 이 아니라 Message 타입이라는 것이다. Message 빈의 타입은 MessageFactoryBean 의 getObjectType() aㅔ서드가 돌려주는 타입으로 결정된다. 또, getObject() 메서드가 생성해주는 오브젝트가 message 빈의 오브젝트가 된다.


#### 다이내믹 프록시를 만들어주는 팩토리 빈

Proxy 의 newProxyInstance() 메서드를 통해서만 생성이 가능한 다이내믹 프록시 오브젝트는 일반적인 방법으로는 스프링의 빈으로 등록할 수 없다. 대신 팩토리 빈을 사용하면 다이내믹 프록시 오브젝트를 스프링의 빈으로 만들어줄 수가 있다. 팩토리 빈의 getObject() 메서드에 다이내믹 프록시 오브젝트를 만들어주는 코드를 넣으면 되기 때문이다.

팩토리 빈 방식을 통해 아래와 같은 구조로 빈이 만들어지고 관계가 설정되게 하려는 것이다.

<img width="625" alt="image" src="https://github.com/user-attachments/assets/c2799ca2-e963-4d3a-a6eb-e8efd8898037">

스프링 빈에는 팩토리 빈과 UserServiceImpl만 빈으로 등록한다. 팩토리 빈은 다이내믹 프록시가 위임할 티깃 오브젝트인 UserServiceImpl에 대한 레퍼런스를 프로퍼티를 통해 DI를 받아둬야 한다.

다이내믹 프록시를 직접 만들어서 UserService에 적용했던 upgradeAllOrNothing() 테스트의 코드를 팩토리 빈을 만들어서 getObject() 안에 넣어주기만 하면 된다.

#### 트랜잭션 프록시 팩토리 빈

아래는 TransactionHandler를 이용하는 다이내믹 프록시를 생성하는 팩토리 빈 클래스다.

<img width="571" alt="image" src="https://github.com/user-attachments/assets/15b376d7-cb32-4e0a-a260-76ac1178d0f1">

<img width="599" alt="image" src="https://github.com/user-attachments/assets/eabb8055-7351-480d-a364-722ddbae8108">

팩토리 빈이 만드는 다이내믹 프록시는 구현 인터페이스나 타깃의 종류에 제한이 없다. 따라서 UserService 외에도 트랜잭션 부가기능이 필요한 오브젝트를 위한 프록시를 만들 때 얼마든지 재사용이 가능하다.


#### 트랜잭션 프록시 팩토리 빈 테스트

UserServiceTest 테스트 중에서 add() @Autowired 로 가져온 userService 빈을 사용하기 때문에 TxProxyFactoryBean 팩토리 빈이 생성하는 다이내믹 프록시를 통해 UserService 기능을 사용하게 될 것이다. 반면에 upgradeLevels() 와 mockUpgradeLevels() 는 목 오브젝트를 이용해 비즈니스 로직에 대한 단위테스트를 만들었으니 트랜잭션과는 무관하다. 가장 중요한 트랜잭션 적용 기능을 확인하는 upgradeAllOrNothing() 의 경우는 수동 DI를 통해 직접 다이내믹 프록시를 만들어서 사용하니 팩토리 빈이 적용되지 않는다.

add() 의 경우는 단순 위임 방식으로 동작한다. TxProxyFactoryBean 이 다이내믹 프록시를 기대한 대로 완벽하게 구성해주는지는 테스트를 해봐야 안다.