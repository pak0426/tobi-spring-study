package tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
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
}