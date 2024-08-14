package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class HelloTest {
    @Test
    public void simpleProxy() {
        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("mini")).isEqualTo("Hello mini");
        assertThat(hello.sayHi("mini")).isEqualTo("Hi mini");
        assertThat(hello.sayThankYou("mini")).isEqualTo("Thank you mini");
    }

    @Test
    public void upperProxy() {
        Hello proxiedHello = new HelloUppercase(new HelloTarget());
        assertThat(proxiedHello.sayHello("mini")).isEqualTo("HELLO MINI");
        assertThat(proxiedHello.sayHi("mini")).isEqualTo("HI MINI");
        assertThat(proxiedHello.sayThankYou("mini")).isEqualTo("THANK YOU MINI");
    }

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

        String str1 = "abc";
        String str2 = "cde";

        System.out.println("str1.substring(str1.length() - 1) = " + str1.substring(str1.length() - 1));
        boolean b = str2.startsWith(str1.substring(str1.length() - 1));
        System.out.println("b = " + b);
    }
}