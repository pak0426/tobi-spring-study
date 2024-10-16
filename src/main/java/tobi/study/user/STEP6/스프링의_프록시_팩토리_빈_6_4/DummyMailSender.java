package tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class DummyMailSender implements MailSender {
    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {

    }
}