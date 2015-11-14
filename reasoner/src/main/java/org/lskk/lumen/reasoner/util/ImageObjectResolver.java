package org.lskk.lumen.reasoner.util;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
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
public class ImageObjectResolver {

    private static final Logger log = LoggerFactory.getLogger(ImageObjectResolver.class);
    private static final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(ImageObjectResolver.class.getClassLoader());

    @Inject
    private VisualCaptureRouter visualCaptureRouter;

    public void resolve(ImageObject imageObject) throws IOException {
        Preconditions.checkArgument(imageObject.getUrl() != null,
                "CommunicateAction.ImageObject.url is required");
//                    Preconditions.checkArgument(url.startsWith("file:") || url.startsWith("classpath:"),
//                            "CommunicateAction.ImageObject.url only supports file: and classpath: schemes");

        if (imageObject.getUrl().startsWith("lumen:")) {
            log.debug("Resolving image {} from visualCaptureRouter", imageObject.getUrl());
            imageObject.setContent(visualCaptureRouter.getCameraMain());
            imageObject.setContentType(visualCaptureRouter.getCameraMainType());
        } else {
            final Resource res = resourceResolver.getResource(imageObject.getUrl());
            Preconditions.checkState(res.exists() && res.isReadable(), "%s does not exist or is not readable", res);
            final byte[] media = IOUtils.toByteArray(res.getURL());
            imageObject.setContent(media);
            if (imageObject.getContentType() == null) {
                imageObject.setContentType("image/jpeg");
            }
            log.debug("Resolved image {} as {} bytes {}",
                    imageObject.getUrl(), imageObject.getContent().length, imageObject.getContentType());
        }
    }


}
