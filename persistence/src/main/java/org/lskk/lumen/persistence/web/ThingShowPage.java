package org.lskk.lumen.persistence.web;

import com.google.common.base.Preconditions;
import de.agilecoders.wicket.core.markup.html.bootstrap.list.BootstrapListView;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.jpa.YagoLabel;
import org.lskk.lumen.persistence.jpa.YagoType;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;

@MountPath("schema_Thing/${nodeName}")
public class ThingShowPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(ThingShowPage.class);

    @Inject
    private YagoTypeRepository yagoTypeRepo;
    private final LoadableDetachableModel<YagoType> yagoTypeModel;

    public ThingShowPage(PageParameters parameters) {
        super(parameters);

        final String nodeName = parameters.get("nodeName").toString();
        yagoTypeModel = new LoadableDetachableModel<YagoType>() {
            @Override
            protected YagoType load() {
                final YagoType yagoType = Preconditions.checkNotNull(yagoTypeRepo.findOneByNn(nodeName),
                        "Cannot find yagoType schema_Thing/%s", nodeName);
//                yagoType.getSuperClasses().size();
                return yagoType;
            }
        };
        setDefaultModel(new CompoundPropertyModel<>(yagoTypeModel));

        add(new Label("nn"));
        add(new Label("prefLabel"));
        add(new Label("isPreferredMeaningOf"));
        add(new Label("hasGloss"));
        add(new BootstrapListView<YagoLabel>("labels") {
            @Override
            protected void populateItem(ListItem<YagoLabel> item) {
                final YagoLabel yagoLabel = item.getModelObject();
                item.add(new Label("value", yagoLabel.getValue()));
                item.add(new Label("inLanguage", yagoLabel.getInLanguage()));
            }
        });
        add(new BootstrapListView<YagoType>("superClasses") {
            @Override
            protected void populateItem(ListItem<YagoType> item) {
                final YagoType yagoType = item.getModelObject();
                item.add(new BookmarkablePageLink<>("link", ThingShowPage.class,
                        new PageParameters().set("nodeName", yagoType.getNn()))
                    .setBody(new Model<>(yagoType.getNn())));
            }
        });
        add(new BootstrapListView<YagoType>("subClasses") {
            @Override
            protected void populateItem(ListItem<YagoType> item) {
                final YagoType yagoType = item.getModelObject();
                item.add(new BookmarkablePageLink<>("link", ThingShowPage.class,
                        new PageParameters().set("nodeName", yagoType.getNn()))
                    .setBody(new Model<>(yagoType.getNn())));
            }
        });
    }

    @Override
    public IModel<String> getTitleModel() {
        return new Model<>(yagoTypeModel.getObject().getNn() + " - YAGO Type");
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new StringResourceModel("app.description");
    }
}
