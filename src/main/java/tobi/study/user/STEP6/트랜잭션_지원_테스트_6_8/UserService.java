package tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8;

import java.util.List;

public interface UserService {
    void add(User user);
    void deleteAll();
    void update(User user);
    void upgradeLevels();
    User get(String id);
    List<User> getAll();
}
