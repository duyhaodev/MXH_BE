package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final Cloudinary cloudinary;

    public PostResponse create(String userId, String content, MultipartFile image) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String mediaUrl = null;
        String mediaType = null;

        if (image != null && !image.isEmpty()) {
            String contentType = image.getContentType();
            String folder = "threads/posts/other";
            String detectedType = null;

            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    folder = "threads/posts/image";
                    detectedType = "image";
                } else if (contentType.startsWith("video/")) {
                    folder = "threads/posts/video";
                    detectedType = "video";
                }
            }
            Map<String, Object> upload = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto")
            );
            mediaUrl = (String) upload.get("secure_url");
            mediaType = detectedType;
        }
        Post post = Post.builder()
                .userId(user.getId())
                .content(content)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .scope("public")
                .createdAt(LocalDateTime.now())
                .build();
        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved, user);
    }

    // Giữ nguyên feed như cũ (có thể bạn muốn sort theo createdAt desc sau này)
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
