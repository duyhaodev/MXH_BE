package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    @PostMapping(path = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {

        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return ResponseEntity.ok(postService.create(user.getId(), content, files));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject();
        User currentUser = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        postService.deletePost(currentUser.getId(), postId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> feed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String currentUsername = jwt.getSubject();
        User user = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        return ResponseEntity.ok(postService.getFeed(user.getId(), page, size));
    }

    // Xem profile của mình
    @GetMapping("/profile")
    public ResponseEntity<List<PostResponse>> getMyProfilePosts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<PostResponse> posts = postService.getPostsByUserId(user.getId(), user.getId());
        return ResponseEntity.ok(posts);
    }

    // Xem profile của mình - Tab Reposts
    @GetMapping("/profile/reposts")
    public ResponseEntity<List<PostResponse>> getMyReposts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // ownerId = viewerId = chính mình
        List<PostResponse> posts = postService.getRepostsByUserId(user.getId(), user.getId());
        return ResponseEntity.ok(posts);
    }

    // Xem profile của người khác
    @GetMapping("/profile/{username}")
    public ResponseEntity<List<PostResponse>> getUserProfilePosts(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUsername = jwt.getSubject();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        List<PostResponse> posts = postService.getPostsByUsername(username, currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    // Xem profile của người khác - TabReposts
    @GetMapping("/profile/{username}/reposts")
    public ResponseEntity<List<PostResponse>> getUserReposts(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUsername = jwt.getSubject();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        List<PostResponse> posts = postService.getRepostsByUsername(username, currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    // Xem chi tiết 1 bài post
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getOne(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUsername = jwt.getSubject();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        return ResponseEntity.ok(postService.getPostById(postId, currentUser.getId()));
    }

    // Repost bài viết
    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<PostResponse> repost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUsername = jwt.getSubject();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        PostResponse repost = postService.repost(currentUser.getId(), postId);
        return ResponseEntity.ok(repost);
    }

    @DeleteMapping("/posts/{postId}/repost")
    public ResponseEntity<java.util.Map<String, String>> unrepost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUsername = jwt.getSubject();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        String repostId = postService.unrepost(currentUser.getId(), postId);
        return ResponseEntity.ok(java.util.Map.of("repostId", repostId));
    }

}
