package org.acme.Repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

import org.acme.Entity.UserEntity;

@ApplicationScoped
public class UserRepository implements PanacheRepository<UserEntity> {

    public Optional<UserEntity> findByEmail(String email) {
        return Optional.ofNullable(find("email", email).firstResult());
    }

    public Optional<UserEntity> findByTag(String tag) {
        return Optional.ofNullable(find("tag", tag).firstResult());
    }
}
