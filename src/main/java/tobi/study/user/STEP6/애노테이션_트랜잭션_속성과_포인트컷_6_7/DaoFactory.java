package tobi.study.user.STEP6.애노테이션_트랜잭션_속성과_포인트컷_6_7;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
        UserDaoJdbc userDaoJdbc = new UserDaoJdbc();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }

    @Bean
    public UserService testUserService() {
        return new UserServiceImpl() {  // 익명 클래스로 직접 정의
            private String id = "d";

            @Override
            protected void upgradeLevel(User user) {
                if (user.getId().equals(id)) throw new RuntimeException();
                super.upgradeLevel(user);
            }
        };
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
    @Bean
    public TransactionInterceptor transactionAdvice() {
        TransactionInterceptor txInterceptor = new TransactionInterceptor();
        txInterceptor.setTransactionManager(transactionManager());

        Properties txAttributes = new Properties();
        txAttributes.setProperty("get*", "PROPAGATION_REQUIRED,readOnly");
        txAttributes.setProperty("*", "PROPAGATION_REQUIRED");

        txInterceptor.setTransactionAttributes(txAttributes);
        return txInterceptor;
    }

    @Bean
    public AspectJExpressionPointcut transactionPointcut() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String expression = "bean(*Service)";
        pointcut.setExpression(expression);
        return pointcut;
    }

    @Bean
    public DefaultPointcutAdvisor transactionAdvisor() {
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setPointcut(transactionPointcut());
        advisor.setAdvice(transactionAdvice());
        return advisor;
    }

    @Bean(name = "transactionAdvice")
    public TransactionInterceptor txAdvice() {
        NameMatchTransactionAttributeSource txAttributeSource = new NameMatchTransactionAttributeSource();

        // get* 메소드에 대한 트랜잭션 속성 설정
        RuleBasedTransactionAttribute readOnlyTx = new RuleBasedTransactionAttribute();
        readOnlyTx.setReadOnly(true);
        readOnlyTx.setPropagationBehavior(Propagation.REQUIRED.value());

        // 나머지 메소드에 대한 트랜잭션 속성 설정
        RuleBasedTransactionAttribute requiredTx = new RuleBasedTransactionAttribute();
        requiredTx.setPropagationBehavior(Propagation.REQUIRED.value());

        // 트랜잭션 속성 맵핑
        Map<String, TransactionAttribute> txMethods = new HashMap<>();
        txMethods.put("get*", readOnlyTx);
        txMethods.put("*", requiredTx);

        txAttributeSource.setNameMap(txMethods);

        return new TransactionInterceptor(transactionManager(), txAttributeSource);
    }

    @Bean
    public Advisor txAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* *..*Service.*(..))");

        return new DefaultPointcutAdvisor(pointcut, txAdvice());
    }
}