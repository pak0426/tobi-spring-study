package tobi.study.user.STEP2.UserDaoTest_개선_2_2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class DConnectionMaker implements ConnectionMaker {
    @Override
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
        );
        return c;
    }
}

