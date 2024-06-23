package tobi.study.user.STEP6.트랜잭션_코드의_분리_6_1;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class DummyMailSender implements MailSender {
    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {

    }
}