package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.ConversationRequest;
import CloneThreads.Threads.dto.response.ConversationResponse;
import CloneThreads.Threads.entity.Conversation;
import CloneThreads.Threads.entity.ConversationUser;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.dto.request.ConversationRequest;
import CloneThreads.Threads.dto.response.ConversationResponse;
import CloneThreads.Threads.entity.Conversation;
import CloneThreads.Threads.entity.ConversationUser;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.repository.ConversationRepository;
import CloneThreads.Threads.repository.ConversationUserRepository;
import CloneThreads.Threads.repository.MessageRepository;
import CloneThreads.Threads.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    ConversationUserRepository conversationUserRepository;
    UserRepository userRepository;
    MessageRepository messageRepository;

    @Transactional
    public ConversationResponse create(ConversationRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Giả sử request gửi lên ID của người muốn chat
        String targetUserId = request.getParticipantIds().getFirst();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tạo hash để kiểm tra hội thoại 1-1 đã tồn tại chưa
        List<String> ids = Arrays.asList(currentUser.getId(), targetUser.getId());
        Collections.sort(ids);
        String hash = String.join("_", ids);

        Optional<Conversation> existingConv = conversationRepository.findByParticipantsHash(hash);
        if (existingConv.isPresent()) {
            ConversationUser cu = conversationUserRepository.findByConversationIdAndUserId(existingConv.get().getId(), currentUser.getId())
                    .orElse(null);
            Boolean unread = cu != null ? cu.getUnread() : false;
            return toConversationResponse(existingConv.get(), currentUser.getId(), unread);
        }

        // Tạo mới
        Conversation conversation = Conversation.builder()
                .type(request.getType()) // "DIRECT"
                .participantsHash(hash)
                .createdAt(LocalDateTime.now())
                .build();
        conversation = conversationRepository.save(conversation);

        // Lưu participants
        ConversationUser cu1 = ConversationUser.builder()
                .conversationId(conversation.getId())
                .userId(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .unread(false)
                .build();

        ConversationUser cu2 = ConversationUser.builder()
                .conversationId(conversation.getId())
                .userId(targetUser.getId())
                .createdAt(LocalDateTime.now())
                .unread(false)
                .build();

        conversationUserRepository.saveAll(List.of(cu1, cu2));

        return toConversationResponse(conversation, currentUser.getId(), false);
    }

    public List<ConversationResponse> myConversations() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<ConversationUser> cus = conversationUserRepository.findAllByUserId(currentUser.getId());

        
        return cus.stream()
                .map(cu -> {
                    // Fetch lại conversation để đảm bảo lấy được data mới nhất nếu cần
                     return conversationRepository.findById(cu.getConversationId())
                             .map(conv -> toConversationResponse(conv, currentUser.getId(), cu.getUnread()))
                             .orElse(null);
                })
                .filter(Objects::nonNull)
                // Sort by last message time (newest first), if needed. Assuming frontend handles it or we sort here.
                // Let's sort here for better UX
                .sorted((c1, c2) -> {
                    LocalDateTime t1 = c1.getLastMessageTimestamp() != null ? c1.getLastMessageTimestamp() : c1.getCreatedAt();
                    LocalDateTime t2 = c2.getLastMessageTimestamp() != null ? c2.getLastMessageTimestamp() : c2.getCreatedAt();
                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());
    }

    public boolean markAsRead(String conversationId) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUserName(currentUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            ConversationUser cu = conversationUserRepository.findByConversationIdAndUserId(conversationId, currentUser.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

            cu.setUnread(false);
            conversationUserRepository.save(cu);
            return true;
        } catch (AppException e) {
            log.error("Failed to mark conversation as read: {}", e.getMessage());
            // Optionally, re-throw or handle specific error codes if needed by the controller
            return false;
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, String currentUserId, Boolean unread) {
        ConversationResponse response = ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .createdAt(conversation.getCreatedAt())
                .unread(unread)
                .build();

        // Populate last message info
        messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .ifPresentOrElse(msg -> {
                    response.setLastMessageContent(msg.getContent());
                    response.setLastMessageTimestamp(msg.getCreatedAt());
                }, () -> {
                    log.debug("No messages found for conversation {}", conversation.getId());
                });

        // Tìm người kia để lấy tên và avatar
        // (Logic này có thể tối ưu hơn bằng query custom, nhưng tạm thời dùng logic này cho đơn giản)
        List<ConversationUser> participants = conversationUserRepository.findAllByConversationId(conversation.getId());
        
        participants.stream()
                .filter(p -> !p.getUserId().equals(currentUserId))
                .findFirst()
                .ifPresent(partner -> {
                    // Cần lấy thông tin User của partner. 
                    // Vì ConversationUser đã cấu hình @ManyToOne User, ta có thể dùng user đó (Lazy load)
                    // Tuy nhiên, ConversationUser trong hàm này đang được fetch EAGER hoặc cần query lại nếu session đóng.
                    // Để an toàn và tận dụng cache cấp 1, ta dùng userRepository findById nếu obj user chưa load.
                    
                    User u = partner.getUser();
                    if(u == null) {
                         u = userRepository.findById(partner.getUserId()).orElse(null);
                    }
                    
                    if (u != null) {
                        response.setConversationName(u.getUserName()); // Hoặc fullName
                        response.setConversationAvatar(u.getAvatarUrl());
                    }
                });

        return response;
    }
}
