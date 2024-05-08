## 3.5 템플릿과 콜백

지금까지 UserDao와 StatementStrategy, JdbcContext를 이용해 만든 코드는 일종의 전략 패턴이 적용된 것이라고 볼 수 있다. 복잡하지만 바뀌지 않는 일정한 패턴을 갖는 작업 흐름이 존재하고 그중 일부분만 자주 바꿔서 사용해야 하는 경우에 적합한 구조다. 전략 패턴의 기본 구조에 익명 내부 클래스를 활용한 방식이다. 이런 방식을 스프링에서는 **템플릿 콜백 패턴** 이라고 부른다. **전략 패턴의 컨텍스트를 템플릿이라 부르고, 익명 내부 클래스로 만들어지는 오브젝트를 콜백이라고 부른다.**

### 3.5.1 템플릿/콜백의 동작 원리

템플릿은 고정된 작업 흐름을 가진 코드를 재사용한다는 의미에서 붙인 이름이다. 콜백은 템플릿 안에서 호출되는 것을 목적으로 만들어진 오브젝트를 말한다.

#### 템플릿/콜백의 특징

템플릿/콜백 패턴의 콜백은 보통 단일 메서드 인터페이스를 사용한다. 템플릿의 작업 흐름 중 특정 기능을 위해 한 번 호출되는 경우가 일반적이기 때문이다. 

하나의 템플릿에서 여러 가지 종류의 전략을 사용해야 한다면 하나 이상의 콜백 오브젝트를 사용할 수도 있다. <br>
콜백은 일반적으로 하나의 메서드를 가진 인터페이스를 구현한 익명 내부 클래스로 만들어진다고 보면 된다.

콜백 인터페이스의 메서드에는 보통 파라미터가 있다. 이 파라미터는 템플릿의 작업 흐름 중에 만들어지는 컨텍스트 정보를 전달받을 때 사용된다. 아래 그림은 템플릿/콜백 패턴의 일반적인 작업 흐름을 보여준다.

<img width="548" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/23b01d4b-1f34-4e7f-bb31-cd7f05241fca">

- 클라이언트의 역할은 템플릿 안에서 실행될 로직을 담은 콜백 오브젝트를 만들고, 콜백이 참조할 정보를 제공하는 것이다. 만들어진 콜백은 클라이언트가 템플릿의 메서드를 호출할 때 파라미터로 전달된다.
- 템플릿은 정해진 작업 흐름을 따라 작업을 진행하다가 내부에서 생성한 참조정보를 가지고 콜백 오브젝트의 메서드를 호출한다. 콜백은 클라이언트 메서드에 있는 정보와 템플릿이 제공한 참조정보를 이용해서 작업을 수행하고 그 결과를 다시 템플릿에 돌려준다.
- 템플릿은 콜백이 돌려준 정보를 사용해서 작업을 마저 수행한다. 경우에 따라 최종 결과를 클라이언트에 다시 돌려주기도 한다.

복잡해 보이지만 DI 방식의 전략 패턴 구조라고 생각하면 간단하다. 클라이언트가 템플릿 메서드를 호출하면서 콜백 오브젝트를 전달하는 것은 메서드 레벨에서 일어나는 DI다. 템플릿이 사용할 콜백 인터페이스를 구현한 오브젝트를 메서드를 통해 주입해주는 DI 작업이 클라이언트가 템플릿의 기능을 호출하는 것과 동시에 일어난다. 일반적인 DI라면 템플릿에 인스턴스 변수를 만들어두고 사용할 의존 오브젝트를 수정자 메서드로 받아서 사용할 것이다.

반면에 템플릿/콜백 방식에서는 매번 메서드 단위로 사용할 오브젝트를 새롭게 전달받는다는 것이 특징이다. 콜백 오브젝트가 내부 클래스로서 자신을 생성한 클라이언트와 콜백이 강하게 결합된다는 면에서도 일반적인 DI와 조금 다르다.

