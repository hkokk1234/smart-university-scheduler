package com.smartuniversity.scheduler.identity.application;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly =true)
public class IdentityReferences {
    private final UserRepository users;
    public IdentityReferences(UserRepository users) { this.users =users; }
    public User user(UUID id) { return users.findById(id).orElseThrow(() -> ApiException.notFound("User")); }
}
