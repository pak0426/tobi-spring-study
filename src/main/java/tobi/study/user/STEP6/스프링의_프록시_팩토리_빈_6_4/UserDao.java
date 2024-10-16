package tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4;


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