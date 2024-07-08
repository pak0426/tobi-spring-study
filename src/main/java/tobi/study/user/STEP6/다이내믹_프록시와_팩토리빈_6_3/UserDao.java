package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;


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