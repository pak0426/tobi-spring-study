package tobi.study.user.STEP3.JDBC_전략_패턴의_최적화_3_3;

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

