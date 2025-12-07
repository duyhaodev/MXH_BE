package CloneThreads.Threads.mapper;

import CloneThreads.Threads.dto.response.CommentResponse;
import CloneThreads.Threads.dto.response.MediaResponse;
import CloneThreads.Threads.entity.Comment;
import CloneThreads.Threads.entity.Media;
import CloneThreads.Threads.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "postId", source = "comment.postId")
    @Mapping(target = "userId", source = "comment.userId")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "parentId", source = "comment.parentId")
    @Mapping(target = "createdAt", source = "comment.createdAt")

    // map user info
    @Mapping(target = "userName", source = "user.userName")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")

    // map list media
    @Mapping(target = "mediaList", source = "comment.mediaList")
    CommentResponse toResponse(Comment comment, User user);

    MediaResponse toMediaResponse(Media media);
}
