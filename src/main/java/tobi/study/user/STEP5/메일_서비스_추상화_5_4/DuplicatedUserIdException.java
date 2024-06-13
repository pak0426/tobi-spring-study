package tobi.study.user.STEP5.메일_서비스_추상화_5_4;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
