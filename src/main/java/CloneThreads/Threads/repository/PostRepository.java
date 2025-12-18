package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface PostRepository extends JpaRepository<Post, String> {

    @Query("""
    SELECT p
    FROM Post p
    WHERE p.repostOf IS NULL
      AND LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY p.createdAt DESC
""")
    List<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    long countByRepostOf_Id(String originalPostId);
    List<Post> findByUserIdAndRepostOfIsNotNullOrderByCreatedAtDesc(String userId);  //Lấy các bài repost
    List<Post> findByUserIdAndRepostOfIsNullOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndRepostOf_Id(String userId, String repostOfId);
    java.util.Optional<Post> findByUserIdAndRepostOf_Id(String userId, String repostOfId);
    void deleteByRepostOf_Id(String originalPostId);
}
