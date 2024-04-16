package tobi.study.user.의존관계_주입_1_7;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
