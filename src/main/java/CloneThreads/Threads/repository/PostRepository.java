package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface PostRepository extends JpaRepository<Post, String> {

//	@Query(
//	        value = "SELECT * FROM posts WHERE content ILIKE CONCAT('%', :keyword, '%')",
//	        nativeQuery = true
//	    )
//    List<Post> searchPosts(@Param("keyword") String keyword);
	@Query(
		    value = "SELECT p.*, u.full_name as authorName, u.username as authorUsername " +
		            "FROM posts p " +
		            "JOIN users u ON p.user_id = u.id " +
		            "WHERE p.content ILIKE CONCAT('%', :keyword, '%')",
		    nativeQuery = true
		)
	List<Map<String, Object>> searchPosts(@Param("keyword") String keyword);
}
