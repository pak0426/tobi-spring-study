package tobi.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4")
class TobiSpringStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TobiSpringStudyApplication.class, args);
    }
}
