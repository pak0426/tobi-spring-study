package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

public class HelloUppercase implements Hello {
    /**
     * 위임할 타깃 오브젝트.
     * 여기서는 타깃 클래스의 오브젝트 인것은 알지만 다른 프록시를 추가할 수도 있으므로 인터페이스로 접근한다.
     */
    Hello hello;

    public HelloUppercase(Hello hello) {
        this.hello = hello;
    }

    @Override
    public String sayHello(String name) {
        //toUpperCase() -> 위임과 부가기능 적용
        return hello.sayHello(name).toUpperCase();
    }

    @Override
    public String sayHi(String name) {
        return hello.sayHi(name).toUpperCase();
    }

    @Override
    public String sayThankYou(String name) {
        return hello.sayThankYou(name).toUpperCase();
    }
}
