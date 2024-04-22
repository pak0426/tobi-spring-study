package tobi.study.user.STEP1.싱글톤_레지스트리와_오브젝트_스코프_1_6;

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
