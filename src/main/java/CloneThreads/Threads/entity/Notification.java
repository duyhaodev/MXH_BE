package CloneThreads.Threads.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(length = 20, nullable = false)
    String type;

    @Column(name = "from_user_id", nullable = false)
    UUID fromUserId;

    @Column(name = "post_id")
    UUID postId;

    @Column(name = "comment_id")
    UUID commentId;

    @Column(columnDefinition = "TEXT")
    String message;

    @Column(name = "is_read")
    Boolean isRead;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
