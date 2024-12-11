package tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8;


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