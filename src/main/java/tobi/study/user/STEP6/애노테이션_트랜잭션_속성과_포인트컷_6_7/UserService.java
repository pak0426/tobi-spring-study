package tobi.study.user.STEP6.애노테이션_트랜잭션_속성과_포인트컷_6_7;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {
    void add(User user);
    void deleteAll();
    void update(User user);
    void upgradeLevels();
    User get(String id);
    List<User> getAll();
}
