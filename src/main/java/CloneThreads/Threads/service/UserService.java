package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.UserCreationRequest;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.mapper.UserMapper;
import CloneThreads.Threads.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse createUser(UserCreationRequest request){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new AppException("email is already exists");
        }

        User user = userMapper.toUser(request);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return userMapper.toUserResponse(userRepository.save(user));
    }
}
