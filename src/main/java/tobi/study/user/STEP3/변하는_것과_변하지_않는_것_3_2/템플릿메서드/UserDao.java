package tobi.study.user.STEP3.변하는_것과_변하지_않는_것_3_2.템플릿메서드;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class UserDao {
    private User user;

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    abstract protected PreparedStatement makeStatement(Connection c) throws SQLException;
}
