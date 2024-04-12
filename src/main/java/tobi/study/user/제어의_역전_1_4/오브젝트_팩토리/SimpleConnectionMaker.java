package tobi.study.user.제어의_역전_1_4.오브젝트_팩토리;

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
