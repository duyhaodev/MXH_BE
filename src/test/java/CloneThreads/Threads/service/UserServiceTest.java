package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.UserCreationRequest;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.mapper.UserMapper;
import CloneThreads.Threads.repository.UserRepository;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- TEST REGISTER USER ---

    @Test
    void createUser_Success() {
        // Arrange
        UserCreationRequest request = UserCreationRequest.builder()
                .email("newuser@gmail.com")
                .fullName("New User")
                .password("password123")
                .build();

        User userEntity = User.builder()
                .email("newuser@gmail.com")
                .userName("newuser")
                .build();
        
        User savedUser = User.builder()
                .id("user-id-123")
                .email("newuser@gmail.com")
                .userName("newuser")
                .enabled(false)
                .build();

        UserResponse response = UserResponse.builder()
                .id("user-id-123")
                .email("newuser@gmail.com")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(userEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(response);

        // Act
        UserResponse result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("newuser@gmail.com", result.getEmail());
        verify(emailService).sendVerificationEmail(eq("newuser@gmail.com"), anyString());
    }

    @Test
    void createUser_Fail_EmailExisted() {
        // Arrange
        UserCreationRequest request = UserCreationRequest.builder().email("exist@gmail.com").build();
        when(userRepository.existsByEmail("exist@gmail.com")).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> userService.createUser(request));
        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    // --- TEST SEARCH USER ---

    @Test
    void searchUser_excludeSelf() {
        // Arrange
        String currentUsername = "currentUser";
        String otherUsername = "otherUser";
        String keyword = "User";

        User currentUser = User.builder().userName(currentUsername).fullName("Current User").build();
        User otherUser = User.builder().userName(otherUsername).fullName("Other User").build();

        UserResponse otherUserResponse = UserResponse.builder().userName(otherUsername).build();

        // Mock Security Context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(currentUsername);
        SecurityContextHolder.setContext(securityContext);

        // Mock Repository
        when(userRepository.searchUsers(keyword)).thenReturn(List.of(currentUser, otherUser));

        // Mock Mapper
        when(userMapper.toUserResponse(otherUser)).thenReturn(otherUserResponse);

        // Act
        List<UserResponse> results = userService.searchUser(keyword);

        // Assert
        assertEquals(1, results.size());
        assertEquals(otherUsername, results.get(0).getUserName());
        verify(userMapper, times(1)).toUserResponse(any(User.class));
        verify(userMapper).toUserResponse(otherUser);
        verify(userMapper, never()).toUserResponse(currentUser);
    }
}