package CloneThreads.Threads.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {
    private String content;
    private String mediaUrl;
    private String mediaType;
    private String scope;
}
