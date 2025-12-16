package CloneThreads.Threads.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {
    String id;
    String type;
    String conversationName;
    String conversationAvatar;
    Boolean unread;
    String lastMessageContent;
    LocalDateTime lastMessageTimestamp;
    LocalDateTime createdAt;
}
