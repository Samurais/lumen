package org.lskk.lumen.reasoner.ux;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.service.Fact;
import org.lskk.lumen.reasoner.ReasonerException;

/**
 * Created by ceefour on 21/02/2016.
 */
public class Label extends NuiComponent {

    public Label(String id, Object model) {
        super(id, model);
    }

    @Override
    public String renderSsml() {
        if (null == getModel()) {
            return "";
        } else if (getModel() instanceof String) {
            return (String) getModel();
        } else if (getModel() instanceof DateTime) {
            return ((DateTime) getModel()).toString(DateTimeFormat.mediumDateTime());
        } else if (getModel() instanceof Fact) {
            final Fact fact = getModelAs(Fact.class);
            return fact.getObjectAsText();
        } else if (getModel() instanceof Thing) {
            final Thing thing = getModelAs(Thing.class);
            return thing.getPrefLabel() != null ? thing.getPrefLabel() : thing.getNn();
        } else {
            throw new ReasonerException(String.format("Cannot render SSML for %s %s", getModel().getClass().getName(), getModel()));
        }
    }
}
