package tobi.study.user.STEP5.트랜잭션_서비스_추상화_5_2;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
