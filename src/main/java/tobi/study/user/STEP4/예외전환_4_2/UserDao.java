package tobi.study.user.STEP4.예외전환_4_2;


import javax.sql.DataSource;
import java.util.List;

public interface UserDao {
    void add(User user);
    User get(String id);
    List<User> getAll();
    void deleteAll();
    int getCount();
    DataSource getDataSource();
}