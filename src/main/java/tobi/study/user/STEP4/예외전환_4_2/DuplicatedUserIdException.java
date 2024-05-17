package tobi.study.user.STEP4.예외전환_4_2;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
