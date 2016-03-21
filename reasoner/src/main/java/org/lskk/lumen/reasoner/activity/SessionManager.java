package org.lskk.lumen.reasoner.activity;

import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.reasoner.ux.Channel;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages {@link InteractionSession}s based on {@link org.lskk.lumen.reasoner.ux.Channel},
 * user identity, etc. You can open a session from chat and continue it on Twitter,
 * and can interact with session simultaneously both using chat and Twitter.
 * Created by ceefour on 06/03/2016.
 */
@Service
public class SessionManager {

    private List<InteractionSession> sessions = new ArrayList<>();

    @Inject
    private Provider<InteractionSession> sessionProvider;

    public List<InteractionSession> getSessions() {
        return sessions;
    }

    /**
     * Get or create a {@link InteractionSession} based on based on {@link org.lskk.lumen.reasoner.ux.Channel},
     * user identity, etc.
     * @return
     * @todo based on identity, etc.
     * @param channel
     * @param avatarId
     */
    public InteractionSession getOrCreate(Channel<?> channel, String avatarId) {
        if (sessions.isEmpty()) {
            final InteractionSession session = sessionProvider.get();
            session.getActiveLocales().add(LumenLocale.INDONESIAN);
            session.getActiveLocales().add(Locale.US);
            sessions.add(session);
            session.open(channel, avatarId);
            return session;
        } else {
            return sessions.get(0);
        }
    }

}
