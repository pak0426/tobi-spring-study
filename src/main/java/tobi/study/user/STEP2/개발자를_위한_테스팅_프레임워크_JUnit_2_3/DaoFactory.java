package tobi.study.user.STEP2.개발자를_위한_테스팅_프레임워크_JUnit_2_3;

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