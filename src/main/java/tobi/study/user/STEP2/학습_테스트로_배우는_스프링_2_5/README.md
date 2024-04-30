# 2.5 학습 테스트로 배우는 스프링

개발자가 자신이 만든 코드가 아닌 다른 사람이 만든 코드와 기능에 대한 테스트를 작성할 필요가 있을까? 일반적으로 애플리케이션 개발자는 자신이 만들고 있는 코드에 대한 테스트만 작성하면 된다. 하지만 때로는 자신이 만들지 않은 프레임워크나 다른 개발팀에서 만들어서 제공한 라이브러리 등에 대해서도 테스트를 작성해야 한다. 이런 테스트를 **학습 테스트** 라고 한다.

학습 테스틩 목적은 자신이 사용할 API나 프레임워크의 기능을 테스트로 보면서 사용 방법을 익히려는 것이다. 따라서 테스트이지만 프레임워크나 기능에 대한 검증이 목적이 아니다.

## 2.5.1 학습 테스트의 장점

### 다양한 조건에 따른 기능을 손쉽게 확인해볼 수 있다.

학습 테스트는 자동화된 테스트 코드로 만들어지기 때문에 다양한 조건에 따라 기능이 어떻게 동작하는지 빠르게 확인할 수 있다.

### 학습 테스트 코드를 개발 중에 참고할 수 있다.

학습 테스트는 다양한 기능과 조건에 대한 테스트 코드를 개별적으로 만들고 남겨둘 수 있다. 아직 익숙하지 않은 기술을 사용해야 하는 개발자에게 이렇게 미리 만들어진 다양한 기능에 대한 코드가 좋은 참고자료가 된다.

### 프레임워크나 제품을 업그레이드할 때 호환성 검증을 도와준다.

학습 테스트에 애플리케이션에서 자주 사용하는 기능에 대한 테스트를 만들어놓았다면 새로운 버전의 프레임워크나 자주 사용하는 기능에 대한 테스트를 만들어놓았다면 새로운 버전의 프레임워크나 제품을 학습 테스트에만 먼저 적용해본다.

### 테스트 작성에 대한 좋은 훈련이 된다.

### 새로운 기술을 공부하는 과정이 즐거워진다.

## 2.5.2 학습 테스트 예제

### JUnit 테스트 오브젝트 테스트

JUnit 은 테스트 메소드를 수행할 때마다 새로운 오브젝트를 만든다고 했다. 그런데 정말 매번 새로운 오브젝트가 만들어질까? 그에 대한 학습 테스트를 만들어보자.

```java
public class JUnitTest {
    static JUnitTest testObject;

    @Test
    public void test1() {
        System.out.println("JUnitTest.test1");
        System.out.println("this = " + this);
        System.out.println("testObject = " + testObject);
        assertNotEquals(this, testObject);
        testObject = this;
    }

    @Test
    public void test2() {
        System.out.println("JUnitTest.test2");
        System.out.println("this = " + this);
        System.out.println("testObject = " + testObject);
        assertNotEquals(this, testObject);
        testObject = this;
    }

    @Test
    public void test3() {
        System.out.println("JUnitTest.test3");
        System.out.println("this = " + this);
        System.out.println("testObject = " + testObject);
        assertNotEquals(this, testObject);
        testObject = this;
    }
}
```

<img width="591" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/17a1d608-0a0d-4226-919e-9a907c5e3e8e">

테스트를 돌려보면 성공한다. 테스트 메서드가 실행될 때마다 스태틱 변수인 testObject에 저장해둔 오브젝트와 다른 새로운 오브젝트가 만들어졌음을 확인할 수 있다.

이 정도로도 충분할 듯 싶지만 좀 더 개선해보자.

```java
public class JUnitTest {
    static Set<JUnitTest> testObjects = new HashSet<>();

    @Test
    public void test1() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }

    @Test
    public void test2() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }

    @Test
    public void test3() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }
}
```

