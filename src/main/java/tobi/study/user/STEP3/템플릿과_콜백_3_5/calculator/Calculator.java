package tobi.study.user.STEP3.템플릿과_콜백_3_5.calculator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Calculator {
    public String concatenate(String filePath) throws IOException {
        LineCallback<String> concatenateCallback = new LineCallback<String>() {
            @Override
            public String doSomethingWithReader(String line, String value) throws IOException {
                return value + line;
            }
        };
        return lineReadTemplate(filePath, concatenateCallback, "");
    }

    public int calcSum(String filePath) throws IOException {
        LineCallback<Integer> sumCallBack = new LineCallback<Integer>() {
            @Override
            public Integer doSomethingWithReader(String line, Integer value) throws IOException {
                return value + Integer.parseInt(line);
            }
        };
        return lineReadTemplate(filePath, sumCallBack, 0);
    }

    public int calcMultiply(String filePath) throws IOException {
        LineCallback<Integer> multiplyCallBack = new LineCallback<Integer>() {
            @Override
            public Integer doSomethingWithReader(String line, Integer value) throws IOException {
                return value * Integer.parseInt(line);
            }
        };
        return lineReadTemplate(filePath, multiplyCallBack, 1);
    }

    public <T> T lineReadTemplate(String filePath, LineCallback<T> callback, T initVal) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));
            T res = initVal;
            String line = null;

            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithReader(line, res);
            }
            return res;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException e) { System.out.println(e.getMessage()); }
            }
        }
    }

    public int fileReadTemplate(String filePath, BufferedReaderCallback callback) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));
            int ret = callback.doSomethingWithReader(br);
            return ret;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try { br.close(); }
                catch (IOException e) { System.out.println(e.getMessage()); }
            }
        }
    }
}
