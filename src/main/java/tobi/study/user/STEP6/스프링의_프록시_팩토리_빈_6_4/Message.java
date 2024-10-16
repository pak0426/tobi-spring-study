package tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4;

public class Message {
    String text;

    private Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static Message newMessage(String text) {
        return new Message(text);
    }
}
