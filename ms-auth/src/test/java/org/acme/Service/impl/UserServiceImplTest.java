package org.acme.Service.impl;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.ws.rs.core.Response;
import org.acme.DTO.EmailLoginRequestDTO;
import org.acme.DTO.UserCreateDTO;
import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.DTO.UserUpdateDTO;
import org.acme.Entity.UserEntity;
import org.acme.Exception.BusinessException;
import org.acme.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour {@link UserServiceImpl}.
 * Le repository est mocké : aucune base de données n'est nécessaire.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity buildUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
        user.setName("John Doe");
        user.setRole("SELLER");
        user.setTag("JOHN");
        user.setContact("0612345678");
        user.setPassword("hashed-password");
        return user;
    }

    // ---------------------------------------------------------------
    // loginWithEmail
    // ---------------------------------------------------------------

    @Test
    void loginWithEmail_shouldReturnUserDTO_whenCredentialsAreValid() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        EmailLoginRequestDTO request = new EmailLoginRequestDTO(
                "john.doe@example.com",
                "correct-password"
        );

        try (MockedStatic<BcryptUtil> bcrypt = Mockito.mockStatic(BcryptUtil.class)) {
            bcrypt.when(() -> BcryptUtil.matches("correct-password", "hashed-password"))
                    .thenReturn(true);

            UserDTO result = userService.loginWithEmail(request);

            assertNotNull(result);
            assertEquals("john.doe@example.com", result.email());
            assertEquals("SELLER", result.role());
        }
    }

    @Test
    void loginWithEmail_shouldThrowUnauthorized_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        EmailLoginRequestDTO request = new EmailLoginRequestDTO(
                "unknown@example.com",
                "whatever1"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.loginWithEmail(request)
        );

        assertEquals(Response.Status.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void loginWithEmail_shouldThrowUnauthorized_whenPasswordIsWrong() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        EmailLoginRequestDTO request = new EmailLoginRequestDTO(
                "john.doe@example.com",
                "wrong-password"
        );

        try (MockedStatic<BcryptUtil> bcrypt = Mockito.mockStatic(BcryptUtil.class)) {
            bcrypt.when(() -> BcryptUtil.matches("wrong-password", "hashed-password"))
                    .thenReturn(false);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userService.loginWithEmail(request)
            );

            assertEquals(Response.Status.UNAUTHORIZED, exception.getErrorCode());
            assertEquals("Bad password", exception.getMessage());
        }
    }

    @Test
    void loginWithEmail_shouldNormalizeEmail_beforeLookup() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        EmailLoginRequestDTO request = new EmailLoginRequestDTO(
                "  JOHN.DOE@example.com  ",
                "correct-password"
        );

        try (MockedStatic<BcryptUtil> bcrypt = Mockito.mockStatic(BcryptUtil.class)) {
            bcrypt.when(() -> BcryptUtil.matches(anyString(), anyString()))
                    .thenReturn(true);

            userService.loginWithEmail(request);

            verify(userRepository).findByEmail("john.doe@example.com");
        }
    }

    // ---------------------------------------------------------------
    // getUserById
    // ---------------------------------------------------------------

    @Test
    void getUserById_shouldReturnUserDTO_whenUserExists() {
        UserEntity user = buildUser();
        when(userRepository.findById(1L)).thenReturn(user);

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("john.doe@example.com", result.email());
    }

    @Test
    void getUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.getUserById(99L)
        );

        assertEquals(Response.Status.NOT_FOUND, exception.getErrorCode());
    }

    // ---------------------------------------------------------------
    // getUserByEmail
    // ---------------------------------------------------------------

    @Test
    void getUserByEmail_shouldReturnUserDTO_whenUserExists() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        UserDTO result = userService.getUserByEmail("john.doe@example.com");

        assertNotNull(result);
        assertEquals("John Doe", result.name());
    }

    @Test
    void getUserByEmail_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> userService.getUserByEmail("missing@example.com")
        );
    }

    // ---------------------------------------------------------------
    // getAllUsersDTO
    // ---------------------------------------------------------------

    @Test
    void getAllUsersDTO_shouldReturnMappedList() {
        UserEntity user1 = buildUser();
        UserEntity user2 = buildUser();
        user2.setId(2L);
        user2.setEmail("jane.doe@example.com");
        user2.setTag("JANE");

        io.quarkus.hibernate.orm.panache.PanacheQuery<UserEntity> panacheQuery =
                org.mockito.Mockito.mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(panacheQuery.stream()).thenReturn(java.util.stream.Stream.of(user1, user2));
        when(userRepository.findAll()).thenReturn(panacheQuery);

        List<UserDTO> result = userService.getAllUsersDTO();

        assertEquals(2, result.size());
        assertEquals("john.doe@example.com", result.get(0).email());
        assertEquals("jane.doe@example.com", result.get(1).email());
    }

    // ---------------------------------------------------------------
    // deleteUserId
    // ---------------------------------------------------------------

    @Test
    void deleteUserId_shouldSucceed_whenDeletionWorks() {
        when(userRepository.deleteById(1L)).thenReturn(true);

        userService.deleteUserId(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUserId_shouldThrow_whenDeletionFails() {
        when(userRepository.deleteById(1L)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.deleteUserId(1L)
        );

        assertEquals(Response.Status.NOT_MODIFIED, exception.getErrorCode());
    }

    // ---------------------------------------------------------------
    // registerUser
    // ---------------------------------------------------------------

    @Test
    void registerUser_shouldThrowConflict_whenEmailAlreadyExists() {
        UserCreateDTO createDTO = new UserCreateDTO(
                "john.doe@example.com",
                "John Doe",
                null,
                "SELLER",
                null,
                "0612345678"
        );

        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(buildUser()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.registerUser(createDTO)
        );

        assertEquals(Response.Status.CONFLICT, exception.getErrorCode());
        verify(userRepository, never()).persist(any(UserEntity.class));
    }

    @Test
    void registerUser_shouldPersistNewUser_whenEmailIsFree() {
        UserCreateDTO createDTO = new UserCreateDTO(
                "New.User@example.com",
                "New User",
                null,
                "SELLER",
                null,
                "0612345678"
        );

        when(userRepository.findByEmail("new.user@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByTag(anyString()))
                .thenReturn(Optional.empty());

        try (MockedStatic<BcryptUtil> bcrypt = Mockito.mockStatic(BcryptUtil.class)) {
            bcrypt.when(() -> BcryptUtil.bcryptHash(anyString()))
                    .thenReturn("hashed-temp-password");

            UserCredentialsDTO result = userService.registerUser(createDTO);

            assertNotNull(result);
            assertEquals("new.user@example.com", result.email());
            assertEquals("SELLER", result.role());
            assertNotNull(result.temporaryPassword());
            assertEquals(5, result.temporaryPassword().length());
        }

        verify(userRepository, times(1)).persist(any(UserEntity.class));
    }

    // ---------------------------------------------------------------
    // updateUser
    // ---------------------------------------------------------------

    @Test
    void updateUser_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com"))
                .thenReturn(Optional.empty());

        UserUpdateDTO updateDTO = new UserUpdateDTO("New Name", null, null, null);

        assertThrows(
                BusinessException.class,
                () -> userService.updateUser(updateDTO, "ghost@example.com")
        );
    }

    @Test
    void updateUser_shouldUpdateOnlyProvidedFields() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                "Johnny Doe",
                null,
                null,
                "0698765432"
        );

        UserDTO result = userService.updateUser(updateDTO, "john.doe@example.com");

        assertEquals("Johnny Doe", result.name());
        assertEquals("0698765432", result.contact());
        // tag was not provided => stays unchanged
        assertEquals("JOHN", result.tag());
    }

    @Test
    void updateUser_shouldThrowConflict_whenTagAlreadyUsedByAnotherUser() {
        UserEntity user = buildUser();

        UserEntity otherUser = buildUser();
        otherUser.setId(2L);
        otherUser.setTag("TAKEN");

        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByTag("TAKEN"))
                .thenReturn(Optional.of(otherUser));

        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "TAKEN", null, null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(updateDTO, "john.doe@example.com")
        );

        assertEquals(Response.Status.CONFLICT, exception.getErrorCode());
    }

    @Test
    void updateUser_shouldAllowKeepingOwnTag() {
        UserEntity user = buildUser();

        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByTag("JOHN"))
                .thenReturn(Optional.of(user));

        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "JOHN", null, null);

        UserDTO result = userService.updateUser(updateDTO, "john.doe@example.com");

        assertEquals("JOHN", result.tag());
    }

    // ---------------------------------------------------------------
    // updatePassword
    // ---------------------------------------------------------------

    @Test
    void updatePassword_shouldGenerateNewPassword_whenUserExists() {
        UserEntity user = buildUser();
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        try (MockedStatic<BcryptUtil> bcrypt = Mockito.mockStatic(BcryptUtil.class)) {
            bcrypt.when(() -> BcryptUtil.bcryptHash(anyString()))
                    .thenReturn("new-hashed-password");

            UserCredentialsDTO result = userService.updatePassword("john.doe@example.com");

            assertNotNull(result);
            assertEquals("john.doe@example.com", result.email());
            assertEquals(5, result.temporaryPassword().length());
        }

        assertEquals("new-hashed-password", user.getPassword());
    }

    @Test
    void updatePassword_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> userService.updatePassword("ghost@example.com")
        );
    }
}