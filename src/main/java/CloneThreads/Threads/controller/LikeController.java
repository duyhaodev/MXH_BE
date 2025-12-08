package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.LikeResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;

    @PostMapping("/posts/{postId}/likes/toggle")
    public LikeResponse togglePostLike(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
        return likeService.togglePostLike(postId, user.getId());
    }

    @PostMapping("/comments/{commentId}/likes/toggle")
    public LikeResponse toggleCommentLike(
            @PathVariable String commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));

        return likeService.toggleCommentLike(commentId, user.getId());
    }
}
