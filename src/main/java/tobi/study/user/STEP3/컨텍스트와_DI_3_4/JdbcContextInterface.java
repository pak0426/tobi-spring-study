package tobi.study.user.STEP3.컨텍스트와_DI_3_4;

import java.sql.SQLException;

public interface JdbcContextInterface {
    public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException;
}
