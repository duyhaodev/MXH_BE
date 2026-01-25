package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.FollowResponse;
import CloneThreads.Threads.entity.Notification;
import CloneThreads.Threads.service.FollowService;
import CloneThreads.Threads.service.NotificationService;
import CloneThreads.Threads.service.UserService;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private FollowService followService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getActivities(Authentication auth,
                                                             @RequestParam(required = false) List<String> type,
                                                             @RequestParam(defaultValue = "10") int limit) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userId = userService.getUserIdByUsername(auth.getName());
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Notification> notifications = notificationService.findByUserIdOrderByCreatedAtDesc(userId);

            final List<String> finalTypes =
                    (type == null) ? List.of()
                            : type.stream()
                            .filter(t -> t != null
                                    && !"all".equalsIgnoreCase(t)
                                    && !"undefined".equalsIgnoreCase(t))
                            .toList();

            if (!finalTypes.isEmpty()) {
                notifications = notifications.stream()
                        .filter(n -> finalTypes.contains(n.getType()))
                        .toList();
            }


            // Map to activity format
            List<Map<String, Object>> activities = notifications.stream().map(n -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", n.getId());
                activity.put("type", n.getType());
                activity.put("message", n.getMessage());
                activity.put("timestamp", n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                activity.put("postId", n.getPostId());
                activity.put("read", n.getIsRead());

                // User info (fromUser)
                try {
                    var fromUserResp = userService.getUser(n.getFromUserId());
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", fromUserResp.getUserName());
                    user.put("displayName", fromUserResp.getFullName());
                    user.put("avatar", fromUserResp.getAvatarUrl());
                    activity.put("user", user);
                } catch (Exception e) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", "Unknown");
                    user.put("displayName", "Unknown User");
                    user.put("avatar", "");
                    user.put("verified", false);
                    activity.put("user", user);
                }

                // Check trạng thái follow back cho type "follow"
                if ("follow".equals(n.getType())) {
                    boolean followed = followService.isFollowing(userId, n.getFromUserId());
                    activity.put("followed", followed);
                }

                return activity;
            }).limit(limit).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("activities", activities);
            response.put("unreadCount", notificationService.getUnreadCount(userId));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi server, vui lòng thử lại sau!"));
        }
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId, Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userId = userService.getUserIdByUsername(auth.getName());
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            notificationService.markAsRead(notificationId, userId);
            return ResponseEntity.ok().build();
        } catch (AppException e) {
            HttpStatusCode status = e.getErrorCode().getHttpStatusCode();
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi server, vui lòng thử lại sau!"));
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userId = userService.getUserIdByUsername(auth.getName());
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi server, vui lòng thử lại sau!"));
        }
    }

    @PostMapping("/follow-back/{notificationId}")
    public ResponseEntity<?> followBack(@PathVariable String notificationId, Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String currentUserId = userService.getUserIdByUsername(auth.getName());
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid user"));
            }

            Notification notification = notificationService.findById(notificationId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

            if (!notification.getUserId().equals(currentUserId) || !"follow".equals(notification.getType())) {
                throw new AppException(ErrorCode.INVALID_NOTIFICATION);
            }

            String targetUserId = notification.getFromUserId();
            if (targetUserId == null || targetUserId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid target user"));
            }

            // Call follow service
            FollowResponse followResponse = followService.followUser(currentUserId, targetUserId);

            // Mark original notification as read
            notificationService.markAsRead(notificationId, currentUserId);

            log.info("Follow back success: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", followResponse.getMessage()));
        } catch (AppException e) {
            HttpStatusCode status = e.getErrorCode().getHttpStatusCode();
            log.warn("AppException in followBack: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
            return ResponseEntity.status(status)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in followBack for notificationId: {} and user: {}", notificationId,
                    auth != null ? auth.getName() : "unauthenticated", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi server, vui lòng thử lại sau!"));
        }
    }
}