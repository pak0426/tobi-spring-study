package tobi.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3")
class TobiSpringStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TobiSpringStudyApplication.class, args);
    }
}
