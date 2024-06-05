package tobi.study.user.STEP5.사용자_레벨_관리기능추가_5_1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static tobi.study.user.STEP5.사용자_레벨_관리기능추가_5_1.UserService.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    List<User> users;
    @Autowired
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("a", "aUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0),
                new User("b", "bUser", "1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0),
                new User("c", "cUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1),
                new User("d", "dUser", "1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD),
                new User("e", "eUser", "1234", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    public void upgradeLevels() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(users.get(0), false);
        checkLevel(users.get(1), true);
        checkLevel(users.get(2), false);
        checkLevel(users.get(3), true);
        checkLevel(users.get(4), false);
    }

    private void checkLevel(User user, boolean upgraded) {
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
}