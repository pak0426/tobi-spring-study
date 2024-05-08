package tobi.study.user.STEP3.템플릿과_콜백_3_5;

import java.sql.SQLException;

public interface JdbcContextInterface {
    public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException;

    void executeSql(String query) throws SQLException;
}
