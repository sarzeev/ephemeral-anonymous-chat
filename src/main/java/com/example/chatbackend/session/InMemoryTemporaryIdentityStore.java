package com.example.chatbackend.session;

import com.example.chatbackend.model.TemporaryIdentity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTemporaryIdentityStore implements TemporaryIdentityStore {

    @Override
    public Optional<TemporaryIdentity> findById(String temporaryUserId) {
        return Optional.empty();
    }

    @Override
    public void save(TemporaryIdentity temporaryIdentity) {
        // Placeholder for future temporary identity persistence implementation.
    }

    @Override
    public void delete(String temporaryUserId) {
        // Placeholder for future temporary identity cleanup implementation.
    }
}

