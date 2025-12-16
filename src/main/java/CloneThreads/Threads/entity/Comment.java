package CloneThreads.Threads.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "post_id", nullable = false, length = 36)
    String postId;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "parent_id", length = 36)
    String parentId;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "comment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    List<Media> mediaList = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addMedia(Media media) {
        if (mediaList == null) {
            mediaList = new ArrayList<>();
        }
        media.setComment(this);
        mediaList.add(media);
    }
}
