package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class DummyMailSender implements MailSender {
    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {

    }
}