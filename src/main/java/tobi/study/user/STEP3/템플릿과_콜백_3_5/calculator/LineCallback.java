package tobi.study.user.STEP3.템플릿과_콜백_3_5.calculator;

import java.io.BufferedReader;
import java.io.IOException;

public interface LineCallback<T> {
    T doSomethingWithReader(String line, T value) throws IOException;
}
