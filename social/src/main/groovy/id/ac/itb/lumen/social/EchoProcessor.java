package id.ac.itb.lumen.social;

import id.ac.itb.lumen.core.StatusUpdate;
import org.apache.camel.ProducerTemplate;

/**
 * Created by NADIA on 27/02/2015.
 */
public class EchoProcessor {
    
    public void processStatus(StatusUpdate statusUpdate, StatusReplier replier) {
        final String reply = "Saya mendapat pesan dari " + statusUpdate.getFrom().getName() + " => " + statusUpdate.getMessage();
        replier.reply(new UserComment(statusUpdate.getThingId(), reply));
    }
    
}
