package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.CommentResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    // Lấy danh sách comment của 1 post
    @GetMapping
    public List<CommentResponse> listComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUserId = null;
        if (jwt != null) {
            String username = jwt.getSubject();
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            currentUserId = user.getId();
        }
        return commentService.getCommentsByPost(postId, currentUserId, page, size);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentResponse createComment(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "parentId", required = false) String parentId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        String username = jwt.getSubject();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return commentService.create(postId, user.getId(), content, parentId, files);
    }
}
