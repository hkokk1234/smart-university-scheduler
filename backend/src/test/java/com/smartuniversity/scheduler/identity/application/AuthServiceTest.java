package com.smartuniversity.scheduler.identity.application;

import com.smartuniversity.scheduler.identity.api.*;
import com.smartuniversity.scheduler.identity.domain.*;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuthServiceTest {
    private final UserRepository users =mock(UserRepository.class);
    private final PasswordEncoder passwords =mock(PasswordEncoder.class);
    private final TokenService tokens =mock(TokenService.class);

    private AuthService service() {
        when(passwords.encode(anyString())).thenReturn("encoded-test-hash");
        return new AuthService(users, passwords, tokens);
    }

    @Test
    void registrationHashesPasswordAndAlwaysCreatesStudent() {
        AuthService auth =service();
        when(users.findByEmail("new@example.edu")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        auth.register(new RegisterRequest("NEW@example.edu", "New Student", "a-long-test-password"));
        var captured =org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captured.capture());
        assertThat(captured.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(captured.getValue().getPasswordHash()).isEqualTo("encoded-test-hash");
        verify(passwords).encode("a-long-test-password");
        verify(tokens).issue(captured.getValue());
    }

    @Test
    void duplicateRegistrationDoesNotWriteOrIssueToken() {
        AuthService auth =service();
        when(users.findByEmail("used@example.edu")).thenReturn(Optional.of(new User("used@example.edu", "Used")));
        assertThatThrownBy(() -> auth.register(new RegisterRequest("used@example.edu", "Used", "a-long-test-password")))
                .isInstanceOf(ApiException.class).hasMessage("Email is already registered");
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(tokens);
    }

    @Test
    void unknownAndIncorrectCredentialsUseSameFailureAndNeverIssueTokens() {
        AuthService auth =service();
        when(users.findByEmail("missing@example.edu")).thenReturn(Optional.empty());
        when(users.findByEmail("known@example.edu")).thenReturn(Optional.of(new User("known@example.edu", "Known", "stored-hash")));
        for (String email : new String[]{"missing@example.edu", "known@example.edu"}) {
            assertThatThrownBy(() -> auth.login(new LoginRequest(email, "incorrect-test-password")))
                    .isInstanceOf(ApiException.class).hasMessage("Invalid email or password");
        }
        verify(passwords).matches("incorrect-test-password", "encoded-test-hash");
        verify(passwords).matches("incorrect-test-password", "stored-hash");
        verifyNoInteractions(tokens);
    }

    @Test
    void existingDemoUsersWithoutHashesCannotAuthenticate() {
        AuthService auth =service();
        when(users.findByEmail("fixture@example.edu")).thenReturn(Optional.of(new User("fixture@example.edu", "Fixture")));
        when(passwords.matches(anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> auth.login(new LoginRequest("fixture@example.edu", "a-long-test-password")))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(tokens);
    }
}
