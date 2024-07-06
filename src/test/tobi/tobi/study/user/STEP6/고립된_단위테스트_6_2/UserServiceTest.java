package tobi.study.user.STEP6.고립된_단위테스트_6_2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static tobi.study.user.STEP6.고립된_단위테스트_6_2.UserServiceImpl.MIN_LOGIN_COUNT_FOR_SILVER;
import static tobi.study.user.STEP6.고립된_단위테스트_6_2.UserServiceImpl.MIN_RECOMMEND_COUNT_FOR_GOLD;


@SpringBootTest
class UserServiceTest {

    private List<User> users;

    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private MailSender mailSender;

    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("a", "a@a.com", "aUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0),
                new User("b", "b@b.com", "bUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0), // 업그레이드 대상
                new User("c", "c@c.com", "cUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1),
                new User("d", "d@d.com", "dUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD), // 업그레이드 대상
                new User("e", "e@e.com", "eUser", "1234", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    public void upgradeLevels() {
        // 고립된 테스트에서는 테스트 대상 오브젝트를 직접 생성하면 된다.
        UserServiceImpl userServiceImpl = new UserServiceImpl();

        // 목 오브젝트로 만든 UserDao를 직접 DI 해준다.
        MockUserDao mockUserDao = new MockUserDao(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        // MockUserDao 로부터 업데이트 결과를 가져온다.
        List<User> updated = mockUserDao.getUpdated();

        // 업데이트 횟수와 정보를 확인한다.
        assertThat(updated.size()).isEqualTo(2);
        checkUserAndLevel(updated.get(0), "b", Level.SILVER);
        checkUserAndLevel(updated.get(1), "d", Level.GOLD);

        List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }

    // id와 level을 확인하는 결과 메서드
    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assertThat(updated.getId()).isEqualTo(expectedId);
        assertThat(updated.getLevel()).isEqualTo(expectedLevel);
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User updatedUser = userDao.get(user.getId());
        if (upgraded) {
            assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel().nextLevel());
        } else {
            assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel());
        }
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4);
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel()).isEqualTo(Level.GOLD);
        assertThat(userWithoutLevelRead.getLevel()).isEqualTo(Level.BASIC);
    }

    @Test
    public void upgradeAllOrNoting() {
        TestUserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(userDao);
        testUserService.setMailSender(mailSender);

        // 트랜잭션 기능을 분리한 UserServiceTx는 예외 발생용으로 수정할 필요가 없으니 그대로 사용한다.
        UserServiceTx txUserService = new UserServiceTx();
        txUserService.setTransactionManager(platformTransactionManager);
        txUserService.setUserService(testUserService);

        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        try {
            txUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevelUpgraded(users.get(1), false);
    }

    static class TestUserService extends UserServiceImpl {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {
    }

    static class MockMailSender implements MailSender {

        // UserService로부터 전송 요청을 받은 메일 주소를 저장해두고 이를 읽을 수 있게 한다.
        private List<String> requests = new ArrayList<String>();

        public List<String> getRequests() {
            return requests;
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage simpleMessage : simpleMessages) {
                requests.add(simpleMessage.getTo()[0]);
            }
        }

    }

    static class MockUserDao implements UserDao {

        private List<User> users;
        private List<User> updated = new ArrayList<>();

        public MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return this.updated;
        }

        @Override
        public List<User> getAll() {
            return this.users;
        }

        @Override
        public void update(User user) {
            updated.add(user);
        }

        @Override
        public void add(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException();
        }


        @Override
        public DataSource getDataSource() {
            throw new UnsupportedOperationException();
        }
    }
}