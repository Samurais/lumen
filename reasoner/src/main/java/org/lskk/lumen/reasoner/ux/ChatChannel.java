package org.lskk.lumen.reasoner.ux;

import org.apache.camel.ProducerTemplate;
import org.lskk.lumen.core.AvatarChannel;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.util.ToJson;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.util.AudioObjectResolver;
import org.lskk.lumen.reasoner.util.ImageObjectResolver;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Sends outgoing messages to {@code lumen.arkan.social.chat.outbox}.
 * Created by ceefour on 14/11/2015.
 */
@Service
public class ChatChannel extends Channel<Void> {

    @Inject
    private ToJson toJson;
    @Inject
    private ProducerTemplate producer;
    @Inject
    private ImageObjectResolver imageObjectResolver;
    @Inject
    private AudioObjectResolver audioObjectResolver;

    @Override
    public void express(String upAvatarId, CommunicateAction communicateAction, Void params) {
        try {
            final boolean replyHasImage = communicateAction.getImage() != null;
            final boolean replyHasAudio = communicateAction.getAudio() != null;
            if (!communicateAction.getObject().isEmpty() || replyHasImage || replyHasAudio) {
                if (replyHasImage) {
                    imageObjectResolver.resolve(communicateAction.getImage());
                    if (communicateAction.getImage().getContent() != null) {
                        communicateAction.getImage().setContentUrl("data:" + communicateAction.getImage().getContentType() + ";base64," +
                                Base64.getEncoder().encodeToString(communicateAction.getImage().getContent()) );
                        communicateAction.getImage().setUrl(null);
                        communicateAction.getImage().setContentSize((long) communicateAction.getImage().getContent().length);
                    }
                }
                if (replyHasAudio) {
                    audioObjectResolver.resolve(communicateAction.getAudio());
                    if (communicateAction.getAudio().getContent() != null) {
                        communicateAction.getAudio().setContentUrl("data:" + communicateAction.getAudio().getContentType() + ";base64," +
                                Base64.getEncoder().encodeToString(communicateAction.getAudio().getContent()) );
                        communicateAction.getAudio().setUrl(null);
                        communicateAction.getAudio().setContentSize((long) communicateAction.getAudio().getContent().length);
                    }
                }
                final String realAvatarId = Stream.of(communicateAction.getAvatarId(), upAvatarId)
                        .filter(Objects::nonNull).findFirst().orElse("nao1");
                final String topic = AvatarChannel.CHAT_OUTBOX.key(realAvatarId);
                log.info("Expressing via {}: {}", topic, communicateAction);
                producer.sendBody("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&skipQueueDeclare=true&autoDelete=false&routingKey=" + topic,
                        toJson.apply(communicateAction));
            }
        } catch (IOException e) {
            log.error("Cannot express " + communicateAction, e);
            throw new ReasonerException(e, "Cannot express %s", communicateAction);
        }
    }

}
