package org.acme.DTO;

import org.acme.Entity.UserEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
/**
 * SellerDTO
 */
public record UserDTO(

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email est invalide")
    String email,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, message = "Le nom doit faire au moins 2 caractères")
    String name,

    String tag,

    @NotBlank(message = "Le rôle est obligatoire")
    @Pattern(regexp = "ADMIN|SELLER", message = "Le rôle doit être ADMIN ou SELLER")
    String role,

    String svgAvatar,

    @NotBlank(message = "Le contact est obligatoire")
    @Pattern(regexp = "^0[1-9]\\d{8}$", message = "Le numéro de contact est invalide")
    String contact
) {
    public static UserDTO fromEntity(UserEntity entity) {
        return new UserDTO(
            entity.getEmail(),
            entity.getName(),
            entity.getTag(),
            entity.getRole(),
            entity.getSvgAvatar(),
            entity.getContact()
        );
    }
}
