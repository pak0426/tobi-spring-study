package tobi.study.user.STEP6.스프링_AOP_6_5;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionAdvice implements MethodInterceptor { // 스프링의 어드바이스 인터페이스 구현
    private final PlatformTransactionManager transactionManager;

    public TransactionAdvice(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    // 타깃을 호출하는 기능을 가진 콜백 오브젝트를 프록시로부터 받는다.
    // 덕분에 어드바이스는 특정 타깃에 의존하지 않고 재사용 가능하다.
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            // 콜백을 호출해 타깃 메서드 실행. 타깃 메ㅓ드 호출 전후로 필요한 부가기능을 넣을 수 있다.
            // 경우에 따라 타깃이 아예 호출되지 않게 하거나 재시도를 위한 반복 호출도 가능하다.
            Object ret = invocation.proceed();
            this.transactionManager.commit(status);
            System.out.println("Committing transaction");
            return ret;
        } catch (RuntimeException e) {
            // JDK 동적 프록시가 제공하는 Method 와는 달리 스프링의 MethodInvocation 을 통한 타깃 호출은
            // 예외가 포장되지 않고 타깃에서 보낸 그대로 전달된다.
            this.transactionManager.rollback(status);
            System.out.println("Rolling back transaction due to: " + e.getClass());
            throw e;
        }
    }
}
