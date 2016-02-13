package org.lskk.lumen.persistence.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.TextFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.lskk.lumen.persistence.TaxonomyRelated;
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

@MountPath("taxonomy")
public class TaxonomyListPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(TaxonomyListPage.class);

    @Inject @TaxonomyRelated
    private GraphDatabaseService taxonomyDb;

    public static class Thing implements Serializable {
        private Long id;
        private String nn;
        private String prefLabel;
        private String isPreferredMeaningOf;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNn() {
            return nn;
        }

        public void setNn(String nn) {
            this.nn = nn;
        }

        public String getPrefLabel() {
            return prefLabel;
        }

        public void setPrefLabel(String prefLabel) {
            this.prefLabel = prefLabel;
        }

        public String getIsPreferredMeaningOf() {
            return isPreferredMeaningOf;
        }

        public void setIsPreferredMeaningOf(String isPreferredMeaningOf) {
            this.isPreferredMeaningOf = isPreferredMeaningOf;
        }
    }

    public TaxonomyListPage(PageParameters parameters) {
        super(parameters);

        final List<Thing> things = new ArrayList<>();
        try (Transaction tx = taxonomyDb.beginTx()) {
            try (final Result thingsRes = taxonomyDb.execute("MATCH (t: schema_Thing) RETURN t LIMIT 25")) {
                while (thingsRes.hasNext()) {
                    final Map<String, Object> row = thingsRes.next();
                    final Node thingNode = (Node) row.get("t");
                    final Thing thing = new Thing();
                    thing.setId(thingNode.getId());
                    thing.setNn((String) thingNode.getProperty("nn"));
                    thing.setIsPreferredMeaningOf((String) thingNode.getProperty("isPreferredMeaningOf", null));
                    thing.setPrefLabel((String) thingNode.getProperty("prefLabel", null));
                    things.add(thing);
                }
            }
            tx.success();
        }

        final ImmutableList<PropertyColumn<Thing, String>> columns = ImmutableList.of(
            new PropertyColumn<>(new Model<>("ID"), "id", "id"),
            new TextFilteredPropertyColumn<>(new Model<>("Node name"), "nn"),
            new PropertyColumn<>(new Model<>("prefLabel"), "prefLabel"),
            new PropertyColumn<>(new Model<>("isPreferredMeaningOf"), "isPreferredMeaningOf")
        );
        final int ITEMS_PER_PAGE = 20;
        final SortableDataProvider<Thing, String> thingDp = new SortableDataProvider<Thing, String>() {
            {{
                setSort("creationTime", SortOrder.DESCENDING);
            }}

            @Override
            public Iterator iterator(long first, long count) {
                return things.iterator();
//                final PageRequest pageable = new PageRequest((int) (first / ITEMS_PER_PAGE), (int) count,
//                        getSort().isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, getSort().getProperty());
//                return placeRepo.findAll(pageable).iterator();
            }

            @Override
            public long size() {
                return things.size();
            }

            @Override
            public IModel model(Thing object) {
                return new Model<>(object);
            }
        };
        final DefaultDataTable<Thing, String> thingTable = new DefaultDataTable<>("thingTable", columns, thingDp, ITEMS_PER_PAGE);
        thingTable.setOutputMarkupId(true);
        add(thingTable);
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
