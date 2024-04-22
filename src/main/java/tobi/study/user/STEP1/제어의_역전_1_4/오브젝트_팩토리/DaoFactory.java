package tobi.study.user.STEP1.제어의_역전_1_4.오브젝트_팩토리;

class DaoFactory {
    public UserDao userDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao userDao = new UserDao(connectionMaker);
        return userDao;
    }
}