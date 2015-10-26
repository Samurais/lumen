package org.lskk.lumen.reasoner.web;

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
        //add(new GrowlBehavior());
        add(new HtmlTag("html", Locale.US));
        add(new Label("title", getTitleModel()));
        add(new MetaTag("metaDescription", new Model<>("description"), getMetaDescriptionModel()));
        /*add(new BookmarkablePageLink<>("histogramLink", HistogramPage.class));
        add(new BookmarkablePageLink<>("equalizationLink", EqualizationPage.class));
        add(new BookmarkablePageLink<>("chaincodeLink", ChainCodePage.class));
        add(new BookmarkablePageLink<>("chaincodeImlLink", ChainCodeImlPage.class));
        add(new BookmarkablePageLink<>("zhangSuenLink", ZhangSuenPage.class));*/
//        add(new BookmarkablePageLink<>("roadsLink", RoadListPage.class,
//                new PageParameters().set(SeoBookmarkableMapper.LOCALE_PREF_ID_PARAMETER, localePrefId)));
//        add(new BookmarkablePageLink<>("camerasLink", CameraListPage.class,
//                new PageParameters().set(SeoBookmarkableMapper.LOCALE_PREF_ID_PARAMETER, localePrefId)));
//
//        add(new BookmarkablePageLink<>("tweetListLink", TweetListPage.class,
//                new PageParameters().set(SeoBookmarkableMapper.LOCALE_PREF_ID_PARAMETER, localePrefId)));
//        add(new BookmarkablePageLink<>("tweetMapLink", TweetMapPage.class,
//                new PageParameters().set(SeoBookmarkableMapper.LOCALE_PREF_ID_PARAMETER, localePrefId)));
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
