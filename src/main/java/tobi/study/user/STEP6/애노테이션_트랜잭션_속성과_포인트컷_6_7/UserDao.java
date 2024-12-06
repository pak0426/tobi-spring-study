package tobi.study.user.STEP6.애노테이션_트랜잭션_속성과_포인트컷_6_7;


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