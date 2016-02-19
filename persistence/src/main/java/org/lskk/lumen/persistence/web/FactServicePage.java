package org.lskk.lumen.persistence.web;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.ladda.LaddaAjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.persistence.service.MatchingThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Frontend for {@link org.lskk.lumen.persistence.service.FactService}.
 */
@MountPath("fact-service")
public class FactServicePage extends PubLayout {

    public static final List<Locale> LANGUAGE_CHOICES = ImmutableList.of(Locale.US, Locale.forLanguageTag("id-ID"));
    private static Logger log = LoggerFactory.getLogger(FactServicePage.class);

    @Inject
    private FactService factService;

    public static class FactServiceRequest implements Serializable {
        private FactServiceOperation op = null;
        private String upLabel;
        private Locale inLanguage = Locale.forLanguageTag("id-ID");
        private List<String> contextNodeNames = new ArrayList<>();

        public FactServiceOperation getOp() {
            return op;
        }

        public void setOp(FactServiceOperation op) {
            this.op = op;
        }

        public String getUpLabel() {
            return upLabel;
        }

        public void setUpLabel(String upLabel) {
            this.upLabel = upLabel;
        }

        public Locale getInLanguage() {
            return inLanguage;
        }

        public void setInLanguage(Locale inLanguage) {
            this.inLanguage = inLanguage;
        }

        public List<String> getContextNodeNames() {
            return contextNodeNames;
        }

        public void setContextNodeNames(List<String> contextNodeNames) {
            this.contextNodeNames = contextNodeNames;
        }
    }

    public FactServicePage(PageParameters parameters) {
        super(parameters);

        final WebMarkupContainer matchResponseDiv = new WebMarkupContainer("matchResponseDiv");
        matchResponseDiv.setOutputMarkupId(true);

        final CompoundPropertyModel<FactServiceRequest> model = new CompoundPropertyModel<>(new FactServiceRequest());
        final Form<FactServiceRequest> form = new Form<>("form", model);
        form.add(new TextField<>("upLabel"));
        form.add(new DropDownChoice<>("inLanguage", LANGUAGE_CHOICES));
        form.add(new LaddaAjaxButton("matchBtn", new Model<>("Match..."), Buttons.Type.Primary) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                super.onSubmit(target, form);
                model.getObject().setOp(FactServiceOperation.match);
                target.add(matchResponseDiv);
            }
        }.setIconType(FontAwesomeIconType.search));

        final LoadableDetachableModel<List<MatchingThing>> matchingThingsModel = new LoadableDetachableModel<List<MatchingThing>>() {
            @Override
            protected List<MatchingThing> load() {
                final FactServiceRequest req = model.getObject();
                if (FactServiceOperation.match == req.getOp()) {
                    return factService.match(req.getUpLabel(), req.getInLanguage(), ImmutableMap.of());
                } else {
                    return ImmutableList.of();
                }
            }
        };
        matchResponseDiv.add(new ListView<MatchingThing>("matchResultLv", matchingThingsModel) {
            @Override
            protected void populateItem(ListItem<MatchingThing> item) {
                final MatchingThing match = item.getModelObject();
                final BookmarkablePageLink<ThingShowPage> link = new BookmarkablePageLink<>("link",
                        ThingShowPage.class, new PageParameters().set("nodeName", match.getThing().getNn()));
                link.add(new Label("nodeName", match.getThing().getNn()));
                link.add(new Label("confidence", match.getConfidence() * 100f));
                item.add(link);
            }
        });
        form.add(matchResponseDiv);

        add(form);

    }

    @Override
    public IModel<String> getTitleModel() {
        return new Model<>("Fact Service - " + getString("app.title"));
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new StringResourceModel("app.description");
    }
}
