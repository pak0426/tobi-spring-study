package tobi.study.user.싱글톤_레지스트리와_오브젝트_스코프_1_6;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
