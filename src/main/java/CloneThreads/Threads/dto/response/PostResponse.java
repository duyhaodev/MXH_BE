package CloneThreads.Threads.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private String id;
    private String content;
    private String mediaUrl;
    private String mediaType;
    private String scope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // thông tin người đăng
    private String userId;
    private String username;
    private String fullName;
    private String avatarUrl;
}
