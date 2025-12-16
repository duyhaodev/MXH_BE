package CloneThreads.Threads.mapper;

import CloneThreads.Threads.dto.response.MediaResponse;
import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "scope", source = "post.scope")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    @Mapping(target = "mediaList", source = "post.mediaList")
    @Mapping(target = "repostOfId", source = "post.repostOf.id")

    // user
    @Mapping(target = "userId", source = "post.userId")
    @Mapping(target = "username", source = "user.userName")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")

    PostResponse toResponse(Post post, User user);

    MediaResponse toMediaResponse(Media media);
}
