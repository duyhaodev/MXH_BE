package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.CommentResponse;
import CloneThreads.Threads.entity.Comment;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.CommentMapper;
import CloneThreads.Threads.repository.CommentRepository;
import CloneThreads.Threads.repository.LikeRepository;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentMapper commentMapper;
    private final Cloudinary cloudinary;

    private static final long MAX_MEDIA_SIZE = 20L * 1024 * 1024; // 20MB
    public CommentResponse create(String postId, String userId, String content, String parentId, List<MultipartFile> files) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Comment comment = Comment.builder()
                .postId(post.getId())
                .userId(user.getId())
                .content(content)
                .parentId(parentId)
                .createdAt(LocalDateTime.now())
                .build();

        // Lọc file null / rỗng
        List<MultipartFile> validFiles = (files == null)
                ? List.of()
                : files.stream()
                .filter(Objects::nonNull)
                .filter(f -> !f.isEmpty())
                .toList();

        // Validate hết trước
        for (MultipartFile file : validFiles) {
            validateMedia(file);
        }

        // Nếu có file → upload song song
        if (!validFiles.isEmpty()) {
            List<CompletableFuture<Media>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> uploadSingleMedia(file)))
                    .toList();

            List<Media> mediaList = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            for (Media m : mediaList) {
                comment.addMedia(m);
            }
        }
        comment = commentRepository.save(comment);
        return commentMapper.toResponse(comment, user);
    }

    private void validateMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Không xác định được loại file upload");
        }

        boolean isImage = contentType.startsWith("image/");
        boolean isVideo = contentType.startsWith("video/");

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("Chỉ cho phép upload hình ảnh hoặc video trong comment");
        }

        if (file.getSize() > MAX_MEDIA_SIZE) {
            throw new IllegalArgumentException("File quá lớn (tối đa 20MB) cho comment");
        }
    }

    private Media uploadSingleMedia(MultipartFile file) {
        try {
            String type = file.getContentType();
            boolean isImage = type != null && type.startsWith("image/");
            boolean isVideo = type != null && type.startsWith("video/");

            String folder = isImage
                    ? "threads/comments/image"
                    : "threads/comments/video";

            Map upload = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            String mediaUrl = (String) upload.get("secure_url");
            String mediaPublicId = (String) upload.get("public_id");
            String mediaType = isImage ? "image" : (isVideo ? "video" : "other");
            return Media.builder()
                    .mediaUrl(mediaUrl)
                    .mediaPublicId(mediaPublicId)
                    .mediaType(mediaType)
                    .build();

        } catch (Exception e) {
            log.error("Upload comment media failed", e);
            throw new RuntimeException("Upload media thất bại", e);
        }
    }

    public List<CommentResponse> getCommentsByPost(String postId, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of( page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
        return commentPage.getContent()
                .stream()
                .map(c -> {
                    User u = userRepository.findById(c.getUserId()).orElse(null);

                    long likeCount = likeRepository.countByCommentId(c.getId());
                    boolean likedByCurrentUser = currentUserId != null
                            && likeRepository.existsByUserIdAndCommentId(currentUserId, c.getId());

                    CommentResponse res = commentMapper.toResponse(c, u);
                    res.setLikeCount(likeCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    return res;
                })
                .toList();
    }

}
