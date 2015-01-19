package id.ac.itb.lumen.social

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import org.apache.camel.Body
import org.springframework.stereotype.Service

import java.util.function.Function

/**
 * Created by ceefour on 19/01/15.
 */
@Service
class ToJson implements Function<Object, String> {

    protected ObjectMapper om

    ToJson() {
        om = new ObjectMapper()
        om.registerModule(new JodaModule())
        om.registerModule(new GuavaModule())
        om.enable(SerializationFeature.INDENT_OUTPUT)
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Override
    String apply(@Body Object o) {
        return o != null ? om.writeValueAsString(o) : null
    }
}
