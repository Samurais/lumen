package org.lskk.lumen.reasoner.visual;

import com.github.ooxi.jdatauri.DataUri;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.AvatarChannel;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.core.util.ToJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

/**
 * Created by ceefour on 12/11/2015.
 */
@Component
public class VisualCaptureRouter extends RouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(VisualCaptureRouter.class);

    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
//    @Inject
//    private KieSession kieSession;

    private String cameraMainType;
    private byte[] cameraMain;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        final String avatarId = "nao1";
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.CAMERA_MAIN.key(avatarId) + "&routingKey=" + AvatarChannel.CAMERA_MAIN.key(avatarId))
                .process(exchange -> {
                    final ImageObject imageObject = toJson.getMapper().readValue(
                            exchange.getIn().getBody(byte[].class), ImageObject.class);
                    final DataUri dataUri = DataUri.parse(imageObject.getContentUrl(), StandardCharsets.UTF_8);
                    cameraMain = dataUri.getData();
                    cameraMainType = dataUri.getMime();
                    log.debug("Got nao1.camera.main {} {} bytes", cameraMainType, cameraMain.length);
                });
    }

    public String getCameraMainType() {
        return cameraMainType;
    }

    public byte[] getCameraMain() {
        return cameraMain;
    }
}
