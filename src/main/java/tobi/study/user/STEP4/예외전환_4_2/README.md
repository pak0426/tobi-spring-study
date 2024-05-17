## 4.2 예외 전환

예외를 다른 것으로 바꿔서 던지는 예외 전환의 목적은 두 가지라고 설명했다.

1. 앞에서 적용해본 것처럼 런타임 예외로 포장해서 굳이 필요하지 않은 catch/throws 를 줄여주는 것이고, 
2. 다른 하나는 로우 레벨의 예외를 좀 더 의미 있고 추상화된 예외로 바꿔서 던져주는 것이다.

스프링의 JdbcTemplate 이 던지는 DataAccessException 은 일단 런타임 예외로 SQLException 을 포장해주는 역할을 한다. 그래서 대부분 복구가 불가능한 예외인 SQLException 에 대해 애플리케이션 레벨에서는 신경 쓰지 않도록 해주는 것이다. 또한 DataAccessException 은 SQLException 에 담긴 다루기 힘든 상세한 예외정보를 의미 있고 일관성 있는 예외로 전환해서 추상화해주려는 용도로 쓰이기도 한다.

### 4.2.1 JDBC의 한계

JDBC는 자바 표준 JDK에서도 가장 많이 사용되는 기능 중의 하나다. 만약 DB별로 다른 API를 제공하고 이를 사용해야 한다고 상상해보자. DB가 바뀔때마다 DAO 코드도 모두 바뀔 것이고, 제각각 다른 API 사용법을 익혀야 할 것이다.

**JDBC는 자바를 이용해 DB에 접근하는 방법을 추상화된 API 형태로 정의해놓고 각 DB 업체가 JDBC 표준을 따라 만들어진 드라이버를 제공하게 해준다.** 내부 구현은 DB마다 다르겠지만 JDBC의 Connection, Statement, ResultSet 등의 표준 인터페이스를 통해 그 기능을 제공해주기 때문에 자바 개발자들은 표준화된 JDBC의 API에만 익숙해지면 DB의 종류에 상관없이 일관된 방법으로 프로그램을 개발할 수 있다. 인터페이스를 사용하는 객체지향 프로그래밍 방법의 장점을 잘 경험할 수 있는 것이 바로 이 JDBC다.

하지만 DB 종류에 상관없이 사용할 수 있는 데이터 액세스 코드를 작성하는 일은 쉽지 않다. 표준화된 JDBC API가 DB 프로그램 개발 방법을 학습하는 부담은 확실히 줄여주지만 DB를 자유롭게 변경해서 사용할 수 있는 유연한 코드를 보장해주지는 못한다. 현실적으로 DB를 자유롭게 바꾸어 사용할 수 있는 DB 프로그램을 작성하는 데는 2가지 걸림돌이 있다.

#### 비표준 SQL

**첫번째 문제는 JDBC 코드에서 사용하는 SQL이다.** SQL은 어느 정도 표준화된 언어이고 몇 가지 표준 규약이 있긴 하지만, 대부분의 DB는 표준을 따르지 않는 비표준 문법과 기능도 제공한다. 이런 비표준 특정 DB 전용 문법은 매우 폭넓게 사용되고 있다. 해당 DB의 특별한 기능을 사용하거나 최적화된 SQL을 만들 때 유용하기 때문이다.

예를 들어
- 대용량 데이터를 처리하는 경우 성능 향상시키기 위해 최적화 기법을 SQL에 적용하거나
- 웹 화면의 페이지 처리를 위해 가져오는 로우의 시작 위치와 개수를 지정하거나
- 쿼리에 조건을 포함시킨다거나
- 특별한 기능을 제공하는 함수를 SQL에 사용 등

