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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true, length = 255)
    String email;

    @Column(nullable = false, unique = true, length = 50)
    String username;

    @Column(name = "password_hash")
    String passwordHash;

    @Column(name = "full_name")
    String fullName;

    String bio;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "profile_link")
    String profileLink;

    @Column(name = "oauth_provider")
    String oauthProvider;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
