package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.PostCreateRequest;
import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PostCreateRequest req
    ) {
        // Lấy username từ JWT (vì sub = username)
        String username = jwt.getSubject();
        // Tìm user theo username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        // Truyền userId sang service
        return ResponseEntity.ok(postService.create(user.getId(), req));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getFeed(page, size));
    }
}
