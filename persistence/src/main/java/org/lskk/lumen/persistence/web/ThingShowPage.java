package org.lskk.lumen.persistence.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.agilecoders.wicket.core.markup.html.bootstrap.list.BootstrapListView;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.persistence.service.FactServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.util.List;

@MountPath("schema_Thing/${nodeName}")
public class ThingShowPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(ThingShowPage.class);

//    @Inject
//    private ThingRepository thingRepo;
    @Inject
    private FactService factSvc;
    private final LoadableDetachableModel<Thing> thingModel;

    public ThingShowPage(PageParameters parameters) {
        super(parameters);

        final String nodeName = parameters.get("nodeName").toString();
        thingModel = new LoadableDetachableModel<Thing>() {
            @Override
            protected Thing load() {
                final Thing thing = Preconditions.checkNotNull(factSvc.describeThing(nodeName),
                        "Cannot find thing schema_Thing/%s", nodeName);
                return thing;
            }
        };
        setDefaultModel(new CompoundPropertyModel<>(thingModel));

        add(new Label("nn"));
        add(new Label("prefLabel"));
//        add(new Label("isPreferredMeaningOf"));
//        add(new Label("hasGloss"));
        final AbstractReadOnlyModel<List<Thing>> typesModel = new AbstractReadOnlyModel<List<Thing>>() {
            @Override
            public List<Thing> getObject() {
                return ImmutableList.copyOf(thingModel.getObject().getTypes());
            }
        };
        add(new BootstrapListView<Thing>("types", typesModel) {
            @Override
            protected void populateItem(ListItem<Thing> item) {
                final Thing thing = item.getModelObject();
                item.add(new BookmarkablePageLink<>("link", ThingShowPage.class,
                        new PageParameters().set("nodeName", thing.getNn()))
                    .setBody(new Model<>(thing.getNn())));
            }
        });
//        add(new BootstrapListView<YagoLabel>("labels") {
//            @Override
//            protected void populateItem(ListItem<YagoLabel> item) {
//                final YagoLabel yagoLabel = item.getModelObject();
//                item.add(new Label("value", yagoLabel.getValue()));
//                item.add(new Label("inLanguage", yagoLabel.getInLanguage()));
//            }
//        });
        final AbstractReadOnlyModel<List<Thing>> superClassesModel = new AbstractReadOnlyModel<List<Thing>>() {
            @Override
            public List<Thing> getObject() {
                return ImmutableList.copyOf(thingModel.getObject().getSuperClasses());
            }
        };
        add(new BootstrapListView<Thing>("superClasses", superClassesModel) {
            @Override
            protected void populateItem(ListItem<Thing> item) {
                final Thing thing = item.getModelObject();
                item.add(new BookmarkablePageLink<>("link", ThingShowPage.class,
                        new PageParameters().set("nodeName", thing.getNn()))
                    .setBody(new Model<>(thing.getNn())));
            }
        });
        final AbstractReadOnlyModel<List<Thing>> subClassesModel = new AbstractReadOnlyModel<List<Thing>>() {
            @Override
            public List<Thing> getObject() {
                return ImmutableList.copyOf(thingModel.getObject().getSubClasses());
            }
        };
        add(new BootstrapListView<Thing>("subClasses", subClassesModel) {
            @Override
            protected void populateItem(ListItem<Thing> item) {
                final Thing thing = item.getModelObject();
                item.add(new BookmarkablePageLink<>("link", ThingShowPage.class,
                        new PageParameters().set("nodeName", thing.getNn()))
                    .setBody(new Model<>(thing.getNn())));
            }
        });
    }

    @Override
    public IModel<String> getTitleModel() {
        return new Model<>(thingModel.getObject().getNn() + " - Thing");
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new StringResourceModel("app.description");
    }
}
