package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.base.Throwables
import groovy.transform.CompileStatic

/**
 * Created by ceefour on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=CypherQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name="Error", value=CypherQuery.class))
class Error {
    String exceptionClass
    String message
    String stackTrace

    Error(String exceptionClass, String message, String stackTrace) {
        this.exceptionClass = exceptionClass
        this.message = message
        this.stackTrace = stackTrace
    }

    Error(Exception e) {
        this.exceptionClass = e.class as String
        this.message = e.message
        this.stackTrace = Throwables.getStackTraceAsString(e)
    }

}
