package tobi.study.user.DAO의_확장_1_3.의존관계주입;

import java.sql.SQLException;

public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao dao = new UserDao(connectionMaker);

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        dao.add(user);
        System.out.println(user.getId() + " 등록 성공");

        User user2 = dao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }
}
