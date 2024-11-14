package tobi.study.user.STEP6.트랜잭션_속성_6_6.pointcut;

public class Target implements TargetInterface {
    @Override
    public void hello() {}

    @Override
    public void hello(String a) {}

    @Override
    public int minus(int a, int b) throws RuntimeException { return 0; }

    @Override
    public int plus(int a, int b) { return 0; }

    @Override
    public void method() {}
}
