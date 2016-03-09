package org.lskk.lumen.reasoner.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.skill.Skill;

import javax.inject.Inject;

/**
 * Created by ceefour on 07/03/2016.
 */
public class SkillPanel extends GenericPanel<Skill> {

    @Inject
    private ObjectMapper mapper;

    public SkillPanel(String id, IModel<Skill> model) {
        super(id, model);
        add(new Label("skillJson", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                try {
                    return "var skill = " + mapper.writeValueAsString(getModelObject()) + ";";
                } catch (JsonProcessingException e) {
                    throw new ReasonerException(e, "Cannot serialize skill '%s' to JSON: %s",
                            getModelObject().getPath(), getModelObject());
                }
            }
        }).setEscapeModelStrings(false));
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        setVisibilityAllowed(null != getModelObject());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new WebjarsCssResourceReference("jointjs/0.9.7/dist/joint.min.css")));

        response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("lodash/3.10.1/lodash.min.js")));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("backbone/1.2.3/backbone-min.js")));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("graphlib/1.0.7/dist/graphlib.core.min.js")));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("dagre/0.7.4/dist/dagre.core.min.js")));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("jointjs/0.9.7/dist/joint.min.js")));
//        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("jointjs/0.9.7/dist/joint.shapes.uml.min.js")));
//        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("jointjs/0.9.7/dist/joint.layout.DirectedGraph.min.js")));
        response.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("underscore/1.8.3/underscore-min.js")));
    }
}
