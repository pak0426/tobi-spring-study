package tobi.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "tobi.study.user.STEP6.고립된_단위테스트_6_2")
class TobiSpringStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TobiSpringStudyApplication.class, args);
    }
}
