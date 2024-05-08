package tobi.study.user.STEP3.템플릿과_콜백_3_5.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CalcSumTest {
    private Calculator calculator;

    private final String filePath = "/Users/hmmini/mini/develop/project/backend/tobi-spring-study/src/main/java/tobi/study/user/STEP3/템플릿과_콜백_3_5/calculator/numbers.txt";

    @BeforeEach
    void setUp() {
        this.calculator = new Calculator();
    }

    @Test
    public void sumOfNumbers() throws IOException {
        assertEquals(10, calculator.calcSum(filePath));
    }

    @Test
    public void multiplyOfNumbers() throws IOException {
        assertEquals(24, calculator.calcMultiply(filePath));
    }

    @Test
    public void concatenateStrings() throws IOException {
        assertEquals(calculator.concatenate(filePath), "1234");
    }
}
