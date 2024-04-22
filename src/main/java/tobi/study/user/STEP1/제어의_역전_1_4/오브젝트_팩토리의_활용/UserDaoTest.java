package tobi.study.user.STEP1.제어의_역전_1_4.오브젝트_팩토리의_활용;

import java.sql.SQLException;

class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        UserDao userDao = new DaoFactory().userDao();

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