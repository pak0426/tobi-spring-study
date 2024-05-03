package tobi.study.user.STEP3.변하는_것과_변하지_않는_것_3_2.템플릿메서드;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDaoGet extends UserDao {
    @Override
    protected PreparedStatement makeStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("select * from users where id = ?");
        return ps;
    }
}
