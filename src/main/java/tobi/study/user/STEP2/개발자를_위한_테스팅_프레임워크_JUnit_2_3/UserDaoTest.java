package tobi.study.user.STEP2.개발자를_위한_테스팅_프레임워크_JUnit_2_3;


import org.h2.jdbc.JdbcSQLNonTransientException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDaoTest {
    private UserDao userDao;
    private User user1;
    private User user2;
    private User user3;


    @BeforeEach
    void setup() {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        userDao = context.getBean("userDao", UserDao.class);

        user1 = new User("a", "aUser","aUser");
        user2 = new User("b", "bUser","bUser");
        user3 = new User("c", "cUser","cUser");
    }

    @Test
    public void addAndGet() throws SQLException, ClassNotFoundException {
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);

        userDao.add(user1);
        userDao.add(user2);
        assertEquals(userDao.getCount(), 2);

        User userGet1 = userDao.get("a");
        assertEquals(user1.getName(), userGet1.getName());
        assertEquals(user1.getPassword(), userGet1.getPassword());

        User userGet2 = userDao.get("b");
        assertEquals(user2.getName(), userGet2.getName());
        assertEquals(user2.getPassword(), userGet2.getPassword());
    }

    @Test
    public void count() throws SQLException, ClassNotFoundException {
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);

        userDao.add(user1);
        assertEquals(userDao.getCount(), 1);

        userDao.add(user2);
        assertEquals(userDao.getCount(), 2);

        userDao.add(user3);
        assertEquals(userDao.getCount(), 3);
    }

    @Test
    public void getUserFailure() throws SQLException, ClassNotFoundException {
        userDao.deleteAll();
        assertEquals(userDao.getCount(), 0);
        Assertions.assertThrows(JdbcSQLNonTransientException.class, () -> userDao.get("unknownId"));
    }
}