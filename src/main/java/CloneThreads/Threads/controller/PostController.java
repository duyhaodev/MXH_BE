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

    // NHẬN multipart/form-data: content (text) + image (file)
    @PostMapping(path = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws IOException {

        // Lấy username từ JWT (sub = username)
        String username = jwt.getSubject();
        // Tìm user theo username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return ResponseEntity.ok(postService.create(user.getId(), content, image));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getFeed(page, size));
    }
}