이렇게 작성된 비표준 SQL은 결국 DAO 코드에 들어가고, 해당 DAO는 특정 DB에 대한 종속적인 코드가 되고 만다. 다른 DB로 변경하려면 DAO에 담긴 SQL을 적지 않게 수정해야 한다. 보통은 DB가 자주 변경되지도 않고, 사용하는 DB에 최적화하는 것이 중요하므로 비표준 SQL을 거리낌없이 사용한다.

하지만 DB의 변경 가능성을 고려해서 유연하게 만들어야 한다면 SQL은 큰 걸림돌이 된다. 이 문제의 해결책을 생각해보면, 

- 호환 가능한 표준 SQL만 사용하는 방법
- DB별로 별도의 DAO를 만드는 방법
- SQL을 외부에 독립시켜서 DB에 따라 변경해 사용하는 방법

표준 SQL만 사용하는 방법은 간단한 예제 프로그램이라면 모를까 현실성이 없다. **결국 사용할 수 있는 방법은 DAO를 DB별로 만들어 사용하거나 SQL을 외부에서 독립시켜서 바꿔 쓸 수 있게 하는 것이다.**

#### 호환성 없는 SQLException의 DB 에러 정보

**두 번째 문제는 바로 SQLException 이다.** DB를 사용하다가 발생할 수 있는 예외의 원인은 다양하다. 문제는 DB마다 SQL만 다른 것이 아니라 에러의 종류와 원인도 제각각이라는 점이다. 그래서 JDBC는 데이터 처리 중에 발생하는 다양한 예외를 그냥 SQLException 하나에 모두 담아버린다. JDBC API는 이 SQLException 한 가지만 던지도록 설계되어 있다. 예외가 발생한 원인은 SQLException 안에 담긴 에러 코드와 SQL 상태정보를 참조해 봐야 한다. 그런데 SQLException 의 getErrorCode() 로 가져올 수 있는 DB 에러 코드는 DB별로 모두 다르다. DB 벤더가 정의한 고유한 에러 코드를 사용하기 때문이다.

앞에서 메든 add() 메서드에서는 새로운 사용자를 등록하다가 키가 중복돼서 예외가 발생하는 경우를 확인하기 위해 다음과 같은 방법을 사용했다.

```java
if (e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) { ... }
```

SQLException의 에러 코드를 이용해 중복된 값의 등록이 원인인지 확인하는 것이다. 그런데 여기서 사용한 에러 코드는 MySQL 전용 코드일 뿐이다. 

그래서 SQLException은 예외가 발생했을 때의 DB 상태를 담은 SQL 상태 정보를 부가적으로 제공한다. `getSQLState()` 메서드로 예외상황에 대한 상태 정보를 가져올 수 있다. 이 상태정보는 DB별로 달라지는 에러 코드를 대신할 수 있도록, Open Group의 XOPEN SQL 스펙에 정의된 SQL 상태 코드를 따르도록 되어 있다. XOPEN SQL 상태 코드 외에도 JDBC 3.0 에서는 SQL 99의 관계를, JDBC 4.0 에서는 SQL2003의 관례를 따르도록 정의되어 있기는 하다.

예를 들면 통신장애로 DB 연결에 실패했을 경우에는 08S01, 테이블이 존재하지 않는 경우에는 42S02와 같은 식으로 DB 독립적인 표준 상태 코드가 정의되어 있다. 앞의 두 자리는 클래스 코드, 뒤의 세 자리는 서브 클래스 코드로 분류되어 있기도 하다.

SQLException이 이러한 상태 코드를 제공하는 이유는 DB에 독립적인 에러정보를 얻기 위해서다. 그런데 문제는 DB의 JDBC 드라이버에서 SQLException 을 담을 상태 코드를 정확하게 만들어주지 않는다는 점이다. 어떤 경우에는 아예 표준 코드와는 상관없는 엉뚱한 값이 들어 있기도 하고, 어떤 DB 클래스 코드까지는 바로 오지만, 서브 클래스 코드는 일체 무시하고 값을 다 0으로 넣는다거나 하는 식이다. 결과적으로 이 SQL 상태 코드를 믿고 결과를 파악하도록 코드를 작성하는 것은 위험하다.

