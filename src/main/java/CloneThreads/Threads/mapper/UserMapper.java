package CloneThreads.Threads.mapper;

import CloneThreads.Threads.dto.request.UserCreationRequest;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);
}
