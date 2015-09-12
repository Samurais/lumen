package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Throwables;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by ceefour on 2/4/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = CypherQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Error", value = CypherQuery.class))
public class Error {
    public Error(String exceptionClass, String message, String stackTrace) {
        this.exceptionClass = exceptionClass;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public Error(Exception e) {
        this.exceptionClass = DefaultGroovyMethods.asType(e.getClass(), String.class);
        this.message = e.getMessage();
        this.stackTrace = Throwables.getStackTraceAsString(e);
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    private String exceptionClass;
    private String message;
    private String stackTrace;
}
