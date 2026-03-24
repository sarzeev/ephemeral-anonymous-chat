package com.example.chatbackend.session;

import com.example.chatbackend.model.TemporaryIdentity;
import java.util.Optional;

public interface TemporaryIdentityStore {

    Optional<TemporaryIdentity> findById(String temporaryUserId);

    void save(TemporaryIdentity temporaryIdentity);

    void delete(String temporaryUserId);
}

