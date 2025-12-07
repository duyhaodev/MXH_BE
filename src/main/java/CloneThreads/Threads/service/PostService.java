package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.CommentRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final Cloudinary cloudinary;
    private final CommentRepository commentRepository;

    private static final long MAX_MEDIA_SIZE = 20L * 1024 * 1024;

    public PostResponse create(String userId, String content, List<MultipartFile> files) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .userId(user.getId())
                .content(content)
                .scope("public")
                .createdAt(LocalDateTime.now())
                .build();

        // Lọc bỏ file null / rỗng
        List<MultipartFile> validFiles;
        if (files == null || files.isEmpty()) {
            validFiles = Collections.emptyList();
        } else {
            validFiles = files.stream()
                    .filter(Objects::nonNull)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.toList());
        }

        // validate
        if (!validFiles.isEmpty()) {
            for (MultipartFile file : validFiles) {
                validateMedia(file);
            }
        }

        // UPLOAD SONG SONG LÊN CLOUDINARY
        if (!validFiles.isEmpty()) {
            List<CompletableFuture<Media>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> uploadSingleMedia(file)))
                    .collect(Collectors.toList());

            List<Media> mediaList = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Gắn media vào post
            for (Media media : mediaList) {
                post.addMedia(media);
            }
        }

        // Lưu post + media vào DB
        Post saved = postRepository.save(post);

        // 👇 ĐẾM COMMENT CHO POST VỪA TẠO
        long commentCount = commentRepository.countByPostId(saved.getId());

        PostResponse res = postMapper.toResponse(saved, user);
        res.setCommentCount(commentCount);
        return res;
    }

    private void validateMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Không xác định được loại file upload");
        }
        boolean isImage = contentType.startsWith("image/");
        boolean isVideo = contentType.startsWith("video/");

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("Chỉ cho phép upload hình ảnh hoặc video");
        }
        if (file.getSize() > MAX_MEDIA_SIZE) {
            throw new IllegalArgumentException("File quá lớn (tối đa 20MB)");
        }
    }

    private Media uploadSingleMedia(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            boolean isVideo = contentType != null && contentType.startsWith("video/");

            String folder = isImage
                    ? "threads/posts/image"
                    : "threads/posts/video";

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
            String detectedType = isImage ? "image" : "video";

            return Media.builder()
                    .mediaUrl(mediaUrl)
                    .mediaPublicId(mediaPublicId)
                    .mediaType(detectedType)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Upload media failed: " + e.getMessage(), e);
        }
    }

    public List<PostResponse> getFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAll(pageable);

        return postPage.stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId()).orElse(null);

                    // 👇 ĐẾM COMMENT CHO TỪNG POST
                    long commentCount = commentRepository.countByPostId(post.getId());

                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return posts.stream()
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByUsername(String username) {
        // Tìm user theo username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return posts.stream()
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    return res;
                })
                .collect(Collectors.toList());
    }

    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long commentCount = commentRepository.countByPostId(post.getId());

        PostResponse res = postMapper.toResponse(post, user);
        res.setCommentCount(commentCount);
        return res;
    }
}
