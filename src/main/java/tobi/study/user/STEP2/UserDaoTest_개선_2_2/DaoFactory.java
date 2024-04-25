package tobi.study.user.STEP2.UserDaoTest_개선_2_2;

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