package tobi.study.user.STEP1.DAO의_확장_1_3.인터페이스;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
