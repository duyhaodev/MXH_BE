package CloneThreads.Threads.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private String id;
    private String content;
    private String scope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    List<MediaResponse> mediaList;

    // thông tin người đăng
    private String userId;
    private String username;
    private String fullName;
    private String avatarUrl;

    private Long commentCount;
    private Long likeCount;
    private Boolean likedByCurrentUser; // user hiện tại đã like chưa
}
