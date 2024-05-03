package tobi.study.user.STEP3.변하는_것과_변하지_않는_것_3_2.전략;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class DeleteAllStatement implements StatementStrategy {
    @Override
    public PreparedStatement makePrepareStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
