package tobi.study.user.STEP1.DAO의_확장_1_3.인터페이스;

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
