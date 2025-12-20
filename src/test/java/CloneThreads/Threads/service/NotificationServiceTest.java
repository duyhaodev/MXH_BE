package CloneThreads.Threads.service;

import CloneThreads.Threads.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markAllAsRead_shouldCallRepository() {
        // Arrange
        String userId = "user123";

        // Act
        notificationService.markAllAsRead(userId);

        // Assert
        verify(notificationRepository).markAllAsRead(userId);
    }
}
