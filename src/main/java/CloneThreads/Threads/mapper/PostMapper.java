package CloneThreads.Threads.mapper;

import CloneThreads.Threads.dto.response.MediaResponse;
import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post, User user) {
        // map list media từ entity -> dto
        List<MediaResponse> mediaList = (post.getMediaList() == null)
                ? Collections.emptyList()
                : post.getMediaList().stream()
                .map(this::toMediaResponse)
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .scope(post.getScope())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .mediaList(mediaList)

                // thông tin user
                .userId(post.getUserId())
                .username(user != null ? user.getUserName() : null)
                .fullName(user != null ? user.getFullName() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .build();
    }

    private MediaResponse toMediaResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .mediaPublicId(media.getMediaPublicId())
                .mediaType(media.getMediaType())
                .build();
    }
}
