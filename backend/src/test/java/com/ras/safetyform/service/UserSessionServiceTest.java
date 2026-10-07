package com.ras.safetyform.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

class UserSessionServiceTest {

    @SuppressWarnings("unchecked")
    private final FindByIndexNameSessionRepository<Session> repository =
            mock(FindByIndexNameSessionRepository.class);
    private final UserSessionService service = new UserSessionService(repository);

    @Test
    void deletesEverySessionBelongingToTheUser() {
        when(repository.findByPrincipalName("7")).thenReturn(Map.of(
                "session-a", mock(Session.class),
                "session-b", mock(Session.class)));

        service.invalidateAllSessions(7);

        verify(repository).deleteById("session-a");
        verify(repository).deleteById("session-b");
    }

    @Test
    void doesNothingWhenTheUserHasNoSessions() {
        when(repository.findByPrincipalName("7")).thenReturn(Map.of());

        service.invalidateAllSessions(7);

        verify(repository).findByPrincipalName("7");
        verifyNoMoreInteractions(repository);
    }
}
