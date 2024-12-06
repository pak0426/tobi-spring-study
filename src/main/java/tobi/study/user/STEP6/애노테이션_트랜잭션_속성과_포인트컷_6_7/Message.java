package tobi.study.user.STEP6.애노테이션_트랜잭션_속성과_포인트컷_6_7;

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
