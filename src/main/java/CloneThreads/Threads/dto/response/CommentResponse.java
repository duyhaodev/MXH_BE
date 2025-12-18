package CloneThreads.Threads.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommentResponse {
    private String id;
    private String postId;
    private String userId;

    private String content;
    private String parentId;
    private String createdAt;

    private String userName;
    private String fullName;
    private String avatarUrl;

    private long likeCount;
    private boolean likedByCurrentUser;

    private List<MediaResponse> mediaList;
}
