package CloneThreads.Threads.mapper;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post, User user) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .scope(post.getScope())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())

                .userId(post.getUserId())
                .username(user != null ? user.getUserName() : null)
                .fullName(user != null ? user.getFullName() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .build();
    }
}
