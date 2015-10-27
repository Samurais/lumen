package org.lskk.lumen.reasoner;

/**
 * Created by ceefour on 27/10/2015.
 */
public class ReasonerException extends RuntimeException {

    public ReasonerException() {
    }

    public ReasonerException(String message) {
        super(message);
    }

    public ReasonerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReasonerException(Throwable cause) {
        super(cause);
    }

    public ReasonerException(Throwable cause, String format, Object... params) {
        super(String.format(format, params), cause);
    }

    public ReasonerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
