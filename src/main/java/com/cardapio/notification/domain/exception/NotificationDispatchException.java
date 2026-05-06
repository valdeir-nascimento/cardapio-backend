package com.cardapio.notification.domain.exception;

public class NotificationDispatchException extends RuntimeException {

    private final String code;

    public NotificationDispatchException(String code, String message) {
        super(message);
        this.code = code;
    }

    public NotificationDispatchException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
