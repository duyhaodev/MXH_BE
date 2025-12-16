package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.ApiResponse;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Keyword cannot be empty")
                            .build()
            );
        }

        List<User> users = userRepository.searchUsers(keyword);
        List<Map<String, Object>> posts = postRepository.searchPosts(keyword);

        Map<String, Object> results = new HashMap<>();
        results.put("users", users);
        results.put("posts", posts);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(200)
                        .message("Search results")
                        .result(results)
                        .build()
        );
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> searchUser (@RequestParam("keyword") String keyword) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.searchUser(keyword))
                .build();
    }
}
