package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByParticipantsHash(String participantsHash);
}
