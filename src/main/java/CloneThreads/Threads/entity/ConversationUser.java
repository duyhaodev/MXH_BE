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
@Table(name = "conversation_users")
public class ConversationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "conversation_id", nullable = false)
    UUID conversationId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "is_admin")
    Boolean isAdmin;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
