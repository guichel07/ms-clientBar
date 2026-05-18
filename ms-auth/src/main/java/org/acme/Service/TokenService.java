package org.acme.Service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    public String generateEncryptedToken(String email, String role) {
        return Jwt.issuer("https://acme.org/issuer")
            .upn(email)
            .groups(Set.of(role))
            .expiresIn(3600)
            .sign();
    }
}
