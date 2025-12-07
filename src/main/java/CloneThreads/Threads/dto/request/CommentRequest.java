package CloneThreads.Threads.dto.request;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private String parentId;
}
