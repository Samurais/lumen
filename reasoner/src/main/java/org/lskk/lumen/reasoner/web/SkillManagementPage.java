package org.lskk.lumen.reasoner.web;

import com.google.common.collect.ImmutableList;
import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapBookmarkablePageLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.list.BootstrapListView;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.ladda.LaddaAjaxButton;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.wicketstuff.annotation.mount.MountPath;

import javax.inject.Inject;
import java.util.List;

@MountPath("skills/${skillId}")
public class SkillManagementPage extends PubLayout {

    @Inject
    private SkillRepository skillRepo;

    public SkillManagementPage(PageParameters parameters) {
        super(parameters);

        final StringValue skillId = parameters.get("skillId");
        final LoadableDetachableModel<List<Skill>> skillsModel = new LoadableDetachableModel<List<Skill>>() {
            @Override
            protected List<Skill> load() {
                return ImmutableList.copyOf(skillRepo.getSkills().values());
            }
        };

        final Form<Void> form = new Form<>("form");
        form.add(new LaddaAjaxButton("reloadBtn", new Model<>("Reload"), Buttons.Type.Default) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                super.onSubmit(target, form);
                skillRepo.reload();
                setResponsePage(SkillManagementPage.class);
            }
        }.setIconType(FontAwesomeIconType.refresh));

        form.add(new BootstrapListView<Skill>("skillsLv", skillsModel) {
            @Override
            protected void populateItem(ListItem<Skill> item) {
                final CharSequence link = urlFor(SkillManagementPage.class,
                        new PageParameters().set("skillId", item.getModelObject().getId()));
                item.add(new AttributeModifier("href", link.toString()));
//                item.add(new BookmarkablePageLink<>("link", SkillManagementPage.class,
//                        new PageParameters().set("skillId", item.getModelObject().getId()))
//                    .setBody(new PropertyModel<>(item.getModel(), "id")));
                if (item.getModelObject().getId().equals(skillId.toString())) {
                    item.add(new CssClassNameAppender("active"));
                }
                item.add(new Label("id", new PropertyModel<>(item.getModel(), "id")));
            }
        });
        final LoadableDetachableModel<Skill> skillModel = new LoadableDetachableModel<Skill>() {
            @Override
            protected Skill load() {
                return !skillId.isEmpty() ? skillRepo.get(skillId.toString()) : null;
            }
        };
        final WebMarkupContainer skillDiv = new WebMarkupContainer("skillDiv", new CompoundPropertyModel<>(skillModel));
        skillDiv.add(new Label("id"));
        skillDiv.add(new Label("name"));
        skillDiv.add(new Label("description"));

        skillDiv.add(new SkillPanel("skillPanel", skillModel));

        form.add(skillDiv);
        add(form);
    }

    @Override
    public IModel<String> getTitleModel() {
        return new Model<>("Skills");
    }

    @Override
    public IModel<String> getMetaDescriptionModel() {
        return new Model<>("Lumen Reasoner");
    }
}
