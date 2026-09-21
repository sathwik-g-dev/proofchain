package com.proofchain.service;

import com.proofchain.dto.UserRequest;
import com.proofchain.dto.UserResponse;
import com.proofchain.entity.User;
import com.proofchain.exception.DuplicateResourceException;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Create user with valid data hashes password and persists entity")
    void createUser_withValidRequest_hashesPasswordAndSavesUser() {
        UserRequest request = new UserRequest("Sathwik", "sathwik@proofchain.io", "rawPassword123");

        when(userRepository.existsByEmail("sathwik@proofchain.io")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("Sathwik", response.getName());
        assertEquals("sathwik@proofchain.io", response.getEmail());
        verify(passwordEncoder, times(1)).encode("rawPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Create user with duplicate email throws DuplicateResourceException")
    void createUser_withDuplicateEmail_throwsDuplicateResourceException() {
        UserRequest request = new UserRequest("Sathwik", "sathwik@proofchain.io", "password123");
        when(userRepository.existsByEmail("sathwik@proofchain.io")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(request);
        });

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Get user by ID when not found throws ResourceNotFoundException")
    void getUserById_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(999L);
        });

        assertTrue(exception.getMessage().contains("User not found with id: 999"));
    }

    @Test
    @DisplayName("Login with valid credentials returns user response")
    void login_withValidCredentials_returnsUserResponse() {
        String rawPass = "secret123";
        String encoded = passwordEncoder.encode(rawPass);
        User user = new User("Sathwik", "sathwik@proofchain.io", encoded);

        when(userRepository.findByEmail("sathwik@proofchain.io")).thenReturn(Optional.of(user));

        com.proofchain.dto.LoginRequest loginReq = new com.proofchain.dto.LoginRequest("sathwik@proofchain.io", rawPass);
        UserResponse response = userService.login(loginReq);

        assertNotNull(response);
        assertEquals("Sathwik", response.getName());
        assertEquals("sathwik@proofchain.io", response.getEmail());
    }

    @Test
    @DisplayName("Login with incorrect password throws IllegalArgumentException")
    void login_withIncorrectPassword_throwsIllegalArgumentException() {
        String encoded = passwordEncoder.encode("correctPass");
        User user = new User("Sathwik", "sathwik@proofchain.io", encoded);

        when(userRepository.findByEmail("sathwik@proofchain.io")).thenReturn(Optional.of(user));

        com.proofchain.dto.LoginRequest loginReq = new com.proofchain.dto.LoginRequest("sathwik@proofchain.io", "wrongPass");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(loginReq);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));
    }

    @Test
    @DisplayName("Login with non-existent email throws ResourceNotFoundException")
    void login_withNonExistentEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("nonexistent@proofchain.io")).thenReturn(Optional.empty());

        com.proofchain.dto.LoginRequest loginReq = new com.proofchain.dto.LoginRequest("nonexistent@proofchain.io", "anyPass");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.login(loginReq);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));
    }
}
