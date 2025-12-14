package CloneThreads.Threads.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    String id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(length = 20, nullable = false)
    String type;

    @Column(name = "from_user_id", nullable = false)
    String fromUserId;

    @Column(name = "post_id")
    String postId;

    @Column(name = "comment_id")
    String commentId;

    @Column(columnDefinition = "TEXT")
    String message;

    @Column(name = "is_read")
    Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now().withNano(0);
        }
    }
}