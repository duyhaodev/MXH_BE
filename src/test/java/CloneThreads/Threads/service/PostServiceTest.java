package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.CommentRepository;
import CloneThreads.Threads.repository.LikeRepository;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostMapper postMapper;
    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeRepository likeRepository;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createPost_Success_NoMedia() throws IOException {
        // Arrange
        String userId = "user-1";
        String content = "Hello World";
        User user = User.builder().id(userId).build();
        Post savedPost = Post.builder().id("post-1").userId(userId).content(content).build();
        PostResponse response = PostResponse.builder().id("post-1").content(content).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toResponse(savedPost, user)).thenReturn(response);
        when(commentRepository.countByPostId("post-1")).thenReturn(0L);

        // Act
        PostResponse result = postService.create(userId, content, null);

        // Assert
        assertNotNull(result);
        assertEquals("post-1", result.getId());
        assertEquals(content, result.getContent());
        verify(postRepository).save(any(Post.class));
    }
    
    @Test
    void createPost_Success_WithMedia() throws IOException {
        // Arrange
        String userId = "user-1";
        List<MultipartFile> files = List.of(mock(MultipartFile.class));
        User user = User.builder().id(userId).build();
        Post savedPost = Post.builder().id("post-1").userId(userId).build();
        PostResponse response = PostResponse.builder().id("post-1").build();

        // Mock MultipartFile
        when(files.get(0).isEmpty()).thenReturn(false);
        when(files.get(0).getContentType()).thenReturn("image/png");
        when(files.get(0).getBytes()).thenReturn(new byte[]{1, 2, 3});

        // Mock Cloudinary
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), any(Map.class))).thenReturn(Map.of("secure_url", "http://url", "public_id", "pid"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toResponse(savedPost, user)).thenReturn(response);

        // Act
        PostResponse result = postService.create(userId, "content", files);

        // Assert
        assertNotNull(result);
        verify(uploader).upload(any(), any(Map.class));
        verify(postRepository).save(any(Post.class));
    }


    @Test
    void deletePost_Success() {
        // Arrange
        String userId = "user-1";
        String postId = "post-1";
        Post post = Post.builder().id(postId).userId(userId).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        
        // Act
        postService.deletePost(userId, postId);

        // Assert
        verify(postRepository).delete(post);
        verify(postRepository).deleteByRepostOf_Id(postId);
    }

    @Test
    void deletePost_Fail_NotOwner() {
        // Arrange
        String userId = "user-1";
        String otherUserId = "user-2";
        String postId = "post-1";
        Post post = Post.builder().id(postId).userId(otherUserId).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> postService.deletePost(userId, postId));
        
        assertEquals("Bạn không có quyền xóa bài này", exception.getMessage());
        verify(postRepository, never()).delete(any());
    }
}
