package org.lskk.lumen.reasoner.util;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.visual.VisualCaptureRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by ceefour on 14/11/2015.
 */
@Service
public class AudioObjectResolver {

    private static final Logger log = LoggerFactory.getLogger(AudioObjectResolver.class);
    private static final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(AudioObjectResolver.class.getClassLoader());

    public void resolve(AudioObject audioObject) throws IOException {
        Preconditions.checkArgument(audioObject.getUrl() != null,
                "CommunicateAction.AudioObject.url is required for %s", audioObject);
//                    Preconditions.checkArgument(url.startsWith("file:") || url.startsWith("classpath:"),
//                            "CommunicateAction.ImageObject.url only supports file: and classpath: schemes");

        log.debug("Resolving audio {} ...", audioObject.getUrl());
        final Resource res = resourceResolver.getResource(audioObject.getUrl());
        Preconditions.checkState(res.exists() && res.isReadable(), "%s does not exist or is not readable", res);
        final byte[] media = IOUtils.toByteArray(res.getURL());
        audioObject.setContent(media);
        if (audioObject.getContentType() == null) {
            if (StringUtils.endsWithIgnoreCase(audioObject.getUrl(), ".mp3")) {
                audioObject.setContentType("audio/mpeg");
            } else if (StringUtils.endsWithIgnoreCase(audioObject.getUrl(), ".wav")) {
                audioObject.setContentType("audio/wav");
            } else if (StringUtils.endsWithIgnoreCase(audioObject.getUrl(), ".flac")) {
                audioObject.setContentType("audio/flac");
            } else {
                // default is ogg
                audioObject.setContentType("audio/ogg");
            }
        }
        log.debug("Resolved audio {} as {} bytes {}",
                audioObject.getUrl(), audioObject.getContent().length, audioObject.getContentType());
    }

}
