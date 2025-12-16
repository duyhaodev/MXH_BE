package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, String> {

    long countByPostId(String postId);
    long countByCommentId(String commentId);

    boolean existsByUserIdAndPostId(String userId, String postId);
    boolean existsByUserIdAndCommentId(String userId, String commentId);

    void deleteByUserIdAndPostId(String userId, String postId);
    void deleteByUserIdAndCommentId(String userId, String commentId);
}
