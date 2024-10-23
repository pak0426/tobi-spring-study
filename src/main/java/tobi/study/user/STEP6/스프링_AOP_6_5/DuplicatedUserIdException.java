package tobi.study.user.STEP6.스프링_AOP_6_5;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
