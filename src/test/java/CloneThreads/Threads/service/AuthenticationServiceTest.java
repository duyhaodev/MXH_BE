package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.AuthenticationRequest;
import CloneThreads.Threads.dto.response.AuthenticationResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.repository.InvalidatedTokenRepository;
import CloneThreads.Threads.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        // Cần set giá trị cho @Value jwtSignerKey vì Mockito không tự inject từ properties
        ReflectionTestUtils.setField(authenticationService, "jwtSignerKey", "XKrZMWkVc5uTt7HeEKEp9NIhnaTUuYGZh8c37h8X32mN/MPC5DFZ0I2xRe6sZEIK");
        ReflectionTestUtils.setField(authenticationService, "jwtValidDuration", 10L);
    }

    @Test
    void authenticate_Success() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        AuthenticationRequest request = new AuthenticationRequest(email, password);

        User user = User.builder()
                .userName("testuser")
                .email(email)
                .passwordHash("encodedPassword")
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);

        // Act
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Assert
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getToken());
    }

    @Test
    void authenticate_Fail_UserNotFound() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("notfound@example.com", "password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));
        assertEquals(ErrorCode.WRONG_EMAIL_PASSWORD, exception.getErrorCode());
    }

    @Test
    void authenticate_Fail_WrongPassword() {
        // Arrange
        String email = "test@example.com";
        AuthenticationRequest request = new AuthenticationRequest(email, "wrongpass");
        User user = User.builder().email(email).passwordHash("encodedPass").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));
        assertEquals(ErrorCode.WRONG_EMAIL_PASSWORD, exception.getErrorCode());
    }

    @Test
    void authenticate_Fail_NotEnabled() {
        // Arrange
        String email = "test@example.com";
        AuthenticationRequest request = new AuthenticationRequest(email, "password");
        User user = User.builder().email(email).passwordHash("encodedPass").enabled(false).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPass")).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));
        assertEquals(ErrorCode.USER_NOT_ENABLED, exception.getErrorCode());
    }
}
