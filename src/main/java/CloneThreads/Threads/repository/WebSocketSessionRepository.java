package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.WebSocketSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WebSocketSessionRepository extends JpaRepository<WebSocketSession, String> {
    @Transactional
    void deleteBySocketSessionId(String socketSessionId);
    List<WebSocketSession> findAllByUserIdIn(List<String> userIds);

    @Query("SELECT DISTINCT w.userId FROM WebSocketSession w")
    List<String> findAllActiveUserIds();

    long countByUserId(String userId);
}
