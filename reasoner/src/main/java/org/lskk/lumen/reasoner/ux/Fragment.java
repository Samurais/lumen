package org.lskk.lumen.reasoner.ux;

import com.google.common.base.Preconditions;
import org.apache.tomcat.jni.Local;
import org.lskk.lumen.reasoner.ReasonerException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;
import java.net.URL;
import java.util.Locale;

/**
 * Created by ceefour on 21/02/2016.
 */
public class Fragment extends NuiComponent {

    protected Locale locale;
    protected URL markupUrl;
    protected InteractionElement interactionMarkup;
    protected FragmentElement fragmentMarkup;

    public Fragment(String id) {
        super(id);
    }

    public void prepareRender(Locale locale, URL markupUrl) {
        this.locale = locale;
        this.markupUrl = markupUrl;
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(InteractionElement.class, FragmentElement.class,
                    SpanElement.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            interactionMarkup = (InteractionElement) unmarshaller.unmarshal(markupUrl);
            try {
                fragmentMarkup = interactionMarkup.getFragments().stream().filter(it -> getId().equals(it.getId()))
                    .findAny().get();
            } catch (Exception e) {
                throw new ReasonerException(e, "Cannot find <nui:fragment> '%s' from %s", getId(), markupUrl);
            }
        } catch (Exception e) {
            throw new ReasonerException(e, "Cannot load markup for fragment '%s' from %s", getId(), markupUrl);
        }
    }

    @Override
    public String renderSsml() {
        String ssml = "";
        for (final Serializable childMarkup : fragmentMarkup.getContents()) {
            if (childMarkup instanceof SpanElement) {
                final String spanId = Preconditions.checkNotNull(((SpanElement) childMarkup).getId(),
                        "<span> with missing 'id' attribute in fragment '%s': %s", getId(), fragmentMarkup);
                final NuiComponent child = children.stream().filter(it -> spanId.equals(it.getId())).findAny().get();
                ssml += child.renderSsml();
            } else if (childMarkup instanceof String) {
                ssml += childMarkup;
            } else {
                throw new ReasonerException(String.format("Unhandled markup child of Fragment '%s' in %s: %s",
                        getId(), markupUrl, childMarkup));
            }
        }
        ssml = ssml.trim();
        return ssml;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
