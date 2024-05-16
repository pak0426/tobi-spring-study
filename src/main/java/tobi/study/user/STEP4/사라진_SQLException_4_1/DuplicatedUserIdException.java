package tobi.study.user.STEP4.사라진_SQLException_4_1;

class DuplicatedUserIdException extends RuntimeException {
    public DuplicatedUserIdException(Throwable cause) {
        super(cause);
    }
}
