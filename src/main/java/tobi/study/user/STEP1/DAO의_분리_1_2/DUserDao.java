package tobi.study.user.STEP1.DAO의_분리_1_2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class DUserDao extends UserDao {
    @Override
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        // D사 DB Connection 생성 코드
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
        );
        return c;
    }
}
