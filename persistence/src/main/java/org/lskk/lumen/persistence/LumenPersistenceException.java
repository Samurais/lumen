package org.lskk.lumen.persistence;

/**
 * Created by ceefour on 15/02/2016.
 */
public class LumenPersistenceException extends RuntimeException {
    public LumenPersistenceException() {
        super();
    }

    public LumenPersistenceException(String message) {
        super(message);
    }

    public LumenPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LumenPersistenceException(Throwable cause) {
        super(cause);
    }

    protected LumenPersistenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    protected LumenPersistenceException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }
}
