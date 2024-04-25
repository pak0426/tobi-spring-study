package tobi.study.user.STEP2.개발자를_위한_테스팅_프레임워크_JUnit_2_3;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException;
}
