package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.MessageRequest;
import CloneThreads.Threads.dto.response.ApiResponse;
import CloneThreads.Threads.dto.response.MessageResponse;
import CloneThreads.Threads.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageController {
    MessageService messageService;

    @PostMapping("/create")
    ApiResponse<MessageResponse> create(@RequestBody MessageRequest request) {
        return ApiResponse.<MessageResponse>builder()
                .result(messageService.create(request))
                .build();
    }

    @GetMapping("/my-conversations")
    ApiResponse<List<MessageResponse>> getMessages(@RequestParam("conversationId") String conversationId) {
        return ApiResponse.<List<MessageResponse>>builder()
                .result(messageService.getMessages(conversationId))
                .build();
    }
}
