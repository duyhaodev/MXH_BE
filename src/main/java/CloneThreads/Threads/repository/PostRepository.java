package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface PostRepository extends JpaRepository<Post, String> {

@Query(
        value = """
                SELECT 
                    p.id,
                    p.user_id AS "userId",
                    p.content,
                    p.scope,
                    p.created_at AS "createdAt",
                    p.updated_at AS "updatedAt",

                    u.full_name AS "fullName",
                    u.user_name AS "username",
                    u.avatar_url AS "avatarUrl",

                    (
                        COALESCE(
                            json_agg(
                                json_build_object(
                                    'id', m.id,
                                    'mediaUrl', m.media_url,
                                    'mediaPublicId', m.media_public_id,
                                    'mediaType', m.media_type
                                )
                            ) FILTER (WHERE m.id IS NOT NULL),
                            '[]'::json
                        )
                    )::json AS "mediaList"

                FROM posts p
                JOIN users u ON p.user_id = u.id
                LEFT JOIN media m ON m.post_id = p.id
                WHERE p.content ILIKE CONCAT('%', :keyword, '%')
                GROUP BY p.id, u.full_name, u.user_name, u.avatar_url
                ORDER BY p.created_at DESC
                """,
        nativeQuery = true
)
List<Map<String, Object>> searchPosts(@Param("keyword") String keyword);

    long countByRepostOf_Id(String originalPostId);
    List<Post> findByUserIdAndRepostOfIsNotNullOrderByCreatedAtDesc(String userId);  //Lấy các bài repost
    List<Post> findByUserIdAndRepostOfIsNullOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndRepostOf_Id(String userId, String repostOfId);
    java.util.Optional<Post> findByUserIdAndRepostOf_Id(String userId, String repostOfId);
    void deleteByRepostOf_Id(String originalPostId);
}
