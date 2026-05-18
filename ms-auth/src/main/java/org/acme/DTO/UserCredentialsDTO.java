package org.acme.DTO;

/**
 * UserCredentialsDTO
 */
 public record UserCredentialsDTO(

     String temporaryPassword,

     String email,

     String role
 ) {
     public static UserCredentialsDTO of(String temporaryPassword, String email, String role) {
         return new UserCredentialsDTO(
             temporaryPassword,
             email,
             role
         );
     }
 }
