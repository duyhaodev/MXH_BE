package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.id = :notificationId AND n.userId = :userId")
    boolean existsByIdAndUserId(@Param("notificationId") String notificationId, @Param("userId") String userId);

    long countByUserIdAndIsReadFalse(String userId);
}