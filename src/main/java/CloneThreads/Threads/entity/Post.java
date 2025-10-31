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
@Table(name = "posts")
public class Post {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "media_type", length = 20)
    String mediaType;

    @Column(name = "media_url")
    String mediaUrl;

    @Column(length = 20)
    String scope;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
