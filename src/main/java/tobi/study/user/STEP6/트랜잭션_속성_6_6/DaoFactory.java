package tobi.study.user.STEP6.트랜잭션_속성_6_6;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
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
        UserDaoJdbc userDaoJdbc = new UserDaoJdbc();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }

    @Bean
    public DummyMailSender mailSender() {
        DummyMailSender mailSender = new DummyMailSender();
        return mailSender;
    }

    @Bean
    public UserService userService() {
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(userDao());
        userService.setMailSender(mailSender());
        return userService;
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
    public AspectJExpressionPointcut transactionPointcut() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String expression = "* *..*ServiceImpl.upgrade*(..))";
        pointcut.setExpression(expression);
        return pointcut;
    }
}