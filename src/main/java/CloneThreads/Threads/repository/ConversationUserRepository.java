package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.ConversationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationUserRepository extends JpaRepository<ConversationUser, String> {
    List<ConversationUser> findAllByUserId(String userId);
    List<ConversationUser> findAllByConversationId(String conversationId);
    boolean existsByConversationIdAndUserId(String conversationId, String userId);
    Optional<ConversationUser> findByConversationIdAndUserId(String conversationId, String userId);
}