### 4.2.2 DB 에러 코드 매핑을 통한 전환

DB 종류가 바뀌더라도 DAO를 수정하지 않으려면 이 두 가지 문제를 해결해야 한다. SQL과 관련된 부분은 뒤에서 다루기로 하고, 여기서는 SQLException의 비표준 에러 코드와 SQL 상태 정보에 대한 해결책을 알아보자.

해결 방법은 DB별 에러 코드를 참고해서 발생한 예외의 원인이 무엇인지 해석해주는 기능을 만드는 것이다. 키 값이 중복돼서 중복 오류가 발생하는 경우 MySQL 이라면 1062, 오라클이라면 1, DB2 라면 -803이라는 에러 코드를 받게 된다. 이런 에러 코드 값을 확인할 수 있다면, 키 중복 때문에 발생하는 SQLException 을 DuplicateKeyException 이라는 의미가 분명히 드러나는 예외로 전환할 수 있다. DB 종류에 상관없이 동일한 상황에서 일관된 예외를 전달받을 수 있다면 효과적인 대응이 가능하다.

스프링은 DataAccessException 이라는 SQLException을 대체할 수 있는 런타임 예외를 정의하고 있을 뿐 아니라 DataAccessException 의 서브클래스로 세분화된 예외 클래스들을 정의하고 있다. 

- SQL 문법 때문에 발생하는 에러라면 BadSqlGrammarException 을, 
- DB 커넥션을 가져오지 못했을 때는 DataAccessResourceFailureException 을, 
- 데이터의 제약조건을 위배했거나 일관성을 지키지 않는 작업을 수행했을 때는 DataIntegrityViolationException 을
- 그 중에서도 중복 키 때문에 발생한 경우는 DuplicatedKeyException 을 사용할 수 있다.

이 외에도 데이터 액세스 작업 중에 발생할 수 있는 예외상황을 수십 가지 예외로 분류하고 이를 추상화해 정의한 다양한 예외 클래스를 제공한다.

문제는 DB마다 에러 코드가 제각각이라는 점이다. 일일이 DB별로 에러 코드의 종류를 확인하는 작업을 수행하는 건 너무 부담이 크다. 대신 스프링은 DB별 에러 코드를 분류해서 스프링이 정의한 예외 클래스와 매핑해놓은 에러 코드 매핑정보 테이블을 만들어두고 이를 이용한다.

<img width="773" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/a0303e4d-7861-4c6c-b113-9f123697ec9c">

JdbcTemplate 은 SQLException 을 단지 런타임 예외인 DataAccessException 으로 포장하는 것이 아니라 DB의 에러 코드를 DataAccessException 계층 구조의 클래스 중 하나로 매핑해준다. 전환되는 JdbcTemplate 에서 던지는 예외는 모두 DataAccessException의 서브클래스 타입이다. 드라이버나 DB 메타정보를 참고해서 DB 종류를 확인하고 DB별로 미리 준비된 위와 같은 매핑정보를 참고해서 적절한 예외 클래스를 선택하기 때문에 DB가 달라져도 같은 종류의 에러라면 동일한 예외를 받을 수 있는 것이다.

<img width="778" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/ba8fd85a-e79e-431a-80f5-373b90f66844">

add() 메서드를 스프링의 JdbcTemplate 을 사용하도록 바꾸면 아래와 같다. JdbcTemplate은 체크 예외인 SQLException을 런타임 예외인 DataAccessException 계층구조의 예외로 포장해주기 때문에 add() 메서드에는 예외 포장을 위한 코드가 따로 필요 없다. 또, DB의 종류와 상관없이 중복 키로 인해 발생하는 에러는 DataAccessException의 서브클래스인 DuplicateKeyException 으로 매핑돼서 던져진다. add() 메서드를 사용하는 쪽에서 중복 키 상황에 대한 대응이 필요한 경우에 참고할 수 있도록 DuplicateKeyException 을 메서드 선언에 넣어주면 편리하다.

