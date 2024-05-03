package tobi.study.user.STEP3.변하는_것과_변하지_않는_것_3_2;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
