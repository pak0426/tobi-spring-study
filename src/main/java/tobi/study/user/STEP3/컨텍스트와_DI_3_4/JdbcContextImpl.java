package tobi.study.user.STEP3.컨텍스트와_DI_3_4;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcContextImpl implements JdbcContextInterface {
    private DataSource dataSource;

    public JdbcContextImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();
            ps = strategy.makePrepareStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) { try { ps.close(); } catch (SQLException e) {} }
            if (c != null) { try { c.close(); } catch (SQLException e) { } }
        }
    }
}
