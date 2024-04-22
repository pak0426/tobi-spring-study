package tobi.study.user.STEP1.XML을_이용한_설정_1_8;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;

class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
//        ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao userDao = context.getBean("userDao", UserDao.class);

        DaoFactory daoFactory = new DaoFactory();
        UserDao userDao1 = daoFactory.getUserDao();
        UserDao userDao2 = daoFactory.getUserDao();

        System.out.println("userDao1 = " + userDao1);
        System.out.println("userDao2 = " + userDao2);

        UserDao userDao3 = context.getBean("userDao", UserDao.class);
        UserDao userDao4 = context.getBean("userDao", UserDao.class);

        System.out.println("userDao3 = " + userDao3);
        System.out.println("userDao4 = " + userDao4);

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        userDao.add(user);
        System.out.println(user.getId() + " 등록 성공");

        User user2 = userDao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }
}