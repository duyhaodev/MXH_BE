package CloneThreads.Threads.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LikeResponse {
    boolean liked;
    long likeCount;
}
