package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.FollowResponse;
import CloneThreads.Threads.service.FollowService;
import CloneThreads.Threads.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    @Autowired
    private FollowService followService;
    @Autowired
    private UserService userService;

    // --- Toggle follow/unfollow ---
    @PostMapping("/{followingId}/toggle")
    public ResponseEntity<FollowResponse> toggleFollow(@PathVariable String followingId, Authentication auth) {
        String followerId = userService.getUserIdByUsername(auth.getName());
        try {
            boolean isFollowing = followService.isFollowing(followerId, followingId);
            FollowResponse response;
            if (isFollowing) {
                response = followService.unfollowUser(followerId, followingId);
            } else {
                response = followService.followUser(followerId, followingId);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FollowResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // --- Check follow status ---
    @GetMapping("/status/{followingId}")
    public ResponseEntity<?> checkFollowing(@PathVariable String followingId, Authentication auth) {
        String followerId = userService.getUserIdByUsername(auth.getName());
        try {
            boolean isFollowing = followService.isFollowing(followerId, followingId);
            return ResponseEntity.ok(new Object() {
                public final boolean isFollowingValue = isFollowing;
            });
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}