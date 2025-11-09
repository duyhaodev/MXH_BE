package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    @Query(
            value = "SELECT * FROM users WHERE username ILIKE CONCAT('%', :keyword, '%') OR full_name ILIKE CONCAT('%', :keyword, '%')",
            nativeQuery = true
        )
     List<User> searchUsers(@Param("keyword") String keyword);
}
