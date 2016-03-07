package org.lskk.lumen.reasoner.web;

import com.google.common.collect.ImmutableList;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapBookmarkablePageLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.list.BootstrapListView;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
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
        final LoadableDetachableModel<List<Skill>> skillsModel = new LoadableDetachableModel<List<Skill>>() {
            @Override
            protected List<Skill> load() {
                return ImmutableList.copyOf(skillRepo.getSkills().values());
            }
        };
        add(new BootstrapListView<Skill>("skillsLv", skillsModel) {
            @Override
            protected void populateItem(ListItem<Skill> item) {
                item.add(new BookmarkablePageLink<>("link", SkillManagementPage.class,
                        new PageParameters().set("skillId", item.getModelObject().getId()))
                    .setBody(new PropertyModel<>(item.getModel(), "id")));
            }
        });
        final LoadableDetachableModel<Skill> skillModel = new LoadableDetachableModel<Skill>() {
            @Override
            protected Skill load() {
                final StringValue skillId = parameters.get("skillId");
                return !skillId.isEmpty() ? skillRepo.get(skillId.toString()) : null;
            }
        };
        final WebMarkupContainer skillDiv = new WebMarkupContainer("skillDiv", new CompoundPropertyModel<>(skillModel));
        skillDiv.add(new Label("id"));
        skillDiv.add(new Label("name"));
        skillDiv.add(new Label("description"));

        skillDiv.add(new SkillPanel("skillPanel", skillModel));

        add(skillDiv);
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
