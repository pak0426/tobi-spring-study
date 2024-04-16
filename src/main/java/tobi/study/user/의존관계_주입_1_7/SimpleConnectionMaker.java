package tobi.study.user.의존관계_주입_1_7;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class SimpleConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost/~/tobiSpringStudy", "sa", ""
        );
        return c;
    }
}
