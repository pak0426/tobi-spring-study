package tobi.study.user.STEP6.스프링_AOP_6_5;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest {

    private UserDao userDao;
    private User user1;
    private User user2;
    private User user3;


    @BeforeEach
    void setup() {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        userDao = context.getBean("userDao", UserDao.class);

        user1 = new User("a", "a@a.com", "aUser","aUser", Level.BASIC, 1, 0);
        user2 = new User("b", "b@b.com", "bUser","bUser", Level.SILVER, 55, 10);
        user3 = new User("c", "c@c.com", "cUser","cUser", Level.GOLD, 100, 4);
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
        assertThrows(EmptyResultDataAccessException.class, () -> userDao.get("unknownId"));
    }

    @Test
    public void getAll() throws SQLException {
        userDao.deleteAll();

        List<User> users0 = userDao.getAll();
        assertEquals(users0.size(), 0);

        userDao.add(user1);
        List<User> users1 = userDao.getAll();
        checkSameUser(user1, users1.get(0));

        userDao.add(user2);
        List<User> users2 = userDao.getAll();
        checkSameUser(user1, users2.get(0));
        checkSameUser(user2, users2.get(1));

        userDao.add(user3);
        List<User> users3 = userDao.getAll();
        checkSameUser(user1, users3.get(0));
        checkSameUser(user2, users3.get(1));
        checkSameUser(user3, users3.get(2));
    }

    private void checkSameUser(User user1, User user2) {
        assertEquals(user1.getId(), user2.getId());
        assertEquals(user1.getName(), user2.getName());
        assertEquals(user1.getPassword(), user2.getPassword());
        assertEquals(user1.getLevel(), user2.getLevel());
        assertEquals(user1.getLogin(), user2.getLogin());
        assertEquals(user1.getRecommend(), user2.getRecommend());
    }

    @Test
    public void duplicateKey() {
        userDao.deleteAll();
        userDao.add(user1);
        assertThrows(DataAccessException.class, () -> userDao.add(user1));
    }

    @Test
    public void sqlExceptionTranslate() {
        userDao.deleteAll();

        try {
            userDao.add(user1);
            userDao.add(user1);
        }
        catch (DuplicateKeyException ex) {
            SQLException sqlEx = (SQLException) ex.getRootCause();
            SQLExceptionTranslator set = new SQLErrorCodeSQLExceptionTranslator(userDao.getDataSource());

            DataAccessException translate = set.translate(null, null, sqlEx);
            assertTrue(set.translate(null, null, sqlEx) instanceof DuplicateKeyException);
        }
    }

    @Test
    public void update() {
        userDao.deleteAll();

        userDao.add(user1); // 수정할 사용자
        userDao.add(user2); // 수정하지 않을 사용자

        user1.setName("현민박");
        user1.setPassword("goodDay");
        user1.setLevel(Level.GOLD);
        user1.setLogin(100);
        user1.setRecommend(999);

        userDao.update(user1);

        User updatedUser1 = userDao.get(user1.getId());
        checkSameUser(user1, updatedUser1);
        User updatedUser2 = userDao.get(user2.getId());
        checkSameUser(user2, updatedUser2);
    }
}