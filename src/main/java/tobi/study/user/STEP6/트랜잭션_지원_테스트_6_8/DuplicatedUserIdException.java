package tobi.study.user.STEP6.트랜잭션_지원_테스트_6_8;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
