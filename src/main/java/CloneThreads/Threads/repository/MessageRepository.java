package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findAllByConversationIdOrderByCreatedAtDesc(String conversationId);
    java.util.Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);
}