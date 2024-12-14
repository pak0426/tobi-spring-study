package tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8.UserServiceImpl.MIN_LOGIN_COUNT_FOR_SILVER;
import static tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8.UserServiceImpl.MIN_RECOMMEND_COUNT_FOR_GOLD;

@SpringBootTest
class UserServiceTest {

    private List<User> users;

    @Autowired
    private UserService userService;

    @Autowired
    private UserService testUserService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @BeforeEach
    public void setUp() {


        users = Arrays.asList(
                new User("a", "a@a.com", "aUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0),
                new User("b", "b@b.com", "bUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0),
                new User("c", "c@c.com", "cUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1),
                new User("d", "d@d.com", "dUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD),
                new User("e", "e@e.com", "eUser", "1234", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    @DirtiesContext // 컨텍스트의 DI 설정을 변경하라는 테스트라는 것을 알려준다.
    public void upgradeLevels() throws Exception {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        // 메일 발송 결과를 테스트할 수 있도록 목 오브젝트를 만들어 userService 의 의존 오브젝트로 주입해준다.
        MockMailSender mockMailSender = new MockMailSender();
//        userServiceImpl.setMailSender(mockMailSender);


        // 업그레이드 테스트. 메일 발송이 일어나면 MockMailSender 오브젝트의 리스트에 그 결과가 저장된다.
        userService.upgradeLevels();
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);


        // 목 오브젝트에 저장된 메일 수신자 목록을 가져와 업그레이드 대상과 일치하는지 확인
        List<String> requests = mockMailSender.getRequests();
//        assertThat(requests.size()).isEqualTo(2);
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
    @DirtiesContext
    public void upgradeAllOrNoting() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        try {
            testUserService.upgradeLevels();
            fail("RuntimeException expected");
        } catch (RuntimeException e) {

        }

        checkLevelUpgraded(users.get(1), false);
    }

    @Test
    public void transactionSync() {
        // 트랜잭션 정의는 기본 값을 사용한다.
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

        // 트랜잭션 매니저에게 트랜잭션을 요청한다. 기존에 시작된 트랜잭션이 없으니 새로운 트랜잭션을 시작시키고 트랜잭션 정보를 돌려준다.
        // 동시에 만들어진 트랜잭션을 다른 곳에서도 사용할 수 있도록 동기화한다.
        TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);

        // 앞에서 만들어진 트랜잭션에 모두 참여한다.
        userService.deleteAll();

        userService.add(users.get(0));
        userService.add(users.get(1));
        //

        transactionManager.commit(txStatus); // 앞에서 시작한 트랜잭션을 커밋한다.
    }

    static class TestUserService extends UserServiceImpl {
        private String id = "d";

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
}