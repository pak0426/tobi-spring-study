# 1.8 XML을 이용한 설정

스프링은 DaoFactory와 같은 자바 클래스를 이용하는 것 외에도, 다양한 방법을 통해 DI 의존관계 설정정보를 만들 수 있다. 가장 대표적인 것이 바로 XML이다.

XML은 단순한 텍스트 파일이기 때문에 다루기 쉽다. 또, 쉽게 이해할 수 있으며 컴파일과 같은 별도의 빌드 작업이 없다는 것도 장점이다. 환경이 달라져서 오브젝트의 관계가 바뀌는 경우에도 빠르게 변경사항을 반영할
수 있다. 스키마나 DTD를 이용해서 정해진 포맷을 따라 작성됐는지 손쉽게 확인할 수도 있다.

## 1.8.1 XML 설정

스프링의 애플리케이션 컨텍스트는 XML에 담긴 DI 정보를 활용할 수 있다. DI 정보가 담긴 XML 파일은 <beans>를 루트 엘리먼트로 사용한다. 이름에서 알 수 있듯이 <beans> 안에는 여러
개의 <bean>을 정의할 수 있다. XML 설정은 @Configuration과 @Bean이 붙은 자바 클래스로 만든 설정과 내용이 동일하다. @Cofiguration을 <beans>, @Bean을 <bean>에
대응해서 생각하면 이해하기 쉬울 것이다.

하나의 @Bean 메서드를 통해 얻을 수 있는 빈의 DI 정보는 다음 세 가지다.

1. 빈의 이름
 
- @Bean 메서드 이름이 빈의 이름이다. 이 이름은 getBean()에서 사용된다.

2. 빈의 클래스

- 빈 오브젝트를 어떤 클래스를 이용해서 만들지를 정의한다.

3. 빈의 의존 오브젝트

- 빈의 생성자나 수정자 메서드를 통해 의존 오브젝트를 넣어준다. 의존 오브젝트도 하나의 빈이므로 이름이 있을 것이고, 그 이름에 해당하는 메서드를 호출해서 의존 오브젝트를 가져온다. 의존 오브젝트는 하나 이상일
  수도 있다.

XML에서도 <bean>을 사용해서 이 세 가지 정보를 정의할 수 있다.

### connectionMaker() 전환

<img width="684" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/498a91cd-f2d6-4b9d-a85f-da7b7232fd5e">

위 표에는 자바 코드로 만든 설정정보와 그에 대응되는 XML의 설정정보를 비교해뒀다.

<img width="600" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/bef74cb7-854b-4e03-a423-24cf73bebbc2">

XML 설정 파일의 <bean> 태그를 보면 @Bean 메서드에서와 같은 작업이 일어나겠구나라고 떠올릴 수 있으면 좋을 것이다.

### userDao() 전환

userDao()에는 DI 정보의 세 가지 요소가 모두 들어 있다. 여기서 관심을 가질 것은 수정자 메서드를 사용해 의존관계를 주입해주는 부분이다. 스프링 개발자가 수정자 메서드를 선호하는 이유 중에는 XML로
의존관계 정보를 만들 때 편리하다는 점도 있다. 자바빈의 관례를 따라서 수정자 메서드는 프로퍼티가 된다. 프로퍼티 이름은 메서드 이름에서 set을 제외한 나머지 부분을 사용한다. 예를 들어 오브젝트에
setConnectionMaker()라는 이름의 메서드가 있다면 connectionMaker라는 프로퍼티를 갖는다고 할 수 있다.

XML에서는 <property> 태그를 사용해 의존 오브젝트와의 관계를 정의한다. <property> 태그는 name과 ref라는 두 개의 속성을 갖는다. name은 속성의 이름이다. 이 속성 이름으로 수정자
메서드를 알 수 있다. ref는 수정자 메서드를 통해 주입해줄 오브젝트의 빈 이름이다. DI 할 오브젝트도 역시 빈이다. 그 빈의 이름을 지정해주면 된다. @Bean 메서드에서라면 다음과 같은 다른 @Bean
메서드를 호출해서 주입할 오브젝트를 가져온다.

```java
userDao.setConnectionMaker(connectionMaker());
```

여기서 userDao.setConnectionMaker()는 userDao 빈의 connectionMaker 속성을 이용해 의존관계 정보를 주입한다는 듯이다. 메서드 파라미터로 넣는 connectionMaker()는 connectionMaker() 메서드를 호출해서 리턴하는 오브젝트를 주입하라는 의미다. 이 두가지 정보를 <property>의 name 속성과 ref 속성으로 지정해주면 된다. 각 정보를 <property> 태그에 대응하면 다음과 같이 전환이 가능하다.

