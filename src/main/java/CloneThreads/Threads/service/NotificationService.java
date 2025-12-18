package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.Notification;
import CloneThreads.Threads.entity.WebSocketSession;
import CloneThreads.Threads.repository.NotificationRepository;
import CloneThreads.Threads.repository.WebSocketSessionRepository;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {
    static Logger log = LoggerFactory.getLogger(NotificationService.class);

    NotificationRepository notificationRepo;
    UserService userService;
    SocketIOServer socketIOServer;
    WebSocketSessionRepository webSocketSessionRepository;
    ObjectMapper objectMapper;

    @Transactional
    public Notification createFollowNotification(String toUserId, String fromUserId) {
        if (toUserId.equals(fromUserId)) {
            throw new AppException(ErrorCode.INVALID_NOTIFICATION);
        }

        // Fetch fromUser displayName for message
        UserResponse fromUser = userService.getUser(fromUserId);
        String fromUserDisplayName = fromUser.getFullName();

        Notification notification = Notification.builder()
                .userId(toUserId)
                .fromUserId(fromUserId)
                .type("follow")
                .message(fromUserDisplayName + " followed you")
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepo.save(notification);

        sendRealtimeNotification(savedNotification, fromUser);

        return savedNotification;
    }

    private void sendRealtimeNotification(Notification notification, UserResponse fromUser) {
        try {
            List<WebSocketSession> sessions = webSocketSessionRepository.findAllByUserIdIn(List.of(notification.getUserId()));
            if (sessions.isEmpty()) return;

            Map<String, Object> activity = new HashMap<>();
            activity.put("id", notification.getId());
            activity.put("type", notification.getType());
            activity.put("message", notification.getMessage());
            activity.put("timestamp", notification.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            activity.put("read", false);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", fromUser.getUserName());
            userMap.put("displayName", fromUser.getFullName());
            userMap.put("avatar", fromUser.getAvatarUrl());
            activity.put("user", userMap);

            if ("follow".equals(notification.getType())) {
                activity.put("followed", false);
            }

            String jsonPayload = objectMapper.writeValueAsString(activity);

            for (WebSocketSession session : sessions) {
                try {
                    UUID clientUUID = UUID.fromString(session.getSocketSessionId());
                    SocketIOClient client = socketIOServer.getClient(clientUUID);
                    if (client != null) {
                        client.sendEvent("new_notification", jsonPayload);
                        log.info("Sent notification to user {} via socket {}", notification.getUserId(), clientUUID);
                    }
                } catch (Exception e) {
                    log.error("Error sending socket to session {}", session.getSocketSessionId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send realtime notification", e);
        }
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