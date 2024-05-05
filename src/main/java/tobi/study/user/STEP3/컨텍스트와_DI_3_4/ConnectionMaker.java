package tobi.study.user.STEP3.컨텍스트와_DI_3_4;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
