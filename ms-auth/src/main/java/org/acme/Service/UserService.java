package org.acme.Service;

import java.util.List;

import org.acme.DTO.EmailLoginRequestDTO;
import org.acme.DTO.UserCreateDTO;
import org.acme.DTO.UserCredentialsDTO;
import org.acme.DTO.UserDTO;
import org.acme.DTO.UserUpdateDTO;

public interface UserService {
    UserDTO loginWithEmail(EmailLoginRequestDTO emailLoginRequestDTO);
    UserDTO getUserById(Long id);
    UserDTO getUserByEmail(String email);
    List<UserDTO> getAllUsersDTO();
    void deleteUserId(Long id);
    UserCredentialsDTO registerUser(UserCreateDTO userCreateDTO);
    UserDTO updateUser(UserUpdateDTO userUpdateDTO, String emailFromToken);
    UserCredentialsDTO updatePassword(String email);
}
