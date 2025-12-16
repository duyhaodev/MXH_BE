package CloneThreads.Threads.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationRequest {
    List<String> participantIds;
    String type; // DIRECT, GROUP
}
