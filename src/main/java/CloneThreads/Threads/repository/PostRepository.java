package CloneThreads.Threads.repository;

import CloneThreads.Threads.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {}
