package org.lskk.lumen.reasoner.activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages {@link InteractionSession}s based on {@link org.lskk.lumen.reasoner.ux.Channel},
 * user identity, etc. You can open a session from chat and continue it on Twitter,
 * and can interact with session simultaneously both using chat and Twitter.
 * Created by ceefour on 06/03/2016.
 */
public class SessionManager {

    private List<InteractionSession> sessions = new ArrayList<>();

    public List<InteractionSession> getSessions() {
        return sessions;
    }


}
