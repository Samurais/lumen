package org.lskk.lumen.persistence.web;

import com.google.common.collect.ImmutableList;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.ladda.LaddaAjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.lskk.lumen.persistence.service.FactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
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

    public static class FactServiceRequest {
        private FactServiceOperation op = null;
        private String upLabel;
        private Locale inLanguage = Locale.forLanguageTag("id-ID");
        private List<String> contextNodeNames = new ArrayList<>();
    }

    public FactServicePage(PageParameters parameters) {
        super(parameters);

        final WebMarkupContainer matchResponseDiv = new WebMarkupContainer("matchResponseDiv");
        matchResponseDiv.setOutputMarkupId(true);

        final CompoundPropertyModel<FactServiceRequest> model = new CompoundPropertyModel<>(new FactServiceRequest());
        final Form<FactServiceRequest> form = new Form<>("form", model);
        form.add(new TextField<>("upLabel"));
        form.add(new DropDownChoice<>("inLanguage", LANGUAGE_CHOICES));
        form.add(new LaddaAjaxButton("matchBtn", Buttons.Type.Primary) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                super.onSubmit(target, form);
                target.add(matchResponseDiv);
            }
        });



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
