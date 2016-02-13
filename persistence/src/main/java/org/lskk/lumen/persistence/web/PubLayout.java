package org.lskk.lumen.persistence.web;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import de.agilecoders.wicket.core.markup.html.bootstrap.html.HtmlTag;
import de.agilecoders.wicket.core.markup.html.bootstrap.html.MetaTag;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeCssReference;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.util.Locale;

public abstract class PubLayout extends WebPage {

    protected NotificationPanel notificationPanel;

    public abstract IModel<String> getTitleModel();

    public abstract IModel<String> getMetaDescriptionModel();

    @Inject
    protected Environment env;

    public PubLayout(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new HtmlTag("html", Locale.US));
        add(new Label("title", getTitleModel()));
        add(new MetaTag("metaDescription", new Model<>("description"), getMetaDescriptionModel()));
        add(new BookmarkablePageLink<>("yagoTypeListLink", YagoTypeListPage.class));
        add(new BookmarkablePageLink<>("thingListLink", ThingListPage.class));
        add(new BookmarkablePageLink<>("taxonomyListLink", TaxonomyNeo4jListPage.class));
        notificationPanel = new NotificationPanel("notificationPanel");
        notificationPanel.setOutputMarkupId(true);
        add(notificationPanel);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(FontAwesomeCssReference.instance()));
    }

}
