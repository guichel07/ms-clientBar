package org.acme.Controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.DTO.UserUpdateDTO;
import org.acme.Service.UserService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/ms-users")
@Tag(name = "Users", description = "Gestion des Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ADMIN" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Récupérer un user par ID")
    @APIResponse(responseCode = "200", description = "User trouvé")
    @APIResponse(responseCode = "404", description = "User inexistant")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public UserDTO getUserById(@PathParam("id") Long id) {
        return userService.getUserById(id);
    }

    @GET
    @RolesAllowed({ "ADMIN" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Récupérer tous les users")
    @APIResponse(
        responseCode = "200",
        description = "Liste récupérée avec succès"
    )
    @APIResponse(responseCode = "403", description = "Accès refusé")
    public List<UserDTO> getAllUsersDTO() {
        return userService.getAllUsersDTO();
    }

    @PUT
    @RolesAllowed({ "ADMIN" })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Modifier son profil")
    @APIResponse(responseCode = "200", description = "Profil mis à jour")
    @APIResponse(responseCode = "403", description = "Accès refusé")
    @APIResponse(responseCode = "404", description = "Vendeur inexistant")
    public Response updatedUser(
        @Valid UserUpdateDTO updatedUserDTO,
        @Context SecurityContext ctx
    ) {
        String emailFromToken = ctx.getUserPrincipal().getName();
        UserDTO responseDTO = userService.updateUser(
            updatedUserDTO,
            emailFromToken
        );
        return Response.ok(responseDTO).build();
    }

    @DELETE
    @RolesAllowed({ "ADMIN" })
    @Path("/{id}")
    @Operation(summary = "Supprimer un vendeur")
    @APIResponse(responseCode = "204", description = "Vendeur supprimé")
    @APIResponse(responseCode = "404", description = "Vendeur inexistant")
    public Response deleteUserId(@PathParam("id") Long id) {
        userService.deleteUserId(id);
        return Response.noContent().build();
    }


    @POST
    @Path("/update/password")
    @RolesAllowed({ "SELLER", "ADMIN" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Réinitialiser le mot de passe",
        description = "Génère un nouveau mot de passe temporaire pour le vendeur connecté"
    )
    @APIResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public Response updatePassword(@Context SecurityContext ctx) {
        String emailFromToken = ctx.getUserPrincipal().getName();
        UserCredentialsDTO responseDTO = userService.updatePassword(emailFromToken);
        return Response.ok(responseDTO).build();
    }
}
