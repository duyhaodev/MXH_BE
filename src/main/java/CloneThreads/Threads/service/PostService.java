package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.PostCreateRequest;
import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public PostResponse create(String userId, PostCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .userId(user.getId()) // String
                .content(req.getContent())
                .mediaType(req.getMediaType())
                .mediaUrl(req.getMediaUrl())
                .scope(req.getScope())
                .createdAt(LocalDateTime.now())
                .build();

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved, user);
    }

    public List<PostResponse> getFeed(int page, int size) {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId()).orElse(null);
                    return postMapper.toResponse(post, user);
                })
                .collect(Collectors.toList());
    }
}
