package com.hospital.common;

public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final Object extras;

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.extras = null;
    }

    public DomainException(ErrorCode code, String message, Object extras) {
        super(message);
        this.code = code;
        this.extras = extras;
    }

    public ErrorCode getCode() { return code; }
    public Object getExtras() { return extras; }
}
