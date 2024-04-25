package tobi.study.user.STEP2.개발자를_위한_테스팅_프레임워크_JUnit_2_3;

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