<img width="412" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/3a731544-6e79-4371-a3f2-094a712fb460">

마지막으로 이 <property> 태그를 userDao 빈을 정의한 <bean> 태그 안에 넣어주면 된다.

<img width="483" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/fe0e01c7-d600-4737-b006-e2d38d89e426">

### XML의 의존관계 주입 정보

이렇게 해서 두 개의 <bean> 태그를 이용해 @Bean 메서드를 모두 XML로 변환했다. 아래와 같이 작성해주면 XML로의 전환 작업이 끝난다.

```xml
<beans>
  <bean id="connectionMaker" class="springbook.user.dao.DConnectionMaker" />
  <bean id="userDao" class="springbook.user.dao.UserDao">
    <property name="connectionMaker" ref="connectionMaker"/>
  </bean>
</beans>
```


`<property>` 태그의 name과 ref는 의미가 다르므로 이름이 같더라도 어떤 차이가 있는지 구별할 수 있어야 한다.

<img width="597" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/d7d546e1-dc8d-4b26-b4ed-30e160831f42">

때로는 같은 인터페이스를 구현한 의존 오브젝트를 여러 개 정의해두고 그 중에서 원하는 걸 골라 DI하는 경우도 있다. 이때는 각 빈의 이름을 독립적으로 만들어주고 ref 속성을 이용해 DI 받을 빈을 지정해주면 된다.

<img width="623" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/1e77a203-34bf-4d3b-9382-e76991178114">

## 1.8.2 XML을 이용하는 애플리케이션 컨텍스트

이제 애플리케이션 컨텍스트가 DaoFactory 대신 XML 설정정보를 활용하도록 만들어보자. XML에서 빈의 의존관계 정보를 이용하는 IoC/DI 작업에는 GenericXmlApplicationContext 를 사용한다. GenericXmlApplicationContext의 생성자 파라미터로 XML 파일의 클래스패스를 지정해주면 된다. XML 설정파일은 클래스패스 최상단에 두면 편하다.

application.xml을 만들보자. (SpringBoot라면 /resource 폴더에 만들면 된다.)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="connectionMaker" class="tobi.study.user.XML을_이용한_설정_1_8.DConnectionMaker" />

    <bean id="userDao" class="tobi.study.user.XML을_이용한_설정_1_8.UserDao">
        <property name="connectionMaker" ref="connectionMaker" />
    </bean>
</beans>
```

다음으로 UserDaoTest의 애플리케이션 컨텍스트 생성 부분을 수정한다. 기존에 사용했던 AnnotationConfigApplicationContext 대신 GenericXmlApplicationContext를 이용해서 다음과 같이 애플리케이션 컨텍스트를 생성하게 만든다.

```java
class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");
        UserDao userDao = context.getBean("userDao", UserDao.class);
    }
}
```

UserDaoTest를 실행해보면 똑같은 결과가 나온다.

<img width="468" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/854fe714-5820-4b6e-bbfd-cd5183070256">

성공한다면 이제 XML 설정파일을 사용하도록 변환하는 작업을 모두 마친 것이다.

GenericXmlApplicationContext 외에도 ClassPathXmlApplicationContext 를 이용해 XML로부터 설정정보를 가져오는 애플리케이션 컨텍스트를 만들 수 있다. 
- GenericXmlApplicationContext는 클래스 패스 뿐 아니라 다양한 소스로부터 설정파일을 읽어올 수 있다.
- ClassPathXmlApplicationContext는 XML 파일을 클래스패스에서 가져올 때 사용할 수 있는 편리한 기능이 추가된 것이다.

ClassPathXmlApplicationContext의 기능 중에는 클래스패스의 경로 정보를 클래스에서 가져오게 하는 것이 있다. 예를 들어 springbook.user.dao 패키지 안에 daoContext.xml 이라는 설정 파일을 만들었다고 해보자. GenericXmlApplicationContext가 이 XML 설정파일을 사용하게 하려면 클래스 패스 루트로부터 파일의 위치를 지정해야 하므로 다음과 같이 작성해야 한다.

```java
new GenericXmlApplicationContext("springbook/user/dao/daoContext.xml");
```

반면에 ClassPathXmlApplicationContext는 XML 파일과 같은 클래스패스에 있는 클래스 오브젝트를 넘겨서 클래스패스에 대한 힌트를 제공할 수 있다. UserDao는 springbook.user.dao 패키지에 있으므로 daoContext.xml 과 같은 클래스패스 위에 있다. 따라서 아래와 같이 지정할 수 있다.

```java
new ClassPathXmlApplicationContext("daoContext.xml", UserDao.class);
```

이 방법으로 클래스패스를 지정할 경우가 아니라면 GenericXmlApplicationContext를 사용하는 편이 무난하다.

## 1.8.3 DataSource 인터페이스로 변환

### DataSource 인터페이스 적용

javax.sql.DataSource 인터페이스를 살펴보자.

ConnectionMaker는 DB 커넥션을 생성해주는 기능 하나만을 정의한 매우 단순한 인터페이스다. IoC와 DI의 개념을 설명하기 위해 직접 이 인터페이스를 정희하고 사용했지만, 사실 자바에서는 DB 커넥션을 가져오는 오브젝트의 기능을 추상화해서 비슷한 용도로 사용할 수 있게 만들어진 DataSource 인터페이스가 이미 존재한다. 따라서 실전에서 ConnectionMaker와 같은 인터페이스를 직접 만들어서 사용할 일은 없을 것이다.

아래 코드에서 관심을 가질 것은 getConnection() 메서드 하나 뿐이다. 이름만 다르지 ConnectionMaker 인터페이스의 makeConnection()과 목적이 동일한 메서드다. DAO에서는 DataSource의 getconnection() 메서드를 사용해 DB 커넥션을 가져오면 된다.

```java
public interface DataSource extends CommonDataSource, Wrapper {
    Connection getConnection() throws SQLException;
}
```

DataSource 인터페이스와 구현 클래스를 사용할 수 있도록 UserDao를 리팩토링 해보자.

코드를 아래와 같이 바꿔준다.

```java
class UserDao {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(User user) throws SQLException, ClassNotFoundException {
        Connection c = dataSource.getConnection();
        
