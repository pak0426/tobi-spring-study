package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

import java.util.HashSet;
import java.util.Set;

public class HelloTarget implements Hello {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }

    @Override
    public String sayThankYou(String name) {
        return "Thank you " + name;
    }
}
