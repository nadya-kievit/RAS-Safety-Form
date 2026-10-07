package com.ras.safetyform.service;

import com.ras.safetyform.config.SessionUser;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserSessionService(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** Deletes every stored session for the user so their access ends immediately. */
    public void invalidateAllSessions(Integer userId) {
        sessions.findByPrincipalName(SessionUser.principalName(userId))
                .keySet()
                .forEach(sessions::deleteById);
    }
}
