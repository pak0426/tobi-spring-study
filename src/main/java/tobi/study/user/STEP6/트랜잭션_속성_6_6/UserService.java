package tobi.study.user.STEP6.트랜잭션_속성_6_6;

import java.util.List;

public interface UserService {
    void add(User user);
    User get(String id);
    List<User> getAll();
    void deleteAll();
    void update(User user);
    void upgradeLevels();
}
