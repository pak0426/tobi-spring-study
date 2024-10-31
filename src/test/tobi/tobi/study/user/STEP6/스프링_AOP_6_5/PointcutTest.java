package tobi.study.user.STEP6.스프링_AOP_6_5;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Bean;
import tobi.study.user.STEP6.스프링_AOP_6_5.pointcut.Target;

import java.rmi.server.ExportException;
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

    @Test
    public void pointcut() throws Exception {
        targetClassPointcutMatches("execution(* *(..))", true, true, true, true, true, true);
    }

    public void pointcutMatches(String expression, Boolean expected, Class<?> clazz, String methodName, Class<?>... args) throws Exception {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);

        assertThat(pointcut.getClassFilter().matches(clazz)
        && pointcut.getMethodMatcher().matches(clazz.getMethod(methodName, args), null))
                .isTrue();
    }

    public void targetClassPointcutMatches(String expression, boolean... expected) throws Exception {
        pointcutMatches(expression, expected[0], Target.class, "hello");
        pointcutMatches(expression, expected[1], Target.class, "hello", String.class);
        pointcutMatches(expression, expected[2], Target.class, "plus", int.class, int.class);
        pointcutMatches(expression, expected[3], Target.class, "minus", int.class, int.class);
        pointcutMatches(expression, expected[4], Target.class, "method");
        pointcutMatches(expression, expected[5], Bean.class, "method");
    }
}
