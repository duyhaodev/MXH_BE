package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.response.ApiResponse;
import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.service.SearchService;
import CloneThreads.Threads.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final SearchService searchService;


    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(@RequestParam("keyword") String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal Jwt jwt) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder().code(400).message("Keyword cannot be empty").build()
            );
        }
        String currentUserId = null;
        if (jwt != null) {
            String username = jwt.getSubject();
            currentUserId = userRepository.findByUserName(username)
                    .map(User::getId)
                    .orElse(null);
        }

        List<User> users = userRepository.searchUsers(keyword);
        List<PostResponse> posts = searchService.searchPosts(keyword, currentUserId, page, size);
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
