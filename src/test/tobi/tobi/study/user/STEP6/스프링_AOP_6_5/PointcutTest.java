package tobi.study.user.STEP6.스프링_AOP_6_5;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Bean;
import tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Target;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

public class PointcutTest {
    @Test
    public void methodSignaturePointcut() throws SecurityException, NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();

        // Target 클래스 minus() 메서드 시그니처
        pointcut.setExpression("execution(public int " + "tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Target.minus(int,int) " +
                "throws java.lang.RuntimeException)");

        // Target.minus()
        assertThat(pointcut.getClassFilter().matches(Target.class)).isTrue();
        assertThat(pointcut.getMethodMatcher().matches(
                Target.class.getMethod("minus", int.class, int.class), null))
                .isTrue();

        // Target.plus() - 메서드 매처에서 실패
        assertThat(pointcut.getClassFilter().matches(Target.class) &&
                pointcut.getMethodMatcher().matches(
                        Target.class.getMethod("plus", int.class, int.class), null))
                .isFalse();

        // Bean.method() - 클래스 필터에서 실패
        assertThat(pointcut.getClassFilter().matches(Bean.class) &&
                pointcut.getMethodMatcher().matches(
                        Bean.class.getMethod("method"), null))
                .isFalse();

    }
}