        // ...
    }
}
```

다음은 DataSource 구현 클래스가 필요하다. 앞에서 만들었던 DriverManager를 사용하는 SimpleConnectionMaker처럼 단순한 DataSource 구현 클래스를 하나 가져다 사용하자.

### 자바 코드 설정 방식

먼저 DaoFactory 설정 방식을 이용해보자. 기존 코드를 변경해 보겠다.

```java
implementation 'com.h2database:h2'
implementation 'org.springframework:spring-jdbc'
```
라이브러리를 추가해준다.

그리고 DaoFactory 클래스를 아래와 같이 수정한다.

```java
package tobi.study.user.XML을_이용한_설정_1_8;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

@Configuration
class DaoFactory {
  @Bean
  public DataSource dataSource() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

    dataSource.setDriverClass(org.h2.Driver.class);
    dataSource.setUrl("jdbc:h2:tcp://localhost/~/tobiSpringStudy");
    dataSource.setUsername("sa");
    dataSource.setPassword("");

    return dataSource;
  }

  @Bean
  public UserDao userDao() {
    UserDao userDao = new UserDao();
    userDao.setDataSource(dataSource());
    return userDao;
  }

  @Bean
  public ConnectionMaker connectionMaker() {
    return new DConnectionMaker();
  }

  public UserDao getUserDao() {
    UserDao userDao = new UserDao();
    userDao.setDataSource(dataSource());
    return userDao;
  }
}
```

그리고 UserDaoTest를 DaoFactory를 사용하도록 바꿔준다음 테스트를 해보자.

```java
public static void main(String[] args) throws SQLException, ClassNotFoundException {
//        ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");
    ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
}
```

### XML 설정 방식

이번에는 XML 설정 방식으로 변경해보자.

먼저 id가 connectionMaker인 <bean>을 없애고 dataSource라는 이름의 <bean>을 등록한다.

```xml
<bean id="dataSource" class="org.springframework.jdbc.datasource.SimpleDriverDataSource" />
```

하지만 이 <bean> 설정으로 SimpleDriverDataSource의 오브젝트를 만드는 것까지 가능하지만, dataSource() 메서드에서 수정자로 넣어준 DB 접속 정보는 나타나 있지 않다. UserDao처럼 다른 빈에 의존하는 경우 <property> 태그와 ref 속성으로 의존할 빈 이름을 넣어주면 된다. 하지만 SimpleDriverDataSource 오브젝트의 경우는 단순 Class 타입의 오브젝트나 텍스트 값이다. 그렇다면 XML에서는 어떻게 해서 dataSource() 메서드에서처럼 DB 연결정보를 넣도록 설정을 만들 수 있을까?

## 1.8.4 프로퍼티 값의 주입

### 값 주입

이렇게 다른 빈 오브젝트의 레퍼런스가 아닌 단순 정보도 오브젝트를 초기화하는 과정에서 수정자 메서드에 넣을 수 있다.