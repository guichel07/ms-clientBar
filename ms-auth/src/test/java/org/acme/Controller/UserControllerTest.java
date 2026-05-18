package org.acme.Controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.DTO.UserUpdateDTO;
import org.acme.Exception.BusinessException;
import org.acme.NoDbProfile;
import org.acme.Service.UserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration légers pour {@link UserController}.
 * UserService est mocké via @InjectMock : on vérifie ici le routing HTTP,
 * les codes de statut et surtout l'application des rôles (@RolesAllowed).
 */
@QuarkusTest
@TestProfile(NoDbProfile.class)
class UserControllerTest {

    @InjectMock
    UserService userService;

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
    // GET /ms-users/{id}
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void getUserById_shouldReturn200_whenCallerIsAdmin() {
        when(userService.getUserById(1L)).thenReturn(buildUserDTO());

        given()
                .when()
                .get("/ms-users/1")
                .then()
                .statusCode(200)
                .body("email", equalTo("john.doe@example.com"));
    }

    @Test
    @TestSecurity(user = "seller@example.com", roles = { "SELLER" })
    void getUserById_shouldReturn403_whenCallerIsNotAdmin() {
        given()
                .when()
                .get("/ms-users/1")
                .then()
                .statusCode(403);
    }

    @Test
    void getUserById_shouldReturn401_whenNotAuthenticated() {
        given()
                .when()
                .get("/ms-users/1")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void getUserById_shouldReturn404_whenUserDoesNotExist() {
        when(userService.getUserById(99L))
                .thenThrow(new BusinessException(Response.Status.NOT_FOUND, "User not found"));

        given()
                .when()
                .get("/ms-users/99")
                .then()
                .statusCode(404);
    }

    // ---------------------------------------------------------------
    // GET /ms-users
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void getAllUsersDTO_shouldReturn200WithList_whenCallerIsAdmin() {
        when(userService.getAllUsersDTO()).thenReturn(List.of(buildUserDTO()));

        given()
                .when()
                .get("/ms-users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo("john.doe@example.com"));
    }

    @Test
    @TestSecurity(user = "seller@example.com", roles = { "SELLER" })
    void getAllUsersDTO_shouldReturn403_whenCallerIsNotAdmin() {
        given()
                .when()
                .get("/ms-users")
                .then()
                .statusCode(403);
    }

    // ---------------------------------------------------------------
    // PUT /ms-users
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void updatedUser_shouldReturn200_whenCallerIsAdmin() {
        UserDTO updated = new UserDTO(
                "john.doe@example.com",
                "Johnny Doe",
                "JOHN",
                "SELLER",
                null,
                "0612345678"
        );
        when(userService.updateUser(any(UserUpdateDTO.class), eq("admin@example.com")))
                .thenReturn(updated);

        UserUpdateDTO updateDTO = new UserUpdateDTO("Johnny Doe", null, null, null);

        given()
                .contentType(ContentType.JSON)
                .body(updateDTO)
                .when()
                .put("/ms-users")
                .then()
                .statusCode(200)
                .body("name", equalTo("Johnny Doe"));
    }

    @Test
    @TestSecurity(user = "seller@example.com", roles = { "SELLER" })
    void updatedUser_shouldReturn403_whenCallerIsNotAdmin() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("Johnny Doe", null, null, null);

        given()
                .contentType(ContentType.JSON)
                .body(updateDTO)
                .when()
                .put("/ms-users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void updatedUser_shouldReturn400_whenContactIsInvalid() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, "invalid-phone");

        given()
                .contentType(ContentType.JSON)
                .body(updateDTO)
                .when()
                .put("/ms-users")
                .then()
                .statusCode(400);
    }

    // ---------------------------------------------------------------
    // DELETE /ms-users/{id}
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void deleteUserId_shouldReturn204_whenCallerIsAdmin() {
        doNothing().when(userService).deleteUserId(1L);

        given()
                .when()
                .delete("/ms-users/1")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin@example.com", roles = { "ADMIN" })
    void deleteUserId_shouldReturn404_whenUserDoesNotExist() {
        doThrow(new BusinessException(Response.Status.NOT_FOUND, "Not deleted"))
                .when(userService).deleteUserId(99L);

        given()
                .when()
                .delete("/ms-users/99")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "seller@example.com", roles = { "SELLER" })
    void deleteUserId_shouldReturn403_whenCallerIsNotAdmin() {
        given()
                .when()
                .delete("/ms-users/1")
                .then()
                .statusCode(403);
    }

    // ---------------------------------------------------------------
    // POST /ms-users/update/password
    // ---------------------------------------------------------------

    @Test
    @TestSecurity(user = "john.doe@example.com", roles = { "SELLER" })
    void updatePassword_shouldReturn200_whenCallerIsAuthenticatedSeller() {
        when(userService.updatePassword("john.doe@example.com"))
                .thenReturn(UserCredentialsDTO.of("NewTemp123", "john.doe@example.com", "SELLER"));

        given()
                .when()
                .post("/ms-users/update/password")
                .then()
                .statusCode(200)
                .body("temporaryPassword", equalTo("NewTemp123"));
    }

    @Test
    void updatePassword_shouldReturn401_whenNotAuthenticated() {
        given()
                .when()
                .post("/ms-users/update/password")
                .then()
                .statusCode(401);
    }
}