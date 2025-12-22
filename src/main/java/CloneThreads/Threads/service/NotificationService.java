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

import java.time.LocalDateTime;
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

        // Check if notification already exists
        Optional<Notification> existingNotification = notificationRepo.findExistFollowNotification(toUserId, fromUserId, "follow");

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            // Update the date
            notification.setCreatedAt(LocalDateTime.now().withNano(0));

            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("follow")
                    .message(fromUserDisplayName + " followed you")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
    }

    @Transactional
    public Notification createLikePostNotification(String toUserId, String fromUserId, String postId) {
        if (toUserId.equals(fromUserId)) {
            return null;  // Không tạo noti nếu self-like
        }

        // Fetch fromUser displayName for message
        UserResponse fromUser = userService.getUser(fromUserId);
        String fromUserDisplayName = fromUser.getFullName();

        // Check if notification already exists
        Optional<Notification> existingNotification = notificationRepo.findExistForPostNotification(toUserId, fromUserId, "like_post", postId);

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            // Update the date
            notification.setCreatedAt(LocalDateTime.now().withNano(0));
            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("like_post")
                    .message(fromUserDisplayName + " liked your post")
                    .postId(postId)
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
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
            else if ("comment_post".equals(notification.getType())) {
                activity.put("postId", notification.getPostId());
            }
            else if ("repost".equals(notification.getType())) {
                activity.put("postId", notification.getPostId());
            }
            else if ("like_post".equals(notification.getType())) {
                activity.put("postId", notification.getPostId());
            }
            else if ("like_comment".equals(notification.getType())) {
                activity.put("postId", notification.getPostId());
                activity.put("commentId", notification.getCommentId());
            }

            String jsonPayload = objectMapper.writeValueAsString(activity);

            for (WebSocketSession session : sessions) {
                try {
                    UUID clientUUID = UUID.fromString(session.getSocketSessionId());
                    SocketIOClient client = socketIOServer.getClient(clientUUID);
                    if (client != null) {
                        client.sendEvent("new_notification", jsonPayload);
                    }
                } catch (Exception e) {
                    log.error("Error sending socket to session {}", session.getSocketSessionId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send realtime notification", e);
        }
    }

    @Transactional
    public Notification createLikeCommentNotification(String toUserId, String fromUserId, String commentId, String postId) {
        if (toUserId.equals(fromUserId)) {
            return null;  // Không tạo noti nếu self-like
        }

        // Fetch fromUser displayName for message
        String fromUserDisplayName = userService.getUser(fromUserId).getFullName();
        UserResponse fromUser = userService.getUser(fromUserId);

        // Check if notification already exists
        Optional<Notification> existingNotification = notificationRepo.findExistForCommentNotification(toUserId, fromUserId, "like_comment", commentId);

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            // Update the date
            notification.setCreatedAt(LocalDateTime.now().withNano(0));
            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("like_comment")
                    .message(fromUserDisplayName + " liked your comment")
                    .commentId(commentId)
                    .postId(postId)
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
    }

    @Transactional
    public Notification createCommentOnPostNotification(String toUserId, String fromUserId, String commentId, String postId) {
        if (toUserId.equals(fromUserId)) {
            return null;  // Không tạo noti nếu self-comment
        }

        // Fetch fromUser displayName for message
        String fromUserDisplayName = userService.getUser(fromUserId).getFullName();
        UserResponse fromUser = userService.getUser(fromUserId);

        // Check if notification already exists
        Optional<Notification> existingNotification = notificationRepo.findExistForPostNotification(toUserId, fromUserId, "comment_post", postId);

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            notification.setCreatedAt(LocalDateTime.now().withNano(0));
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("comment_post")
                    .message(fromUserDisplayName + " commented on your post")
                    .commentId(commentId)  // Link to comment
                    .postId(postId)
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
    }

    @Transactional
    public Notification createReplyNotification(String toUserId, String fromUserId, String commentId, String parentCommentId) {
        if (toUserId.equals(fromUserId)) {
            return null;  // Không tạo noti nếu self-reply
        }

        // Fetch fromUser displayName for message
        UserResponse fromUser = userService.getUser(fromUserId);
        String fromUserDisplayName = fromUser.getFullName();

        // Check if notification already exists
        Optional<Notification> existingNotification = notificationRepo.findExistForCommentNotification(toUserId, fromUserId, "reply_comment", parentCommentId);

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            notification.setCreatedAt(LocalDateTime.now().withNano(0));
            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("reply_comment")
                    .message(fromUserDisplayName + " replied to your comment")
                    .commentId(commentId)  // Link to reply comment
                    .postId(null)
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
    }

    @Transactional
    public Notification createRepostNotification(String toUserId, String fromUserId, String postId) {
        if (toUserId.equals(fromUserId)) {
            return null;  // Không tạo noti nếu self-repost
        }

        // Fetch fromUser displayName for message
        String fromUserDisplayName = userService.getUser(fromUserId).getFullName();
        UserResponse fromUser = userService.getUser(fromUserId);

        // Check if notification already exists (với postId trùng)
        Optional<Notification> existingNotification = notificationRepo.findExistForPostNotification(toUserId, fromUserId, "repost_post", postId);

        if (existingNotification.isPresent()) {
            Notification notification = existingNotification.get();
            // Update the date
            notification.setCreatedAt(LocalDateTime.now().withNano(0));
            return notificationRepo.save(notification);
        } else {
            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .type("repost")
                    .message(fromUserDisplayName + " reposted your post")
                    .postId(postId)
                    .isRead(false)
                    .createdAt(LocalDateTime.now().withNano(0))
                    .build();
            sendRealtimeNotification(notification, fromUser);
            return notificationRepo.save(notification);
        }
    }

    @Transactional
    public void deleteRepostNotification(String toUserId, String fromUserId, String postId) {
        Optional<Notification> notificationOpt = notificationRepo.findExistForPostNotification(toUserId, fromUserId, "repost", postId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notificationRepo.delete(notification);
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

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepo.markAllAsRead(userId);
    }
}