package tobi.study.user.STEP6.스프링_AOP_6_5;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.*;

class DynamicProxyTest {
    @Test
    public void simpleProxy() {
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { Hello.class },
                new UppercaseHandler(new HelloTarget())
        );
    }

    @Test
    public void proxyFactoryBean() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget()); // 타깃 설정
        pfBean.addAdvice(new UppercaseAdvice()); // 부가기능을 담은 어드바이스 추가, 여러 개 가능

        Hello proxiedHello = (Hello) pfBean.getObject(); // FactoryBean이므로 getObject()로 생성된 프록시를 가져옴

        assert proxiedHello != null;
        assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
        assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
        assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("THANK YOU HM");
    }

    static class UppercaseAdvice implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 리플렉션의 Method와 달리 메서드 실행 시 타깃 오브젝트를 전달할 필요가 없다. MethodInvocation은
            // 메서드 정보와 함께 타깃 오브젝트를 알고 있기 때문이다.
            String ret = (String) invocation.proceed();
            return ret.toUpperCase();
        }
    }

    @Test
    public void pointcutAdvisor() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());

        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedName("sayH*");

        // 포인트컷 + 어드바이스 = 어드바이저
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));

        Hello proxiedHello = (Hello) pfBean.getObject();

        assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
        assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
        assertThat(proxiedHello.sayThankYou("hm")); // 메서드 이름이 포인트컷 선정 조건에 맞지 않음. 부가기능 적용 x
    }

    @Test
    public void classNamePointcutAdvisor() {
        // 포인트컷 준비
        NameMatchMethodPointcut classMethodPointcut = new NameMatchMethodPointcut() {
            // 익명 내부 클래스 방식으로 클래스 정의
            public ClassFilter getClassFilter() {
                return new ClassFilter() {
                    @Override
                    public boolean matches(Class<?> clazz) {
                        // 클래스 이름이 HelloT로 시작하는 것만 선정
                        return clazz.getSimpleName().startsWith("HelloT");
                    }
                };
            }
        };
        // sayH로 시작하는 메서드 일므을 가진 메서드만 선정
        classMethodPointcut.setMappedName("sayH*");


        // 테스트
        checkAdviced(new HelloTarget(), classMethodPointcut, true);

        class HelloWord extends HelloTarget {};
        checkAdviced(new HelloWord(), classMethodPointcut, false);

        class HelloTom extends HelloTarget {}
        checkAdviced(new HelloTom(), classMethodPointcut, true);
    }

    private void checkAdviced(Object target, Pointcut pointcut, boolean adviced) {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(target);
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
        Hello proxiedHello = (Hello) pfBean.getObject();

        if (adviced) {
            assertThat(proxiedHello.sayHello("hm")).isEqualTo("HELLO HM");
            assertThat(proxiedHello.sayHi("hm")).isEqualTo("HI HM");
            assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("Thank you hm");
        }
        else {
            // 어드바이스 적용 대상 후보에서 탈락
            assertThat(proxiedHello.sayHello("hm")).isEqualTo("Hello hm");
            assertThat(proxiedHello.sayHi("hm")).isEqualTo("Hi hm");
            assertThat(proxiedHello.sayThankYou("hm")).isEqualTo("Thank you hm");
        }
    }
}
