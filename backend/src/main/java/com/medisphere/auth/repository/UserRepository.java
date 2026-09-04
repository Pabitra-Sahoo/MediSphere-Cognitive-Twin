package com.medisphere.auth.repository;

import com.medisphere.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link User} documents.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByLinkedProviderId(String linkedProviderId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
