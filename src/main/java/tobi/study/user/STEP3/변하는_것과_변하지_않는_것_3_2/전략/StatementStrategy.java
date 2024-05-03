package tobi.study.user.STEP3.변하는_것과_변하지_않는_것_3_2.전략;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

interface StatementStrategy {
    PreparedStatement makePrepareStatement(Connection c) throws SQLException;
}
