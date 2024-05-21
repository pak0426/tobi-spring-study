package tobi.study.user.STEP5.사용자_레벨_관리기능추가_5_1;


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