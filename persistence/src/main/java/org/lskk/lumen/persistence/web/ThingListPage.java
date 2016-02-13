package org.lskk.lumen.persistence.web;

import com.google.common.collect.ImmutableList;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.util.Iterator;

/**
 * Lists {@link org.lskk.lumen.persistence.neo4j.Thing}s
 * scoped by partition.
 */
@MountPath("schema_Thing")
public class ThingListPage extends PubLayout {

    private static Logger log = LoggerFactory.getLogger(ThingListPage.class);

    @Inject
    private ThingRepository thingRepo;

    public ThingListPage(PageParameters parameters) {
        super(parameters);

        final ImmutableList<PropertyColumn<Thing, String>> columns = ImmutableList.of(
            new LinkColumn<>(new Model<>("Node name"), "nn", "nn",
                    ThingShowPage.class, "nodeName", "nn"),
            new PropertyColumn<>(new Model<>("prefLabel"), "prefLabel", "prefLabel")
        );
        final int ITEMS_PER_PAGE = 20;
        final SortableDataProvider<Thing, String> thingDp = new SortableDataProvider<Thing, String>() {
            {{
                setSort("nn", SortOrder.ASCENDING);
            }}

            @Override
            public Iterator iterator(long first, long count) {
                final PageRequest pageable = new PageRequest((int) (first / ITEMS_PER_PAGE), (int) count,
                        getSort().isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, getSort().getProperty());
                return thingRepo.findAll(pageable).iterator();
            }

            @Override
            public long size() {
                return thingRepo.count();
            }

            @Override
            public IModel model(Thing object) {
                return new Model<>(object);
            }
        };
        final BootstrapDataTable<Thing, String> thingTable = new BootstrapDataTable<>("thingTable", columns, thingDp, ITEMS_PER_PAGE);
        thingTable.setOutputMarkupId(true);
        add(thingTable);
    }

    @Override
    public IModel<String> getTitleModel() {
        return new Model<>("Things - " + getString("app.title"));
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new StringResourceModel("app.description");
    }
}
