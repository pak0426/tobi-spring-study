package tobi.study.user.STEP3.템플릿과_콜백_3_5;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

interface StatementStrategy {
    PreparedStatement makePrepareStatement(Connection c) throws SQLException;
}
