package org.lskk.lumen.persistence.web;

import com.google.common.collect.ImmutableList;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.TextFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.TaxonomyRelated;
import org.lskk.lumen.persistence.jpa.YagoType;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@MountPath("yagotype")
public class YagoTypeListPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(YagoTypeListPage.class);

    @Inject
    private YagoTypeRepository yagoTypeRepo;

    public YagoTypeListPage(PageParameters parameters) {
        super(parameters);

        final ImmutableList<PropertyColumn<YagoType, String>> columns = ImmutableList.of(
            new LinkColumn<>(new Model<>("Node name"), "nn", "nn",
                    YagoTypeShowPage.class, "nodeName", "nn"),
            new PropertyColumn<>(new Model<>("prefLabel"), "prefLabel", "prefLabel"),
            new PropertyColumn<>(new Model<>("isPreferredMeaningOf"), "isPreferredMeaningOf"),
            new PropertyColumn<>(new Model<>("hasGloss"), "hasGloss")
        );
        final int ITEMS_PER_PAGE = 20;
        final SortableDataProvider<YagoType, String> yagoTypeDp = new SortableDataProvider<YagoType, String>() {
            {{
                setSort("nn", SortOrder.ASCENDING);
            }}

            @Override
            public Iterator iterator(long first, long count) {
                final PageRequest pageable = new PageRequest((int) (first / ITEMS_PER_PAGE), (int) count,
                        getSort().isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, getSort().getProperty());
                return yagoTypeRepo.findAll(pageable).iterator();
            }

            @Override
            public long size() {
                return yagoTypeRepo.count();
            }

            @Override
            public IModel model(YagoType object) {
                return new Model<>(object);
            }
        };
        final BootstrapDataTable<YagoType, String> yagoTypeTable = new BootstrapDataTable<>("yagoTypeTable", columns, yagoTypeDp, ITEMS_PER_PAGE);
        yagoTypeTable.setOutputMarkupId(true);
        add(yagoTypeTable);
//        add(new BootstrapAjaxPagingNavigator("navigator", yagoTypeTable));
    }

    @Override
    public IModel<String> getTitleModel() {
        return new StringResourceModel("app.title");
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new StringResourceModel("app.description");
    }
}
