package org.acme.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * LoginRequestDTO
 */
public record EmailLoginRequestDTO(

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email est invalide")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 5, message = "Le mot de passe doit faire au moins 5 caractères")
    String password
) {}