스태틱 변수로 테스트 오브젝트를 저장할 수 있는 컬렉션을 만들어두고 테스트마다 현재 테스트 오브젝트가 컬렉션에 이미 있는지 확인해보는 테스트이다.

###  스프링 테스트 컨텍스트 테스트

이번에는 스프링 테스트 컨텍스트 테스트 만들어보자. JUnit과 반대로 스프링의 테스트용 애플리케이션 컨텍스트는 테스트 개수에 상관없이 한 개만 만들어진다. 또 이렇게 만들어진 컨텍스트는 모든 테스트에서 공유된다고 했다. 그걸 검증해보자.

<img width="653" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/1b5a93aa-6b06-4ad7-8594-ecf485e8a048">

새로운 설정 파일을 만든다. 이 설정파일에는 아무런 빈을 등록할 필요가 없다. DI 기능이 아니라 애플리케이션 컨텍스트가 만들어지는 방식을 확인해보려는 것이기 때문이다.

<img width="646" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/b43d6cc4-321f-4b97-b4ad-a870f38f8a11">
<img width="653" alt="image" src="https://github.com/pak0426/pak0426/assets/59166263/8e273698-3b5c-423a-a7fc-9c9676abb548">

테스트 메서드에서 매번 동일한 애플리케이션 컨텍스트가 context 변수에 주입됐는지 확인해야 한다.

## 2.5.3 버그 테스트

버그 테스트란 코드에 오류가 있을 때 그 오류를 가장 잘 드러내줄 수 있는 테스트를 말한다. QA팀의 테스트 중에 기능 오류가 발견됐다고 했을때 코드를 뒤져가면서 수정하려고 하기 보다는 먼저 버그 테스트를 만들어보는 편이 유용하다. 

버그 테스트는 실패하도록 만들어야 한다. 버그가 원인이 되서 테스트가 실패하는 코드를 만드는 것이다. 그리고 나서 테스트가 성공할 수 있도록 애플리케이션 코드를 수정한다.

### 테스트의 완성도를 높여준다.

### 버그의 내용을 명확하게 분석하게 해준다.

### 기술적인 문제를 해결하는데 도움이 된다.

# 2.6 정리

2장에서는 다음과 같이 테스트의 필요성과 작성 방법을 살펴봤다.

- 테스트는 자동화돼야 하고, 빠르게 실행할 수 있어야 한다.
- main() 테스트 대신 JUnit 프레임워크를 이용한 테스트 작성이  편리하다.
- 테스트 결과는 일관성 있어야 한다. 코드의 변경 없이 환경이나 테스트 실행 순서에 따라서 결과가 달라지면 안 된다.
- 테스트는 포괄적으로 작성해야 한다. 충분한 검증을 하지 않는 테스트는 없는 것보다 나쁠 수 있다.
- 코드 작성과 테스트 수행의 간격이 짧을수록 효과적이다.
- 테스트하기 쉬운 코드가 좋은 코드다.
- 테스트를 먼저 만들고 테스트를 성공시키는 코드를 만들어가는 테스트 주도 개발 방법도 유용하다.
- 테스트 코드도 애플리케이션 코드와 마찬가지로 적절한 리팩토링이 필요하다.
- @BeforeEach, @AfterEach 를 사용해서 테스트 메서드들의 공통 준비 작업과 정리 작업을 처리할 수 있다.
- 스프링 테스트 컨텍스트 프레임워크를 이용하면 테스트 성능을 향상시킬 수 있다.
- 동일한 설정파일을 사용하는 테스트는 하나의 애플리케이션 컨텍스트를 공유한다.
- @Autowired 를 사용하면 컨텍스트의 빈을 테스트 오브젝트에 DI 할 수 있다.
- 기술의 사용 방법을 익히고 이해를 돕기 위해 학습 테스트를 작성하자.
- 오류가 발견될 경우 그에 대한 버그 테스트를 만들어두면 유용하다.