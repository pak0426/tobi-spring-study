package tobi.study.user.STEP6.트랜잭션_속성_6_6;


import javax.sql.DataSource;
import java.util.List;

public interface UserDao {
    void add(User user);
    User get(String id);
    List<User> getAll();
    void deleteAll();
    int getCount();
    void update(User user);
    DataSource getDataSource();
}