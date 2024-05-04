package tobi.study.user.STEP3.JDBC_전략_패턴의_최적화_3_3;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
