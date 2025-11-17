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
			value = "SELECT p.id, p.user_id AS \"userId\", p.content, " +
					"p.media_type AS \"mediaType\", p.media_url AS \"mediaUrl\", p.media_public_id AS \"mediaPublicId\", " +
					"p.scope, p.created_at AS \"createdAt\", p.updated_at AS \"updatedAt\", " +
					"u.full_name AS \"fullName\", u.user_name AS \"username\", u.avatar_url AS \"avatarUrl\" " +
					"FROM posts p " +
					"JOIN users u ON p.user_id = u.id " +
					"WHERE p.content ILIKE CONCAT('%', :keyword, '%')",
			nativeQuery = true
	)
	List<Map<String, Object>> searchPosts(@Param("keyword") String keyword);
}
