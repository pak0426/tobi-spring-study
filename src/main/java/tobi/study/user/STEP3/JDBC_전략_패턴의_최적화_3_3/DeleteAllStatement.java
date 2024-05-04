package tobi.study.user.STEP3.JDBC_전략_패턴의_최적화_3_3;

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