템플릿/콜백 방식은 전략 패턴과 DI의 장점을 익명 내부 클래스 사용 전략과 결합한 독특한 활용법이라고 이해할 수 있다. 단순히 전략 패턴으로만 보기엔 독특한 특징이 많으므로 템플릿/콜백을 하나의 고유한 디자인 패턴으로 기억해두면 편리하다. 다만 이 패턴에 녹아 있는 전략 패턴과 수동 DI를 이해할 수 있어야 한다.

#### JdbcContext에 적용된 템플릿/콜백

앞에서 만들었던 UserDao, JdbcContext와 StatementStrategy의 코드에 적용된 템플릿/콜백 패턴을 한번 살펴보자. 아래 그림은 UserDao, JdbcContext를 템플릿/콜백 패턴의 구조에서 살펴본 것이다. 템플릿과 클라이언트가 메서드 단위인 것이 특징이다.

<img width="578" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/cdf27039-dcba-4d68-8003-ed51c91a08be">

JdbcContext의 workWithStatementStrategy() 템플릿은 리턴 값이 없는 단순한 구조다. 조회 작업에서는 보통 템플릿의 작업 결과를 클라이언트에 리턴해준다. 템플릿의 작업 흐름이 좀 더 복잡한 경우에는 한 번 이상 콜백을 호출하기도 하고 여러 개의 콜백을 클라이언트로부터 받아서 사용하기도 한다.

### 3.5.2 편리한 콜백의 재활용

템플릿/콜백 방식은 템플릿에 담긴 코드를 여기저기서 반복적으로 사용하는 원시적인 방법에 비해 많은 장점이 있다. 당장에 JdbcContext를 사용하기만 해도 기존에 JDBC 기반의 코드를 만들었을 때 발생했던 여러 가지 문제점과 불편한 점을 제거할 수 있다. 클라이언트인 DAO의 메서드는 간결해지고 최소한의 데이터 액세스 로직만 갖고 있게 된다.

#### 콜백의 분리와 재활용

그래서 이번에는 복잡한 익명 내부 클래스의 사용을 최소화할 수 있는 방법을 찾아보자. JDBC의 try/catch/finally에 적용했던 방법을 현재 UserDao의 메서드에도 적용해보는 것이다. 만약 분리를 통해 재사용이 가능한 코드를 찾아낼 수 있다면 익명 내부 클래스를 사용한 코드를 간결하게 만들 수도 있다.

아래에 나온 클라이언트인 deleteAll() 메서드와 익명 내부 클래스로 만든 콜백 오브젝트의 구조를 다시 잘 살펴보자.

```java
public void deleteAll() throws SQLException {
    this.jdbcContextInterface.workWithStatementStrategy(
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

StatementStrategy 인터페이스의 makePreparedStatement() 메서드를 구현한 콜백 오브젝트 코드를 살펴보면 그 내용은 간단하다. 고정된 SQL 쿼리 하나를 담아서 PreparedStatement를 만드는 게 전부다. 바인딩할 파라미터 없이 미리 만들어진 SQL을 이용해 PreparedStatement를 만들기만 하면 되는 콜백이 적지는 않을 것이다. 즉 deleteAll()과 유사한 내용의 콜백 오브젝트가 반복될 가능성이 높다.

그렇다면, 중복될 가능성이 있는 자주 바뀌지 않는 부분을 분리해보자. 메서드의 내용을 통틀어서 바뀔 수 있는 것은 오직 `delete from users`라는 문자열뿐이다. 단순 SQL을 필요로 하는 콜백이라면 나머지 코드는 매번 동일할 것이다.

```java
public void deleteAll() throws SQLException {
    executeSql("delete from users");
}

