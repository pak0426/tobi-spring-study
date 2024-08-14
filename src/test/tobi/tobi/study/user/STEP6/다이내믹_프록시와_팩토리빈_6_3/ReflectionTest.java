package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionTest {
    @Test
    public void invokeMethod() throws Exception {
        String name = "Spring";

        // length()
        assertThat(name.length()).isEqualTo(6);

        Method lengthMethod = String.class.getMethod("length");
        assertThat(lengthMethod.invoke(name)).isEqualTo(6);

        // charAt()
        assertThat(name.charAt(0)).isEqualTo('S');

        Method charAtMethod = String.class.getMethod("charAt", int.class);
        assertThat((Character) charAtMethod.invoke(name, 0)).isEqualTo('S');
    }
}