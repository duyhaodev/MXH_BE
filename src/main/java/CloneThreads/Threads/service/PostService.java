package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.CommentRepository;
import CloneThreads.Threads.repository.LikeRepository;
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
import org.springframework.transaction.annotation.Transactional;

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
    private final LikeRepository likeRepository;


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

    @Transactional
    public void deletePost(String currentUserId, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getRepostOf() != null) {
            throw new RuntimeException("Không thể xóa bài repost");
        }

        if (!post.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài này");
        }

        postRepository.deleteByRepostOf_Id(post.getId());
        if (post.getMediaList() != null) {
            for (Media media : post.getMediaList()) {
                try {
                    cloudinary.uploader().destroy(
                            media.getMediaPublicId(),
                            ObjectUtils.asMap("resource_type", "auto")
                    );
                } catch (Exception e) {
                    System.err.println("Delete media failed: " + media.getMediaPublicId());
                }
            }
        }
        postRepository.delete(post);
    }

    // HELPER
    private String getOriginalId(Post post) {
        return post.getRepostOf() != null
                ? post.getRepostOf().getId()
                : post.getId();
    }

    public List<PostResponse> getFeed(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId()).orElse(null);
                    String originalId = getOriginalId(post);
                    long commentCount = commentRepository.countByPostId(originalId);
                    long likeCount = likeRepository.countByPostId(originalId);
                    long repostCount = postRepository.countByRepostOf_Id(originalId);
                    boolean likedByCurrentUser = false;
                    if (currentUserId != null) {
                        likedByCurrentUser = likeRepository
                                .existsByUserIdAndPostId(currentUserId, originalId);
                    }
                    boolean repostedByCurrentUser = false;
                    if (currentUserId != null) {
                        repostedByCurrentUser = postRepository
                                .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
                    }

                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    res.setLikeCount(likeCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    res.setRepostCount(repostCount);
                    res.setRepostedByCurrentUser(repostedByCurrentUser);

                    if (post.getRepostOf() != null) {
                        Post original = post.getRepostOf();
                        User originalUser = userRepository.findById(original.getUserId())
                                .orElse(null);
                        if (originalUser != null) {
                            res.setOriginalUserId(originalUser.getId());
                            res.setOriginalUsername(originalUser.getUserName());
                            res.setOriginalFullName(originalUser.getFullName());
                            res.setOriginalAvatarUrl(originalUser.getAvatarUrl());
                        }
                    }
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByUserId(String userId, String currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Post> posts = postRepository.findByUserIdAndRepostOfIsNullOrderByCreatedAtDesc(userId);

        return posts.stream()
                .map(post -> {
                    String originalId = getOriginalId(post);
                    long repostCount = postRepository.countByRepostOf_Id(originalId);
                    long commentCount = commentRepository.countByPostId(originalId);
                    long likeCount = likeRepository.countByPostId(originalId);
                    boolean likedByCurrentUser = false;
                    if (currentUserId != null) {
                        likedByCurrentUser = likeRepository.existsByUserIdAndPostId(currentUserId, originalId);
                    }
                    boolean repostedByCurrentUser = false;
                    if (currentUserId != null) {
                        repostedByCurrentUser = postRepository
                                .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
                    }

                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    res.setLikeCount(likeCount);
                    res.setRepostCount(repostCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    res.setRepostedByCurrentUser(repostedByCurrentUser);
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByUsername(String username, String currentUserId) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Post> posts = postRepository.findByUserIdAndRepostOfIsNullOrderByCreatedAtDesc(user.getId());

        return posts.stream()
                .map(post -> {
                    String originalId = getOriginalId(post);
                    long repostCount = postRepository.countByRepostOf_Id(originalId);
                    long commentCount = commentRepository.countByPostId(originalId);
                    long likeCount = likeRepository.countByPostId(originalId);

                    boolean likedByCurrentUser = false;
                    if (currentUserId != null) {
                        likedByCurrentUser = likeRepository.existsByUserIdAndPostId(currentUserId, originalId);
                    }
                    boolean repostedByCurrentUser = false;
                    if (currentUserId != null) {
                        repostedByCurrentUser = postRepository
                                .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
                    }
                    PostResponse res = postMapper.toResponse(post, user);
                    res.setCommentCount(commentCount);
                    res.setLikeCount(likeCount);
                    res.setRepostCount(repostCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    res.setRepostedByCurrentUser(repostedByCurrentUser);
                    return res;
                })
                .collect(Collectors.toList());
    }

    public PostResponse getPostById(String postId, String currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String originalId = getOriginalId(post);
        long repostCount = postRepository.countByRepostOf_Id(originalId);
        long commentCount = commentRepository.countByPostId(originalId);
        long likeCount = likeRepository.countByPostId(originalId);
        boolean likedByCurrentUser = false;
        if (currentUserId != null) {
            likedByCurrentUser = likeRepository
                    .existsByUserIdAndPostId(currentUserId, originalId);
        }
        boolean repostedByCurrentUser = false;
        if (currentUserId != null) {
            repostedByCurrentUser = postRepository
                    .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
        }

        PostResponse res = postMapper.toResponse(post, user);
        res.setCommentCount(commentCount);
        res.setLikeCount(likeCount);
        res.setRepostCount(repostCount);
        res.setLikedByCurrentUser(likedByCurrentUser);
        res.setRepostedByCurrentUser(repostedByCurrentUser);

        if (post.getRepostOf() != null) {
            Post original = post.getRepostOf();
            User originalUser = userRepository.findById(original.getUserId())
                    .orElse(null);
            if (originalUser != null) {
                res.setOriginalUserId(originalUser.getId());
                res.setOriginalUsername(originalUser.getUserName());
                res.setOriginalFullName(originalUser.getFullName());
                res.setOriginalAvatarUrl(originalUser.getAvatarUrl());
            }
        }
        return res;
    }

    // Repost
    public PostResponse repost(String currentUserId, String originalPostId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post original = postRepository.findById(originalPostId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (postRepository.existsByUserIdAndRepostOf_Id(currentUserId, originalPostId)) {
            throw new RuntimeException("Bạn đã repost bài này rồi");
        }

        Post repost = Post.builder()
                .userId(currentUser.getId())
                .content(original.getContent())
                .scope(original.getScope())
                .createdAt(LocalDateTime.now())
                .repostOf(original)
                .build();

        if (original.getMediaList() != null && !original.getMediaList().isEmpty()) {
            for (Media m : original.getMediaList()) {
                Media cloned = Media.builder()
                        .mediaUrl(m.getMediaUrl())
                        .mediaPublicId(m.getMediaPublicId())
                        .mediaType(m.getMediaType())
                        .build();
                repost.addMedia(cloned);
            }
        }
        Post saved = postRepository.save(repost);
        String originalId = getOriginalId(saved);
        long repostCount = postRepository.countByRepostOf_Id(originalId);
        long commentCount = commentRepository.countByPostId(originalId);
        long likeCount = likeRepository.countByPostId(originalId);
        boolean likedByCurrentUser = currentUserId != null &&
                likeRepository.existsByUserIdAndPostId(currentUserId, originalId);

        PostResponse res = postMapper.toResponse(saved, currentUser);
        res.setCommentCount(commentCount);
        res.setLikeCount(likeCount);
        res.setRepostCount(repostCount);
        res.setLikedByCurrentUser(likedByCurrentUser);
        res.setRepostedByCurrentUser(true);

        User originalUser = userRepository.findById(original.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        res.setOriginalUserId(originalUser.getId());
        res.setOriginalUsername(originalUser.getUserName());
        res.setOriginalFullName(originalUser.getFullName());
        res.setOriginalAvatarUrl(originalUser.getAvatarUrl());

        return res;
    }

    public String unrepost(String currentUserId, String originalPostId) {
        Post repost = postRepository.findByUserIdAndRepostOf_Id(currentUserId, originalPostId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa repost bài này"));

        String repostId = repost.getId();
        postRepository.delete(repost);
        return repostId;
    }

    public List<PostResponse> getRepostsByUserId(String ownerId, String currentUserId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Post> posts = postRepository
                .findByUserIdAndRepostOfIsNotNullOrderByCreatedAtDesc(owner.getId());

        return posts.stream()
                .map(post -> {
                    String originalId = getOriginalId(post);
                    long repostCount = postRepository.countByRepostOf_Id(originalId);
                    long commentCount = commentRepository.countByPostId(originalId);
                    long likeCount = likeRepository.countByPostId(originalId);
                    boolean likedByCurrentUser = currentUserId != null &&
                            likeRepository.existsByUserIdAndPostId(currentUserId, originalId);
                    boolean repostedByCurrentUser = false;
                    if (currentUserId != null) {
                        repostedByCurrentUser = postRepository
                                .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
                    }

                    PostResponse res = postMapper.toResponse(post, owner);
                    res.setCommentCount(commentCount);
                    res.setLikeCount(likeCount);
                    res.setRepostCount(repostCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    res.setRepostedByCurrentUser(repostedByCurrentUser);
                    Post original = post.getRepostOf();
                    if (original != null) {
                        User originalUser = userRepository.findById(original.getUserId())
                                .orElse(null);
                        if (originalUser != null) {
                            res.setOriginalUserId(originalUser.getId());
                            res.setOriginalUsername(originalUser.getUserName());
                            res.setOriginalFullName(originalUser.getFullName());
                            res.setOriginalAvatarUrl(originalUser.getAvatarUrl());
                        }
                    }

                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<PostResponse> getRepostsByUsername(String username, String currentUserId) {
        User owner = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Post> posts = postRepository
                .findByUserIdAndRepostOfIsNotNullOrderByCreatedAtDesc(owner.getId());

        return posts.stream()
                .map(post -> {
                    String originalId = getOriginalId(post);
                    long repostCount = postRepository.countByRepostOf_Id(originalId);
                    long commentCount = commentRepository.countByPostId(originalId);
                    long likeCount = likeRepository.countByPostId(originalId);
                    boolean likedByCurrentUser = currentUserId != null &&
                            likeRepository.existsByUserIdAndPostId(currentUserId, originalId);
                    boolean repostedByCurrentUser = false;
                    if (currentUserId != null) {
                        repostedByCurrentUser = postRepository
                                .existsByUserIdAndRepostOf_Id(currentUserId, originalId);
                    }

                    PostResponse res = postMapper.toResponse(post, owner);
                    res.setCommentCount(commentCount);
                    res.setLikeCount(likeCount);
                    res.setRepostCount(repostCount);
                    res.setLikedByCurrentUser(likedByCurrentUser);
                    res.setRepostedByCurrentUser(repostedByCurrentUser);

                    Post original = post.getRepostOf();
                    if (original != null) {
                        User originalUser = userRepository.findById(original.getUserId())
                                .orElse(null);
                        if (originalUser != null) {
                            res.setOriginalUserId(originalUser.getId());
                            res.setOriginalUsername(originalUser.getUserName());
                            res.setOriginalFullName(originalUser.getFullName());
                            res.setOriginalAvatarUrl(originalUser.getAvatarUrl());
                        }
                    }
                    return res;
                })
                .collect(Collectors.toList());
    }
}
