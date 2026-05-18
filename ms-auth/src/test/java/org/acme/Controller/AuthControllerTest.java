package org.acme.Controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.acme.DTO.EmailLoginRequestDTO;
import org.acme.DTO.UserCreateDTO;
import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.Exception.BusinessException;
import org.acme.NoDbProfile;
import org.acme.Service.TokenService;
import org.acme.Service.UserService;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration légers pour {@link AuthController}.
 * UserService et TokenService sont mockés via @InjectMock : aucune base de
 * données ni JWT réel n'est nécessaire, seul le câblage HTTP/JAX-RS est testé.
 */
@QuarkusTest
@TestProfile(NoDbProfile.class)
class AuthControllerTest {

    @InjectMock
    UserService userService;

    @InjectMock
    TokenService tokenService;

    private UserDTO buildUserDTO() {
        return new UserDTO(
                "john.doe@example.com",
                "John Doe",
                "JOHN",
                "SELLER",
                null,
                "0612345678"
        );
    }

    // ---------------------------------------------------------------
    // POST /ms-auth/login
    // ---------------------------------------------------------------

    @Test
    void login_shouldReturn200AndSetJwtCookie_whenCredentialsAreValid() {
        when(userService.loginWithEmail(any(EmailLoginRequestDTO.class)))
                .thenReturn(buildUserDTO());
        when(tokenService.generateEncryptedToken(anyString(), anyString()))
                .thenReturn("fake-jwt-token");

        given()
                .contentType(ContentType.JSON)
                .body(new EmailLoginRequestDTO("john.doe@example.com", "correct-password"))
                .when()
                .post("/ms-auth/login")
                .then()
                .statusCode(200)
                .body("email", equalTo("john.doe@example.com"))
                .body("role", equalTo("SELLER"))
                .cookie("jwt", "fake-jwt-token");
    }

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid() {
        when(userService.loginWithEmail(any(EmailLoginRequestDTO.class)))
                .thenThrow(new BusinessException(Response.Status.UNAUTHORIZED, "Bad password"));

        given()
                .contentType(ContentType.JSON)
                .body(new EmailLoginRequestDTO("john.doe@example.com", "wrong-password"))
                .when()
                .post("/ms-auth/login")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Bad password"));
    }

    @Test
    void login_shouldReturn400_whenEmailIsInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body(new EmailLoginRequestDTO("not-an-email", "whatever1"))
                .when()
                .post("/ms-auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void login_shouldReturn400_whenPasswordIsTooShort() {
        given()
                .contentType(ContentType.JSON)
                .body(new EmailLoginRequestDTO("john.doe@example.com", "ab"))
                .when()
                .post("/ms-auth/login")
                .then()
                .statusCode(400);
    }

    // ---------------------------------------------------------------
    // POST /ms-auth/register
    // ---------------------------------------------------------------

    @Test
    void register_shouldReturn201AndSetJwtCookie_whenPayloadIsValid() {
        when(userService.registerUser(any(UserCreateDTO.class)))
                .thenReturn(UserCredentialsDTO.of("Temp1234", "new.user@example.com", "SELLER"));
        when(tokenService.generateEncryptedToken(anyString(), anyString()))
                .thenReturn("fake-jwt-token");

        UserCreateDTO createDTO = new UserCreateDTO(
                "new.user@example.com",
                "New User",
                null,
                "SELLER",
                null,
                "0612345678"
        );

        given()
                .contentType(ContentType.JSON)
                .body(createDTO)
                .when()
                .post("/ms-auth/register")
                .then()
                .statusCode(201)
                .body("email", equalTo("new.user@example.com"))
                .body("temporaryPassword", equalTo("Temp1234"))
                .cookie("jwt", "fake-jwt-token");
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyUsed() {
        when(userService.registerUser(any(UserCreateDTO.class)))
                .thenThrow(new BusinessException(Response.Status.CONFLICT, "Cet email est déjà utilisé"));

        UserCreateDTO createDTO = new UserCreateDTO(
                "john.doe@example.com",
                "John Doe",
                null,
                "SELLER",
                null,
                "0612345678"
        );

        given()
                .contentType(ContentType.JSON)
                .body(createDTO)
                .when()
                .post("/ms-auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    void register_shouldReturn400_whenRoleIsInvalid() {
        UserCreateDTO createDTO = new UserCreateDTO(
                "new.user@example.com",
                "New User",
                null,
                "SUPERADMIN",
                null,
                "0612345678"
        );

        given()
                .contentType(ContentType.JSON)
                .body(createDTO)
                .when()
                .post("/ms-auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void register_shouldReturn400_whenContactIsInvalid() {
        UserCreateDTO createDTO = new UserCreateDTO(
                "new.user@example.com",
                "New User",
                null,
                "SELLER",
                null,
                "not-a-phone"
        );

        given()
                .contentType(ContentType.JSON)
                .body(createDTO)
                .when()
                .post("/ms-auth/register")
                .then()
                .statusCode(400);
    }

    // ---------------------------------------------------------------
    // GET /ms-auth/me
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "john.doe@example.com", roles = { "SELLER" })
    void me_shouldReturn200_whenAuthenticatedWithAllowedRole() {
        when(userService.getUserByEmail("john.doe@example.com"))
                .thenReturn(buildUserDTO());

        given()
                .when()
                .get("/ms-auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo("john.doe@example.com"));
    }

    @Test
    void me_shouldReturn401_whenNotAuthenticated() {
        given()
                .when()
                .get("/ms-auth/me")
                .then()
                .statusCode(401);
    }

    // ---------------------------------------------------------------
    // POST /ms-auth/logout
    // ---------------------------------------------------------------

    @Test
    void logout_shouldReturn200AndClearJwtCookie() {
        given()
                .when()
                .post("/ms-auth/logout")
                .then()
                .statusCode(200)
                .cookie("jwt", equalTo(""));
    }
}