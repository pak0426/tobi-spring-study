package tobi.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "tobi.study.user.STEP6.트랜잭션_속성_6_6")
class TobiSpringStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TobiSpringStudyApplication.class, args);
    }
}
