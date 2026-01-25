package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.ActivityGroupResponse;
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
    public ResponseEntity<?> getActivities(
            Authentication auth,
            @RequestParam(required = false) List<String> type,
            @RequestParam(defaultValue = "10") int limit) {

        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = userService.getUserIdByUsername(auth.getName());

        List<ActivityGroupResponse> activities =
                notificationService.getGroupedActivities(userId, type, limit);

        return ResponseEntity.ok(Map.of(
                "activities", activities,
                "unreadCount", notificationService.getUnreadCount(userId)
        ));
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

    @PostMapping("/follow-back/{targetUserId}")
    public ResponseEntity<?> followBack(@PathVariable String targetUserId, Authentication auth) {
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

//            Notification notification = notificationService.findById(notificationId)
//                    .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

//            if (!notification.getUserId().equals(currentUserId) || !"follow".equals(notification.getType())) {
//                throw new AppException(ErrorCode.INVALID_NOTIFICATION);
//            }
//
//            String targetUserId = notification.getFromUserId();
//            if (targetUserId == null || targetUserId.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Invalid target user"));
//            }

            log.info("Follow back: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            // Call follow service
            FollowResponse followResponse = followService.followUser(currentUserId, targetUserId);

            // Mark original notification as read
//            notificationService.markAsRead(notificationId, currentUserId);

            log.info("Follow back success: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", followResponse.getMessage()));
        } catch (AppException e) {
            HttpStatusCode status = e.getErrorCode().getHttpStatusCode();
            log.warn("AppException in followBack: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
            return ResponseEntity.status(status)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
//            log.error("Unexpected error in followBack for notificationId: {} and user: {}", notificationId,
//                    auth != null ? auth.getName() : "unauthenticated", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi server, vui lòng thử lại sau!"));
        }
    }
}