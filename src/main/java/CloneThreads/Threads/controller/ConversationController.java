package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.ConversationRequest;
import CloneThreads.Threads.dto.response.ApiResponse;
import CloneThreads.Threads.dto.response.ConversationResponse;
import CloneThreads.Threads.service.ConversationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;

    @PostMapping("/create")
    ApiResponse<ConversationResponse> createConversation(@RequestBody ConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.create(request))
                .build();
    }

    @GetMapping("/my-conversations")
    ApiResponse<List<ConversationResponse>> myConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(conversationService.myConversations())
                .build();
    }

    @PutMapping("/mark-as-read/{conversationId}")
    ApiResponse<Boolean> markAsRead(@PathVariable String conversationId) {
        boolean success = conversationService.markAsRead(conversationId);
        return ApiResponse.<Boolean>builder()
                .result(success)
                .build();
    }
}
