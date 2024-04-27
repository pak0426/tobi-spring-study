package tobi.study.user.STEP2.스프링_테스트_적용_2_4;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DaoFactory {
    @Bean
    public UserDao userDao() {
        return new UserDao(connectionMaker());
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }

    public UserDao getUserDao() {
        return new UserDao(connectionMaker());
    }
}