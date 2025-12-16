package CloneThreads.Threads.service;

import CloneThreads.Threads.entity.Notification;
import CloneThreads.Threads.repository.NotificationRepository;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepo;
    @Autowired
    private UserService userService;

    @Transactional
    public Notification createFollowNotification(String toUserId, String fromUserId) {
        if (toUserId.equals(fromUserId)) {
            throw new AppException(ErrorCode.INVALID_NOTIFICATION);
        }

        // Fetch fromUser displayName for message
        String fromUserDisplayName = userService.getUser(fromUserId).getFullName();

        Notification notification = Notification.builder()
                .userId(toUserId)
                .fromUserId(fromUserId)
                .type("follow")
                .message(fromUserDisplayName + " followed you")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    public List<Notification> findByUserIdOrderByCreatedAtDesc(String userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Notification> findById(String id) {
        return notificationRepo.findById(id);
    }

    public long getUnreadCount(String userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        notification.setIsRead(true);
        notificationRepo.save(notification);
    }
}