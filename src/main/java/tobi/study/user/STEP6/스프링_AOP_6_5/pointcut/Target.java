package tobi.study.user.STEP6.스프링_AOP_6_5.pointcut;

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
