package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.LikeResponse;
import CloneThreads.Threads.entity.Comment;
import CloneThreads.Threads.entity.Like;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.repository.CommentRepository;
import CloneThreads.Threads.repository.LikeRepository;
import CloneThreads.Threads.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public LikeResponse togglePostLike(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post không tồn tại"));

        boolean alreadyLiked = likeRepository.existsByUserIdAndPostId(userId, post.getId());
        if (alreadyLiked) {
            likeRepository.deleteByUserIdAndPostId(userId, post.getId());
        } else {
            Like like = Like.builder()
                    .userId(userId)
                    .postId(post.getId())
                    .commentId(null)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(like);

            // Tạo notification (nếu không phải self-like)
            if (!post.getUserId().equals(userId)) {
                notificationService.createLikePostNotification(post.getUserId(), userId, post.getId());
            }
        }

        long count = likeRepository.countByPostId(post.getId());
        return LikeResponse.builder()
                .liked(!alreadyLiked)
                .likeCount(count)
                .build();
    }

    @Transactional
    // ===== LIKE / UNLIKE COMMENT =====
    public LikeResponse toggleCommentLike(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment không tồn tại"));

        boolean alreadyLiked = likeRepository.existsByUserIdAndCommentId(userId, comment.getId());

        if (alreadyLiked) {
            // UNLIKE
            likeRepository.deleteByUserIdAndCommentId(userId, comment.getId());
        } else {
            // LIKE
            Like like = Like.builder()
                    .userId(userId)
                    .postId(comment.getPostId())
                    .commentId(comment.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(like);
            // Tạo notification like (nếu không phải self-like)
            if (!comment.getUserId().equals(userId)) {
                notificationService.createLikeCommentNotification(comment.getUserId(), userId, comment.getId(), comment.getPostId());
            }
        }

        long count = likeRepository.countByCommentId(comment.getId());

        return LikeResponse.builder()
                .liked(!alreadyLiked)
                .likeCount(count)
                .build();
    }
}
