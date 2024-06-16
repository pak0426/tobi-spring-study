package tobi.study.user.STEP5.메일_서비스_추상화_5_4;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class DummyMailSender implements MailSender {
    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {

    }
}