private void executeSql(final String query) throws SQLException {
    this.jdbcContextInterface.workWithStatementStrategy(
            new StatementStrategy() {
                @Override
                public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
                    return c.prepareStatement(query);
                }
            }
    );
}
```

바뀌지 않는 모든 부분을 빼내서 executeSql() 메서드로 만들었다. 바뀌는 부분인 SQL 문장만 파라미터로 받아서 사용하게 만들었다. SQL을 담은 파라미터를 final로 선언해서 익명 내부 클래스인 콜백 안에서 직접 사용할 수 있게 하는 것만 주의하면 된다.

이렇게 해서 재활용 가능한 콜백을 담은 메서드가 만들어졌다. 이제 모든 고정된 SQL을 실행하는 DAO 메서드는 deleteAll() 메서드처럼 executeSql()을 호출하는 한 줄이면 끝이다. 복잡한 익명 내부 클래스인 콜백을 직접 만들 필요조차 없어졌다. 처음 try/catch/finally 를 다 갖춰서 만들었던 메서드와 바뀐 한 줄짜리 메서드를 비교해보자.


### 콜백과 템플릿의 결합

executeSql() 메서드는 UserDao만 사용하기는 아깝다. 이렇게 재사용 가능한 콜백을 담고 있는 메서드라면 DAO가 공유할 수 있는 템플릿 클래스 안으로 옮겨도 된다. 엄밀히 말해서 템플릿은 JdbcContext 클래스가 아니라 workWithStatementStrategy() 메서드이므로 JdbcContext 클래스로 콜백 생성과 템플릿 호출이 담긴 executeSql() 메서드를 옮긴다고 해도 문제 될 것은 없다. executeSql() 메서드를 JdbcContextImpl로 옮겨주자.

```java
public class JdbcContextImpl implements JdbcContextInterface {
    // ...

