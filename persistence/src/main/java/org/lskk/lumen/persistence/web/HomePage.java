package org.lskk.lumen.persistence.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage extends PubLayout {

//    @Inject
//    private PlaceRepository placeRepo;

    public HomePage(PageParameters parameters) {
        super(parameters);

//        final ImmutableList<PropertyColumn<Place, String>> columns = ImmutableList.of(
//            new PropertyColumn<>(new Model<>("ID"), "id", "id"),
//            new TextFilteredPropertyColumn<>(new Model<>("Name"), "name", "name"),
//            new PropertyColumn<>(new Model<>("Description"), "description"),
//            new PropertyColumn<>(new Model<>("Created"), "creationTime", "creationTime")
//        );
//        final int ITEMS_PER_PAGE = 20;
//        final SortableDataProvider placeDp = new SortableDataProvider<Place, String>() {
//            {{
//                setSort("creationTime", SortOrder.DESCENDING);
//            }}
//
//            @Override
//            public Iterator iterator(long first, long count) {
//                final PageRequest pageable = new PageRequest((int) (first / ITEMS_PER_PAGE), (int) count,
//                        getSort().isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, getSort().getProperty());
//                return placeRepo.findAll(pageable).iterator();
//            }
//
//            @Override
//            public long size() {
//                return placeRepo.count();
//            }
//
//            @Override
//            public IModel model(Place object) {
//                return new Model<>(object);
//            }
//        };
//        final DefaultDataTable<Place, String> placeTable = new DefaultDataTable<>("placeTable", columns, placeDp, ITEMS_PER_PAGE);
//        placeTable.setOutputMarkupId(true);
//        placeTable.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(1)));
//        add(placeTable);
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
