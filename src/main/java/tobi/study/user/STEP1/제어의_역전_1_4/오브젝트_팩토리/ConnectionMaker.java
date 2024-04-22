package tobi.study.user.STEP1.제어의_역전_1_4.오브젝트_팩토리;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
