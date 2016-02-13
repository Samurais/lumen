package org.lskk.lumen.persistence.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.TextFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.jpa.YagoType;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.util.Iterator;

@MountPath("yagotype/schema_Thing/${nodeName}")
public class YagoTypeShowPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(YagoTypeShowPage.class);

    @Inject
    private YagoTypeRepository yagoTypeRepo;
    private final LoadableDetachableModel<YagoType> yagoTypeModel;

    public YagoTypeShowPage(PageParameters parameters) {
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
        add(new Label("superClasses"));
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