```java
import org.springframework.dao.DuplicateKeyException;

public void add() throws DuplicateKeyException {
    // JdbcTemplate 을 이용해 User를 add 하는 코드
}
```

JdbcTemplate 을 이용한다면 JDBC에서 발생하는 DB 관련 예외는 거의 신경 쓰지 않아도 된다.

그런데 중복키 에러가 발생했을 때 애플리케이션에서 직접 정의한 예외를 발생시키고 싶을 수 있다. 개발 정책 때문일 수도 있고, 스프링 DuplicateKeyException의 런타임 예외이기 때문에 예외처리를 강제하지 않는 것이 불안해서 그럴 수도 있다. 아무튼 애플리케이션 레벨의 체크 예외인 `DuplicateUserIdException`을 던지게 하고 싶다면 아래와 같이 스프링의 DuplicateKeyException 예외를 전환해주는 코드를 DAO 안에 넣으면 된다.

```java
public void add() throws DuplicateUserIdException {
    try {
        // jdbcTemplate을 이용해 User를 add 하는 코드
    } catch (DuplicateKeyException e) {
        // 로그를 남기는 등의 필요한 작업
        throw new DulicateUserIdException(e);
    }
}
```

JDK 1.6에 포함된 JDBC 4.0 부터는 기존에 JDBC의 단일 예외 클래스였던 SQLException을 스프링의 DataAccessException과 비슷한 방식으로 좀 더 세분화해서 정의하고 있다. SQL 문법 오류인 경우는 SQLSyntaxErrorException, 제약조건 위반인 경우는 SQLIntegrityConstraintViolationException 과 같은 식으로 세분화된 예외를 사용하도록 만들었다. 이 규약을 따르는 드라이버의 경우 좀 더 상세한 예외를 만들어서 전달할 수 있게 됐다.

하지만 SQLException 의 서브클래스이므로 여전히 체크 예외라는 점과 그 예외를 세분화하는 기준이 SQL 상태정보를 이용한다는 점에서 여전히 문제점이 있다. 시간이 더 많이 지나고 JDK 6.0 이상을 사용하며, JDBC 4.0의 스펙을 충실히 따라 정확한 상태정보를 가지고 일관성 있는 예외를 만들어주는 JDBC 드라이버가 충분히 보급된다면 모르겠지만, 아직은 스프링의 에러 코드 매핑을 통한 DataAccessException 방식을 사용하는 것이 이상적이다.

### 4.2.3 DAO 인터페이스와 DataAccessException 계층구조

DataAccessException 은 JDBC의 SQLException 을 전환하는 용도로만 만들어진 건 아니다. JDBC 외의 자바 데이터 액세스 기술에서 발생하는 예외에도 적용된다. 자바에는 JDBC 외에도 데이터를 액세스를 위한 표준 기술이 존재한다. JDO나 JPA는 JDBC와 마찬가지로 자바의 표준 퍼시스턴스 기술이지만 JDBC와는 성격과 사용 방법이 크게 다르다. 또한 오라클의 TopLink 같은 상용 제품이나 오픈소스인 하이버네이트 같은 표준을 따르긴 하지만 독자적인 프로그래밍 모델을 지원하는 ORM 기술도 있다. JDBC를 기반으로 하고, 성격도 비슷하지만 사용 방법과 API, 발생하는 예외가 다른 iBatis도 있다.

DataAccessException 은 의미가 같은 예외라면 데이터 액세스 기술의 종류와 상관없이 일관된 예외가 발생하도록 만들어준다. 데이터 액세스 기술에 독립적인 추상화된 예외를 제공하는 것이다. 스프링이 왜 이렇게 DataAccessException 계층구조를 이용해 기술에 독립적인 예외를 정의하고 사용하게 하는지 생각해보자.

