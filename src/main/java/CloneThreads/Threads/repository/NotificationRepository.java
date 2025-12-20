package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.id = :notificationId AND n.userId = :userId")
    boolean existsByIdAndUserId(@Param("notificationId") String notificationId, @Param("userId") String userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.fromUserId = :fromUserId AND n.type = :type")
    Optional<Notification> findExistFollowNotification(@Param("userId") String userId, @Param("fromUserId") String fromUserId, @Param("type") String type);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.fromUserId = :fromUserId AND n.type = :type AND n.postId = :postId")
    Optional<Notification> findExistForPostNotification(@Param("userId") String userId, @Param("fromUserId") String fromUserId, @Param("type") String type, @Param("postId") String postId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.fromUserId = :fromUserId AND n.type = :type AND n.commentId = :commentId")
    Optional<Notification> findExistForCommentNotification(@Param("userId") String userId, @Param("fromUserId") String fromUserId, @Param("type") String type, @Param("commentId") String commentId);

    long countByUserIdAndIsReadFalse(String userId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") String userId);
}