    private void executeSql(final String query) throws SQLException {
        workWithStatementStrategy(
                new StatementStrategy() {
                    @Override
                    public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
                        return c.prepareStatement(query);
                    }
                }
        );
    }
}
```

executeSql() 메서드가 JdbcContext로 이동했으니 UserDao의 메서드에서도 아래와 같이 바꿔줘야 한다.

```java
public void deleteAll() throws SQLException {
    jdbcContextInterface.executeSql("delete from users");
}
```

이제 모든 DAO 메서드에서 executeSql() 메서드를 사용할 수 있게 됐다. 익명 내부 클래스의 사용으로 조금 복잡해 보였던 클라이언트 메서드는 이제 깔끔하고 단순해졌다.

아래 그림에서 볼 수 있듯이 결국 JdbcContext 안에 클라이언트와 템플릿, 콜백이 모두 함께 공존하면서 동작하는 구조가 됐다.

<img width="516" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/86896e28-d5de-486a-8270-afbec98f4bc9">

일반적으로는 성격이 다른 코드들은 가능한 한 분리하는 편이 낫지만, 이 경우는 반대다. 하나의 목적을 위해 서로 긴밀하게 연관되어 동작하는 응집력이 강한 코드들이기 때문에 한 군데 모여 있는게 유리하다. 구체적인 구현과 내부의 전략 패턴, 코드에 의한 DI, 익명 내부 클래스 등의 기술은 최대한 감춰두고, 외부에는 꼭 필요한 기능을 제공하는 단순한 메서드만 노출해주는 것이다.

콜백의 작업이 좀 더 복잡한 add()에도 같은 방법을 적용할 수 있다. add() 메서드의 콜백에서는 SQL 문장과 함께 PreparedStatement에 바인딩될 파라미터 내용이 추가돼야 한다. 바인딩 파라미터의 개수는 일정하지 않으므로 자바 5에서 추가된 가변인자로 정의해두는 것이 좋다. 콜백에서 PreparedStatement를 만든 뒤에 바인딩할 파라미터 타입을 살펴서 적절한 설정 메서드를 호출해주는 작업이 조금 복잡할 수는 있겠지만, 한 번 만들어두면 매우 편리하게 사용할 수 있으니 도전해볼 만하다.

### 3.5.3 템플릿/콜백의 응용

지금까지 살펴본 템플릿/콜백 패턴은 사실 스프링에서만 사용할 수 있다거나 스프링만이 제공해주는 독점적인 기술은 아니다. 하지만 스프링만큼 이 패턴을 적극적으로 활용하는 프레임워크는 없다. 스프링의 많은 API나 기능을 살펴보면 템플릿/콜백 패턴을 적용한 경우를 많이 발견할 수 있다.

따지고 보면 DI도 순수한 스프링의 기술은 아니다. 기본적으로는 객체지향의 장점을 잘 살려서 설계하고 구현하도록 도와주는 여러 가지 원칙과 패턴의 활용 결과일 뿐이다. 스프링은 단지 이를 편리하게 사용할 수 있도록 도와주는 컨테이너를 제공하고, 이런 패턴의 사용 방법을 지지해주는 것뿐이다. 템플릿/콜백 패턴도 DI와 객체지향 설계를 적극적으로 응용한 결과다. 스프링에는 다양한 자바 엔터프라이즈 기술에서 사용할 수 있도록 미리 만들어져 제공되는 수십 가지 템플릿/콜백 클래스와 API가 있다.

스프링을 사용하는 개발자라면 당연히 스프링이 제공하는 템플릿/콜백 기능을 잘 사용할 수 있어야 한다. 동시에 템플릿/콜백이 필요한 곳이 있으면 직접 만들어서 사용할 줄도 알아야 한다.

가장 전형적인 템플릿/콜백 패턴의 후보는 try/catch/finally 블록을 사용하는 코드다. 일정한 리소스를 만들거나 가져와 작업하면서 예외가 발생할 가능성이 있는 코드는 보통 try/catch/finally 구조로 코드가 만들어질 가능성이 높다. 예외상황을 처리하기 위한 catch와 리소스를 반납하거나 제거하는 finally가 필요하기 때문이다. 이런 코드가 한두 번 사용되는 것이 아니라 여기저기서 자주 반복된다면 템플릿/콜백 패턴을 적용하기 적당하다.

#### 테스트와 try/catch/finally

간단한 템플릿/콜백 예제를 하나 만들어보자.

파일을 하나 열어서 모든 라인의 숫자를 더한 합을 돌려주는 코드를 만들어보겠다. 개발하면서 테스트를 해야 하니까 숫자가 담긴 파일을 먼저 만들어 준다. 다음과 같이 네 개의 숫자를 담고 있는 numbers.txt 파일을 준비한다.

```text
1
2
3
4
```

모든 라인의 숫자의 합은 10이다. numbers.txt 파일 경로를 주면 10을 돌려주도록 만들면 된다.

```java
class CalcSumTest {
    @Test
    public void sumOfNumbers() throws IOException {
        String filePath = "./numbers.txt";
        Calculator calculator = new Calculator();
        int sum = calculator.calcSum(filePath);
        assertEquals(sum, 10);
    }
}

class Calculator {
    public int calcSum(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        Integer sum = 0;
        String line = null;

        while ((line = br.readLine()) != null) {
            sum += Integer.valueOf(line);
        }
        br.close();
        return sum;
    }
}
```
위의 테스트를 실행해보면 성공이다.

초난감 DAO와 마찬가지로 calcSum() 메서드도 파일을 읽거나 처리하다가 예외가 발생하면, 파일이 정상적으로 닫히지 않고 메서드를 빠져나가는 문제가 발생한다. 따라서 try/finally 블록을 적용해서 어떤 경우에라도 파일이 열렸으면 반드시 닫아주도록 만들어야 한다. 그리고 예외 발생시 로그를 남기는 기능도 추가해보자.

```java
class Calculator {
    public int calcSum(String filePath) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));
            Integer sum = 0;
            String line = null;

            while ((line = br.readLine()) != null) {
                sum += Integer.valueOf(line);
            }

            return sum;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
