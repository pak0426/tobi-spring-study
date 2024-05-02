package tobi.study.user.STEP3.다시_보는_초난감_DAO_3_1;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