#### DAO 인터페이스와 구현의 분리

DAO를 굳이 따로 만들어서 사용하는 이유는 무엇일까? **가장 중요한 이유는 데이터 액세스 로직을 담은 코드를 성격이 다른 코드에서 분리해놓기 위해서다.** 또한 분리된 DAO는 전략 패턴을 적용해 구현 방법을 변경해서 사용할 수 있게 만들기 위해서이기도 하다. DAO를 사용하는 쪽에서는 DAO가 내부에서 어떤 데이터 액세스 기술을 사용하는지 신경 쓰지 않아도 된다. User와 같은 자바빈으로 만들어진, 특정 기술에 독립적인 단순한 오브젝트를 주고받으면서 데이터 액세스 기능을 사용하기만 하면 된다. 그런 면에서 DAO는 인터페이스를 사용해 구체적인 클래스 정보와 구현 방법을 감추고, DI를 통해 제공되도록 만드는 것이 바람직하다.

그런데 DAO의 사용 기술과 구현 코드는 전략 패턴과 DI를 통해서 DAO를 사용하는 클라이언트에게 감출 수 있지만, 메서드 선언에 나타나는 예외정보가 문제가 될 수 있다. UserDao의 인터페이스를 분리해서 기술에 독립적인 인터페이스로 만들려면 아래와 같이 정의해야 한다.

```java
public interface UserDao {
    public void add(User user);
    // ...
}
```

하지만 위의 코드는 메ㅅ드 선언은 사용할 수 없다. DAO에서 사용하는 데이터 액세스 기술의 API가 예외를 던지기 때문이다. 만약 JDBC API를 사용하는 UserDao 구현 클래스 add() 메서드라면 SQLException 을 던질 것이다. 인터페이스의 메서드 선언에는 없는 예외를 구현 클래스 메서드의 throws에 넣을 수는 없다. 따라서 인터페이스 메서드도 다음과 같이 선언돼야 한다.

```java
public void add(User user) throws SQLException;
```

이렇게 정의한 인터페이스는 JDBC가 아닌 데이터 액세스 기술로 DAO 구현을 전환하면 사용할 수 없다. 데이터 액세스 기술의 API는 자신만의 독자적인 예외를 던지기 때문에 다음과 같이 인터페이스 메서드를 바꿔주면 모르겠지만, SQLException을 던지도록 선언한 인터페이스 메서드는 사용할 수 없다.

```java
public void add(User user) throws PersistentException; // JPA
public void add(User user) throws HibernateException; // Hibernate
public void add(User user) throws JdoException; // JDO
```

결국 인터페이스로 메서드의 구현은 추상화했지만 구현 기술마다 던지는 예외가 다르기 때문에 메서드의 선언이 달라진다는 문제가 발생한다. DAO 인터페이스를 기술에 완전히 독립적으로 만들려면 예외가 일치하지 않는 문제도 해결해야 한다. 가장 단순한 해결 방법은 모든 예외를 다 받아주는 throws Exception으로 선언하는 것이다.

```java
public void add(User user) throws Exception;
```

간단하긴 하지만 무책임한 선언이다. 다행히도 JDBC보다는 늦게 등장한 JDO, Hibernate, JPA 등의 기술은 SQLException 같은 체크 예외 대신 런타임 예외를 사용한다. 따라서 throws에 선언을 해주지 않아도 된다. 남은 것은 SQLException을 던지는 JDBC API를 직접 사용하는 DAO뿐인데, 이 경우에는 DAO 메서드 내에서 런타임 예외로 포장해서 던져줄 수 있다. JDBC를 이용한 DAO에서 모든 SQLException을 런타임 예외로 포장해주기만 한다면 DAO의 메서드는 처음 의도했던 대로 다음과 같이 선언해도 된다.

```java
public void add(User user);
```