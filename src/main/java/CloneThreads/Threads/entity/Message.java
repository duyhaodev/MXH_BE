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
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "conversation_id", nullable = false)
    UUID conversationId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    @Column(name = "is_read")
    Boolean isRead;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
