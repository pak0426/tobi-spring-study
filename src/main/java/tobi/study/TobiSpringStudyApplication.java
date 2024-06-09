package tobi.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "tobi.study.user.STEP5.트랜잭션_서비스_추상화_5_2")
class TobiSpringStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TobiSpringStudyApplication.class, args);
    }
}