```

DAO의 JDBC 코드에 적용했던 것과 기본 개념은 같다.  
1. 만들어진 모든 리소스는 확실히 정리하고 빠져나오도록 만드는 것
2. 모든 예외 상황에 대해서는 적절한 처리를 해주도록 하는 것

#### 중복의 제거와 템플릿/콜백 설계

그런데 이번엔 파일에 있는 모든 숫자의 곱을 계산하는 기능을 추가해야 한다는 요구가 발생했다. Calculator라는 클래스의 이름에 맞게 앞으로도 기능이 추가될거라고 생각해보자. 파일을 읽어서 처리하는 비슷한 기능이 필요할 때마다 앞에서 코드를 복사해서 사용할 것인가? 세 번 이상 반복된다면 코드를 개선할 시점이라고 생각하자.

템플릿/콜백 패턴을 적용해보자. 

1. 먼저 템플릿에 담을 반복되는 작업 흐름은 어떤 것인지 살펴보자. 
2. 템플릿이 콜백에게 전달해줄 내부의 정보는 무엇이고, 콜백이 템플릿에게 돌려줄 내용은 무엇인지도 생각해보자.

템플릿이 작업을 마친 뒤 클라이언트에게 전달해줘야 할 것도 있을 것이다. 템플릿/콜백을 적용할 때는 템플릿과 콜백의 경계를 정하고 템플릿이 콜백에게, 콜백이 템플릿에게 각각 전달하는 내용이 무엇인지 파악하는게 가장 중요하다. 그에 따라 콜백의 인터페이스를 정의해야 하기 때문이다.

**가장 쉽게 생각해볼 수 있는 구조는 템플릿이 파일을 열고 각 라인을 읽어올 수 있는 BufferedReader 를 만들어서 콜백에게 전달해주고 콜백이 각 라인을 읽어서 알아서 처리한 후 최종결과만 돌려주는 것이다.**

```java
public interface BufferedReaderCallback {
    int doSomethingWithReader(BufferedReader br) throws IOException;
}
```

이제 템플릿 부분을 메서드로 분리해보자. 템플릿에서는 BufferedReaderCallback 인터페이스 타입의 콜백 오브젝트를 받아서 적절한 시점에 실행해주면 된다. 콜백이 돌려준 결과는 최종적으로 모든 처리를 마친 후에 다시 클라이언트에 돌려주면 된다.

```java
    public int fileReadTemplate(String filePath, BufferedReaderCallback callback) throws IOException {
        BufferedReader br = null;
    
        try {
            br = new BufferedReader(new FileReader(filePath));
            int ret = callback.doSomethingWithReader(br);
            return ret;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException e) { System.out.println(e.getMessage()); }
            }
        }
    }
```
1. BufferedReader를 만들어서 넘겨주는 것과 그 외의 모든 번거로운 작업에 대한 작업 흐름은 템플릿에서 진행하고
2. 준비된 BufferedReader를 이용해 작업을 수행하는 부분은 콜백을 호출해서 처리하도록 만들었다.

이제 calcSum()을 수정해보자.

```java
    public int calcSum(String filePath) throws IOException {
        BufferedReaderCallback sumCallBack = new BufferedReaderCallback() {
            @Override
            public int doSomethingWithReader(BufferedReader br) throws IOException {
                Integer sum = 0;
                String line = null;

                while ((line = br.readLine()) != null) {
                    sum += Integer.valueOf(line);
                }

                return sum;
            }
        };
        return fileReadTemplate(filePath, sumCallBack);
    }

```

이제 파일에 숫자의 곱을 구하는 메서드도 이 템플릿/콜백을 이용해 만들면 된다. 테스트도 수정해보자.

```java
class CalcSumTest {
    private Calculator calculator;

    private final String filePath = "./numbers.txt";

    @BeforeEach
    void setUp() {
        this.calculator = new Calculator();
    }

    @Test
    public void sumOfNumbers() throws IOException {
        assertEquals(10, calculator.calcSum(filePath));
    }

