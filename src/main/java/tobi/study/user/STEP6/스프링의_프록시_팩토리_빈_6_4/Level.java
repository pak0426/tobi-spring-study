package tobi.study.user.STEP6.스프링의_프록시_팩토리_빈_6_4;

public enum Level {
    BASIC(1), SILVER(2), GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public Level nextLevel() {
        switch (this) {
            case BASIC: return SILVER;
            case SILVER: return GOLD;
            case GOLD: return null;
            default: throw new IllegalArgumentException("알 수 없는 등급");
        }
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1: return BASIC;
            case 2: return SILVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unexpected value: " + value);
        }
    }
}
