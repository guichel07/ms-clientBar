package org.acme.Service.impl;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.acme.DTO.EmailLoginRequestDTO;
import org.acme.DTO.UserCreateDTO;
import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.DTO.UserUpdateDTO;
import org.acme.Entity.UserEntity;
import org.acme.Exception.BusinessException;
import org.acme.Repository.UserRepository;
import org.acme.Service.UserService;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static final String PASSWORD_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";

    private static final SecureRandom RANDOM = new SecureRandom();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private String generateSecurePassword(int length) {
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    @Override
    public UserDTO loginWithEmail(EmailLoginRequestDTO emailLoginRequestDTO) {
        UserEntity userEntity = userRepository.findByEmail(
            emailLoginRequestDTO.email().toLowerCase().trim()
        ).orElseThrow(() -> new BusinessException(
            Response.Status.UNAUTHORIZED,
            "User not found"
        ));

        boolean passwordMatches = BcryptUtil.matches(
            emailLoginRequestDTO.password(),
            userEntity.getPassword()
        );

        if (!passwordMatches) {
            throw new BusinessException(
                Response.Status.UNAUTHORIZED,
                "Bad password"
            );
        }

        return UserDTO.fromEntity(userEntity);
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserEntity userEntity = Optional.ofNullable(userRepository.findById(id))
            .orElseThrow(() -> new BusinessException(
                Response.Status.NOT_FOUND,
                "User not found"
            ));

        return UserDTO.fromEntity(userEntity);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(
                Response.Status.NOT_FOUND,
                "User not found"
            ));
        return UserDTO.fromEntity(userEntity);
    }

    @Override
    public List<UserDTO> getAllUsersDTO() {
        return userRepository
            .findAll()
            .stream()
            .map(UserDTO::fromEntity)
            .toList();
    }

    @Override
    @Transactional
    public void deleteUserId(Long id) {
        if (!userRepository.deleteById(id)) {
            throw new BusinessException(
                Response.Status.NOT_MODIFIED,
                "Not deleted"
            );
        }
    }

    @Override
    @Transactional
    public UserCredentialsDTO registerUser(UserCreateDTO userCreateDTO) {

        if (userRepository.findByEmail(userCreateDTO.email().toLowerCase()).isPresent()) {
            throw new BusinessException(
                Response.Status.CONFLICT,
                "Cet email est déjà utilisé"
            );
        }

        String generatedPassword = generateSecurePassword(5);

        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setEmail(userCreateDTO.email().toLowerCase().trim());
        newUserEntity.setName(userCreateDTO.name().trim());
        newUserEntity.setRole(userCreateDTO.role());
        newUserEntity.setPassword(BcryptUtil.bcryptHash(generatedPassword));
        newUserEntity.setTag(generateTag(userCreateDTO.name()));
        newUserEntity.setSvgAvatar(generateDefaultAvatar(userCreateDTO.name()));
        newUserEntity.setContact(userCreateDTO.contact());
        userRepository.persist(newUserEntity);

        return UserCredentialsDTO.of(generatedPassword, newUserEntity.getEmail(), newUserEntity.getRole());
    }

    private String generateTag(String name) {
        String base = name.replaceAll("\\s+", "").toUpperCase();
        base = base.length() >= 4 ? base.substring(0, 4) : base;

        String tag = base;
        while (userRepository.findByTag(tag).isPresent()) {
            String prefix = base.length() >= 3 ? base.substring(0, 3) : base;
            tag = prefix + (char) ('A' + RANDOM.nextInt(26));
        }
        return tag;
    }

    private String generateDefaultAvatar(String name) {
        String initials =
            name.trim().length() >= 2
                ? name.trim().substring(0, 2).toUpperCase()
                : name.trim().toUpperCase();

        return String.format(
            """
                <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>
                    <circle cx='50' cy='50' r='50' fill='#6366f1'/>
                    <text x='50' y='55' text-anchor='middle'
                          dominant-baseline='middle'
                          font-size='35' fill='white' font-family='sans-serif'>%s</text>
                </svg>
            """,
            initials
        );
    }

    @Override
    @Transactional
    public UserDTO updateUser(UserUpdateDTO userDTO, String emailFromToken) {
        UserEntity userEntity = userRepository.findByEmail(emailFromToken)
            .orElseThrow(() -> new BusinessException(
                Response.Status.NOT_FOUND,
                "Utilisateur non trouvé : " + emailFromToken
            ));

        if (userDTO.name() != null && !userDTO.name().isBlank()) {
            userEntity.setName(userDTO.name().trim());
        }

        if (userDTO.svgAvatar() != null && !userDTO.svgAvatar().isBlank()) {
            userEntity.setSvgAvatar(userDTO.svgAvatar());
        }

        if (userDTO.contact() != null && !userDTO.contact().isBlank()) {
            userEntity.setContact(userDTO.contact().trim());
        }

        if (userDTO.tag() != null && !userDTO.tag().isBlank()) {
            UserEntity existingTag = userRepository.findByTag(userDTO.tag()).orElse(null);

            if (existingTag != null && !existingTag.getId().equals(userEntity.getId())) {
                throw new BusinessException(
                    Response.Status.CONFLICT,
                    "Ce tag est déjà utilisé"
                );
            }

            userEntity.setTag(userDTO.tag().toUpperCase().trim());
        }

        return UserDTO.fromEntity(userEntity);
    }

    @Override
    @Transactional
    public UserCredentialsDTO updatePassword(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(
                Response.Status.NOT_FOUND,
                "Utilisateur non trouvé : " + email
            ));

        String generatedPassword = generateSecurePassword(5);
        userEntity.setPassword(BcryptUtil.bcryptHash(generatedPassword));

        return UserCredentialsDTO.of(generatedPassword, userEntity.getEmail(), userEntity.getRole());
    }
}
