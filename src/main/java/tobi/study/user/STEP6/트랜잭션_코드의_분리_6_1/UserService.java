package tobi.study.user.STEP6.트랜잭션_코드의_분리_6_1;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private UserDao userDao;
    private PlatformTransactionManager transactionManager;
    private MailSender mailSender;

    public static final int MIN_LOGIN_COUNT_FOR_SILVER = 50;
    public static final int MIN_RECOMMEND_COUNT_FOR_GOLD = 30;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void upgradeLevels() throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // 트랜잭션 안에서 진행되는 작업
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }

    public void add(User user) {
        if (user.getLevel() == null) {
            user.setLevel(Level.BASIC);
        }
        userDao.add(user);
    }

    private boolean canUpgradeLevel(User user) {
        Level level = user.getLevel();
        switch (level) {
            case BASIC: return (user.getLogin() >= MIN_LOGIN_COUNT_FOR_SILVER);
            case SILVER: return (user.getRecommend() >= MIN_RECOMMEND_COUNT_FOR_GOLD);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown level: " + level);
        }
    }

    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeEmail(user);
    }

    private void sendUpgradeEmail(User user) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("mail.server.com");

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("useradmin@ksug.org");
        mailMessage.setSubject("upgrade 안내");
        mailMessage.setText("사용자님의 등급이 " + user.getLevel().name());

        this.mailSender.send(mailMessage);
    }
}
