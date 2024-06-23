package tobi.study.user.STEP6.트랜잭션_코드의_분리_6_1;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
