package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);

    long countByPostId(String postId);
}
