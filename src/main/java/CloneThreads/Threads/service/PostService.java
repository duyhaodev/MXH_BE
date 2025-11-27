package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PostResponse create(String userId, String content, List<MultipartFile> files) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .userId(user.getId())
                .content(content)
                .scope("public")
                .createdAt(LocalDateTime.now())
                .build();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                String contentType = file.getContentType();
                String folder = "threads/posts/other";

                if (contentType != null) {
                    if (contentType.startsWith("image/")) {
                        folder = "threads/posts/image";
                    } else if (contentType.startsWith("video/")) {
                        folder = "threads/posts/video";
                    }
                }

                // Upload lên Cloudinary
                Map<String, Object> upload = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", folder,
                                "resource_type", "auto"
                        )
                );

                String mediaUrl = (String) upload.get("secure_url");
                String mediaPublicId = (String) upload.get("public_id");
                String detectedType = detectMediaType(contentType);

                Media media = Media.builder()
                        .mediaUrl(mediaUrl)
                        .mediaPublicId(mediaPublicId)
                        .mediaType(detectedType)
                        .build();
                post.addMedia(media);
            }
        }

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved, user);
    }

    private String detectMediaType(String contentType) {
        if (contentType == null) return "other";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        return "other";
    }

    public List<PostResponse> getFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAll(pageable);

        return postPage.stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId()).orElse(null);
                    return postMapper.toResponse(post, user);
                })
                .collect(Collectors.toList());
    }
    public List<PostResponse> getPostsByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return posts.stream()
                .map(post -> postMapper.toResponse(post, user))
                .collect(Collectors.toList());
    }
    public List<PostResponse> getPostsByUsername(String username) {
        // Tìm user theo username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return getPostsByUserId(user.getId());
    }
    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return postMapper.toResponse(post, user);
    }


}
