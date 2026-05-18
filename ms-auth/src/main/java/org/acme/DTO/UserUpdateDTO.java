package org.acme.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UserUpdateDTO
 * DTO dédié à la mise à jour partielle du profil (name, tag, svgAvatar, contact).
 * L'email et le role ne sont volontairement pas modifiables via cet endpoint.
 */
public record UserUpdateDTO(
    @Size(min = 2, message = "Le nom doit faire au moins 2 caractères")
    String name,

    String tag,

    String svgAvatar,

    @Pattern(regexp = "^0[1-9]\\d{8}$", message = "Le numéro de contact est invalide")
    String contact
) {}