    @Test
    public void multiplyOfNumbers() throws IOException {
        assertEquals(24, calculator.calcMultiply(filePath));
    }
}
```

곱하는 기능을 담은 콜백을 사용하도록 만들어주자.

```java
    public int calcMultiply(String filePath) throws IOException {
        BufferedReaderCallback multiplyCallBack = new BufferedReaderCallback() {
            @Override
            public int doSomethingWithReader(BufferedReader br) throws IOException {
                Integer multiply = 1;
                String line = null;

                while ((line = br.readLine()) != null) {
                    multiply *= Integer.valueOf(line);
                }

                return multiply;
            }
        };
        return fileReadTemplate(filePath, multiplyCallBack);
    }
```

#### 템플릿/콜백의 재설계

템플릿/콜백 패턴을 적용해서 파일을 읽어 처리하는 코드를 상당히 깔끔하게 정리할 수 있었다. 이제 try/catch/finally 블록 없이도 파일을 안전하게 처리하는 코드를 사용할 수 있게 됐다.
그러나 위에서 만든 calcSum()과 calcMultiply()에 두 개의 콜백을 비교해보면. 또 공통적인 패턴을 발견할 수 있다.

먼저 결과를 저장할 변수를 초기화하고, BufferedReader를 이용해서 저장할 변수의 값과 함께 계산하다가 파일을 다 읽었으면 결과를 저장하고 있는 변수의 값을 리턴한다.

**템플릿과 콜백을 찾아낼 떄는, 변하는 코드의 경계를 찾고 그 경계를 사이에 두고 주고받는 일정한 정보가 있는지 확인하면 된다고 했다.** 여기서 바뀌는 코드는 값은 더하거나 곱해주는 코드 뿐이다. 앞에서 전달하는 정보는 처음에 선언한 변수 값인 multiply 또는 sum이다. 로직을 처리한 후 다시 외부로 전달하는 것은 계산한 결과이다.

```java
public interface LineCallback {
    int doSomethingWithReader(BufferedReader br, Integer value) throws IOException;
}
```

LineCallBack은 파일의 각 라인과 현재까지 계산한 값을 넘겨주도록 되어 있다. 그리고 새로운 계산 결과를 리턴 값을 통해 다시 전달받는다. 이 콜백을 기준으로 코드를 다시 정리해보면 템플릿에 포함되는 작업 흐름은 많아지고 콜백은 단순해질 것이다.

```java
    public Integer lineReadTemplate(String filePath, LineCallback callback, int initVal) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));
            Integer res = initVal;
            String line = null;

            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithReader(line, res);
            }
            return res;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException e) { System.out.println(e.getMessage()); }
            }
        }
    }
```

이렇게 수정한 템플릿을 사용하는 코드를 만들어보자.

```java
    public int calcSum(String filePath) throws IOException {
        LineCallback sumCallBack = new LineCallback() {
            @Override
            public int doSomethingWithReader(String line, Integer value) throws IOException {
                return value + Integer.parseInt(line);
            }
        };
        return lineReadTemplate(filePath, sumCallBack, 0);
    }

    public int calcMultiply(String filePath) throws IOException {
        LineCallback multiplyCallBack = new LineCallback() {
            @Override
            public int doSomethingWithReader(String line, Integer value) throws IOException {
                return value * Integer.parseInt(line);
            }
        };
        return lineReadTemplate(filePath, multiplyCallBack, 1);
    }
