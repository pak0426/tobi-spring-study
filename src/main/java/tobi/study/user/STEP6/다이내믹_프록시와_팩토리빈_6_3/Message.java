package tobi.study.user.STEP6.다이내믹_프록시와_팩토리빈_6_3;

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
