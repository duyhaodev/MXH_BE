package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.MessageRequest;
import CloneThreads.Threads.dto.response.MessageResponse;
import CloneThreads.Threads.entity.ConversationUser;
import CloneThreads.Threads.entity.Message;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.entity.WebSocketSession;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.repository.ConversationRepository;
import CloneThreads.Threads.repository.ConversationUserRepository;
import CloneThreads.Threads.repository.MessageRepository;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.repository.WebSocketSessionRepository;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {
    MessageRepository messageRepository;
    ConversationUserRepository conversationUserRepository;
    ConversationRepository conversationRepository;
    UserRepository userRepository;
    WebSocketSessionRepository webSocketSessionRepository;
    SocketIOServer socketIOServer;
    ObjectMapper objectMapper;

    public List<MessageResponse> getMessages(String conversationId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate: User must be a participant of the conversation
        boolean isParticipant = conversationUserRepository.existsByConversationIdAndUserId(conversationId, currentUser.getId());
        if (!isParticipant) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc tạo ErrorCode.FORBIDDEN_ACCESS
        }

        List<Message> messages = messageRepository.findAllByConversationIdOrderByCreatedAtDesc(conversationId);

        return messages.stream()
                .map(message -> toMessageResponse(message, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse create(MessageRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate conversation exists
        if (!conversationRepository.existsById(request.getConversationId())) {
             throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // ErrorCode.CONVERSATION_NOT_FOUND
        }

        // Validate user is participant
        boolean isParticipant = conversationUserRepository.existsByConversationIdAndUserId(request.getConversationId(), currentUser.getId());
        if (!isParticipant) {
             throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Mark conversation as unread for other participants
        List<ConversationUser> participants = conversationUserRepository.findAllByConversationId(request.getConversationId());
        List<ConversationUser> others = participants.stream()
                .filter(cu -> !cu.getUserId().equals(currentUser.getId()))
                .peek(cu -> cu.setUnread(true))
                .collect(Collectors.toList());
        conversationUserRepository.saveAll(others);

        // Create Message
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .user(currentUser) // Refactored: Use user object directly
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        // Save
        message = messageRepository.save(message);

        // Manual wiring for response optimization
        message.setUser(currentUser);
        MessageResponse createdMessageResponse = toMessageResponse(message, currentUser.getId());

                List<String> participantUserIds = conversationUserRepository.findAllByConversationId(request.getConversationId())
                        .stream()
                        .map(ConversationUser::getUserId)
                        .toList();
        
                var sessions = webSocketSessionRepository.findAllByUserIdIn(participantUserIds);
        
                Map<String, WebSocketSession> webSocketSessions =
                        sessions.stream()
                                .collect(Collectors.toMap(
                                        WebSocketSession::getSocketSessionId,
                                        Function.identity()
                                ));
        
                webSocketSessions.values().forEach(session -> {
                    try {
                        UUID clientUUID = UUID.fromString(session.getSocketSessionId());
                        SocketIOClient client = socketIOServer.getClient(clientUUID);
                        
                        if (client != null) {
                            // Update 'isMe' for the specific recipient
                            createdMessageResponse.setMe(session.getUserId().equals(currentUser.getId()));
                            String messageJson = objectMapper.writeValueAsString(createdMessageResponse);
                            
                            client.sendEvent("message", messageJson);
                        }
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID format for session: {}", session.getSocketSessionId());
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing message for socket", e);
                    }
                });
        // Reset 'isMe' to true for the sender's HTTP response
        createdMessageResponse.setMe(true);
        return createdMessageResponse;
    }

    /**
     * Helper method similar to chat-service
     * Centralizes the logic for mapping User info and calculating 'isMe'
     */
    private MessageResponse toMessageResponse(Message message, String currentUserId) {
        User sender = message.getUser();

        MessageResponse.SenderInfo senderInfo = null;
        if (sender != null) {
            senderInfo = MessageResponse.SenderInfo.builder()
                    .id(sender.getId())
                    .userName(sender.getUserName())
                    .fullName(sender.getFullName())
                    .avatarUrl(sender.getAvatarUrl())
                    .build();
        }

        // Refactored: Use sender.getId() instead of message.getUserId()
        boolean isMe = sender != null && currentUserId.equals(sender.getId());

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .isMe(isMe)
                .sender(senderInfo)
                .build();
    }
}