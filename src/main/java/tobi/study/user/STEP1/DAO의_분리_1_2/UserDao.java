package tobi.study.user.STEP1.DAO의_분리_1_2;

import java.sql.*;

abstract class UserDao {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        UserDao dao = new UserDao() {
            @Override
            public Connection getConnection() throws ClassNotFoundException, SQLException {
                return null;
            }
        };

        User user = new User();
        user.setId("hmmini");
        user.setName("박현민");
        user.setPassword("1234");
        dao.add(user);
        System.out.println(user.getId() + "등록 성공");

        User user2 = dao.get("hmmini");
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }

    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();

        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) values (?, ?, ?)"
        );
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();

        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?"
        );
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }

    public abstract Connection getConnection() throws ClassNotFoundException, SQLException;
}
