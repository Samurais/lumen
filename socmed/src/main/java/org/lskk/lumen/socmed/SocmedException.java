package org.lskk.lumen.socmed;

/**
 * Created by ceefour on 29/10/2015.
 */
public class SocmedException extends RuntimeException {

    public SocmedException() {
    }

    public SocmedException(String message) {
        super(message);
    }

    public SocmedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocmedException(Throwable cause) {
        super(cause);
    }

    public SocmedException(Throwable cause, String format, Object... params) {
        super(String.format(format, params), cause);
    }

    public SocmedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
