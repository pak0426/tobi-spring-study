package tobi.study.user.STEP2.학습_테스트로_배우는_스프링_2_5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JUnitTest {
    static Set<JUnitTest> testObjects = new HashSet<>();

    @Test
    public void test1() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }

    @Test
    public void test2() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }

    @Test
    public void test3() {
        assertFalse(testObjects.contains(this));
        testObjects.add(this);
    }
}