```

위의 예제를 통해 살펴봤듯이, 템플릿/콜백 패턴은 다양한 작업에 손쉽게 활용할 수 있다. 콜백이라는 이름이 의미하는 것처럼 다시 불려지는 기능을 만들어서 보내고 템플릿과 콜백, 클라이언트 사이에 정보를 주고 받는 일이 처음에는 조금 복잡하게 느껴질 수 있다. 하지만 코드의 특성이 바뀌는 경계를 잘 살피고 그것을 인터페이스를 사용해 분리한다는, 가장 기본적인 객체지향 원칙에만 충실하면 어렵지 않게 템플릿/콜백 패턴을 만들어 활용할 수 있을 것이다.

#### 제네릭스를 이용한 콜백 인터페이스

자바 5에서 추가된 언어적인 특징을 잘 활용하면 좀 더 강력한 템플릿/콜백 구조를 만들 수 있다. 지금까지 사용한 LineCallback과 lineReadTemplate()은 템플릿과 콜백이 만들어내는 결과가 Integer 타입으로 고정되어 있다. 

만약 파일을 라인 단위로 처리해서 만드는 결과의 타입을 다양하게 가져가고 싶다면, **자바 언어에 타입 파라미터라는 개념을 도입한 제네릭스를 이용하면 된다.** 제네릭스를 이용하면 다양한 오브젝트 타입을 지원하는 인터페이스나 메서드를 정의할 수 있다.

파일의 각 라인에 있는 문자를 모두 연결해서 하나의 스트링으로 돌려주는 기능을 만든다고 생각해보자.  이번에는 템플릿이 리턴하는 타입이 스트링이어야 한다. 콜백의 작업 결과도 스트링이어야 한다. 기존에 만들었던 Integer 타입의 결과만 다루는 콜백과 템플릿을 스트링 타입의 값도 처리할 수 있도록 확장해보자.

```java
public interface LineCallback<T> {
    T doSomethingWithReader(String line, T value) throws IOException;
}
```

```java
    public <T> T lineReadTemplate(String filePath, LineCallback<T> callback, T initVal) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));
            T res = initVal;
            String line = null;

            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithReader(line, res);
            }
            return res;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException e) { System.out.println(e.getMessage()); }
            }
        }
    }
```

lineReadTemplate() 메서드는 이제 타입 파라미터 T를 갖는 인터페이스 LineCallback 타입의 오브젝트와 T 타입의 초기값 initVal을 받아서, T 타입의 변수 res를 정의하고, T 타입 파라미터로 선언된 LineCallback의 메서드를 호출해서 처리한 후에 T 타입의 결과를 리턴하는 메서드가 되는 것이다. 이제 LineCallback 콜백과 lineReadTemplate() 템플릿은 파일의 라인을 처리해서 T 타입의 결과를 만들어내는 범용적인 템플릿/콜백이 됐다. 제네릭스 코드에 익숙하지 않으면 타입 파라미터가 많아서 처음 보기에는 복잡해 보일 수 있다. 이럴 땐 타입 파라미터 T를 Integer나 String 같은 특정 타입으로 모두 바꿔서 생각해보면 이해하는 데 도움이 될 것이다.

이제 파일의 모든 라인의 내용을 하나의 문자열로 길게 연결하는 기능을 가진 메서드를 추가해보자.

```java
    public String concatenate(String filePath) throws IOException {
        LineCallback<String> concatenateCallback = new LineCallback<String>() {
            @Override
            public String doSomethingWithReader(String line, String value) throws IOException {
                return value + line;
            }
        };
        return lineReadTemplate(filePath, concatenateCallback, "");
    }
```

테스트 코드도 만들어 보자.

```java
    @Test
    public void concatenateStrings() throws IOException {
        assertEquals(calculator.concatenate(filePath), "1234");
    }
```

파일의 각 라인의 내용을 문자열로 연결해서 합쳐주는 로직이기 때문에 최종 결과는 "1234"가 돼야 한다. 테스트가 성공하는걸 볼 수 있다.

기존의 메서드들도 아래와 같이 바꿔주면 에러가 해결될 것이다.

```java
    // ...
        LineCallback<Integer> sumCallBack = new LineCallback<Integer>() {
            @Override
            public Integer doSomethingWithReader(String line, Integer value) throws IOException {
                // ...
            }
        };
    // ...
```

이렇게 범용적으로 만들어진 템플릿/콜백을 이용하면 파일을 라인 단위로 처리하는 다양한 기능을 편리하게 만들 수 있다.
