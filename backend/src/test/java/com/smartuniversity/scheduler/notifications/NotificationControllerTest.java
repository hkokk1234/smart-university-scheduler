package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.platform.api.*;
import com.smartuniversity.scheduler.platform.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@WebMvcTest(controllers =NotificationController.class, properties ="debug=false")
@Import(SecurityConfiguration.class)
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ChangeService changes;
    @MockitoBean NotificationStream streams;
    @MockitoBean JwtDecoder decoder;
    final UUID user =UUID.randomUUID();
    @Test void anonymousCannotReadTheInboxOrConnectToTheStream() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications/stream")).andExpect(status().isUnauthorized());
        verifyNoInteractions(changes, streams);
    }
    @Test void studentsReadOnlyTheirOwnInboxAndCannotReadAdministrativeHistory() throws Exception {
        when(changes.inbox(user, 0, 20)).thenReturn(new Inbox(new PageResponse<>(List.of(), 0, 20, 0, 0), 0));
        mvc.perform(get("/api/v1/notifications").with(jwt().jwt(token -> token.subject(user.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(changes).inbox(user, 0, 20);
        mvc.perform(get("/api/v1/admin/sections/" + UUID.randomUUID() + "/history").with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isForbidden());
    }
    @Test void foreignNotificationsReturn404AndReadAllIsOwnerScoped() throws Exception {
        UUID notification =UUID.randomUUID();
        doThrow(ApiException.notFound("Notification")).when(changes).markRead(notification, user);
        mvc.perform(post("/api/v1/notifications/" + notification + "/read").with(jwt().jwt(token -> token.subject(user.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notifications/read-all").with(jwt().jwt(token -> token.subject(user.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isNoContent());
        verify(changes).markAllRead(user);
    }
    @Test void administratorsCanReadHistoryAndPageLimitsAreValidated() throws Exception {
        UUID section =UUID.randomUUID();
        when(changes.history(section, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/v1/admin/sections/" + section + "/history").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications").param("size", "101").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isBadRequest());
    }
}
