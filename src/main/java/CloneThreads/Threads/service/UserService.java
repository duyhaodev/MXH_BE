package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.UserCreationRequest;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.mapper.UserMapper;
import CloneThreads.Threads.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    private final UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;
    private final EmailService emailService;

    public UserResponse createUser(UserCreationRequest request){
        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByUserName(request.getUserName())){
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setProfileLink("@" + request.getUserName());
        user.setAvatarUrl("https://res.cloudinary.com/dqdivgrkz/image/upload/v1766129961/Gemini_Generated_Image_y5h7uy5h7uy5h7uy_r2wtrj.png");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // Generate OTP
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setVerificationCode(code);
        user.setVerificationAttempts(0);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        user.setEnabled(false);
        
        User savedUser = userRepository.save(user);
        
        // Send Email
        emailService.sendVerificationEmail(savedUser.getEmail(), code);

        return userMapper.toUserResponse(savedUser);
    }


    public UserResponse verifyUser(String email, String code) {
        User user = userRepository.findByEmail(email) 
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isEnabled()) {
            return userMapper.toUserResponse(user);
        }

        // Check if blocked (Max attempts)
        int attempts = user.getVerificationAttempts() == null ? 0 : user.getVerificationAttempts();
        if (attempts >= 5) {
             throw new AppException(ErrorCode.MAX_OTP_ATTEMPTS);
        }

        // Check expiry
        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (code != null && code.equals(user.getVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationAttempts(0);
            return userMapper.toUserResponse(userRepository.save(user));
        } else {
            // Increment fail attempts
            user.setVerificationAttempts(attempts + 1);
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_OTP_KEY);
        }
    }

    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isEnabled()) {
            throw new RuntimeException("Account already verified");
        }

        // Generate new OTP
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setVerificationCode(code);
        user.setVerificationAttempts(0);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        
        userRepository.save(user);
        
        // Send Email
        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Generate OTP
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setVerificationCode(code);
        user.setVerificationAttempts(0);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        // Send Email
        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check attempts
        int attempts = user.getVerificationAttempts() == null ? 0 : user.getVerificationAttempts();
        if (attempts >= 5) {
            throw new AppException(ErrorCode.MAX_OTP_ATTEMPTS);
        }

        // Check expiry
        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Verify OTP
        if (otp != null && otp.equals(user.getVerificationCode())) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setVerificationCode(null);
            user.setVerificationAttempts(0);
            userRepository.save(user);
        } else {
            user.setVerificationAttempts(attempts + 1);
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_OTP_KEY);
        }
    }

    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        log.info(context.toString());
        String name = context.getAuthentication().getName();
        log.info("Username : {}", name);

        User user = userRepository.findByUserName(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    public UserResponse editProfile(String username, String fullName, String bio, MultipartFile avatar) throws IOException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (fullName != null) user.setFullName(fullName);
        if (bio != null) user.setBio(bio);

        if (avatar != null && !avatar.isEmpty()) {
            Map upload = cloudinary.uploader().upload(
                    avatar.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "threads/users/avatar",
                            "resource_type", "image"
                    )
            );
            String avatarUrl = (String) upload.get("secure_url");
            user.setAvatarUrl(avatarUrl);
        }

        User saved = userRepository.save(user);
        return userMapper.toUserResponse(saved);
    }

    public List<UserResponse> searchUser (String keyword){
        var context = SecurityContextHolder.getContext();
        String currentUsername = context.getAuthentication().getName();

        List<User> user = userRepository.searchUsers(keyword);

        return user.stream()
                .filter(u -> !u.getUserName().equals(currentUsername))
                .map(userMapper::toUserResponse)
                .toList();
    }

    public String getUserIdByUsername(String username) {
        return userRepository.findByUserName(username)
                .map(User::getId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Tăng followers cho user (khi ai đó follow họ)
    @Transactional
    public void incrementFollowers(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFollowersCount(user.getFollowersCount() + 1);
        userRepository.save(user);
    }

    // Giảm followers cho user (khi unfollow)
    @Transactional
    public void decrementFollowers(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        int newCount = Math.max(0, user.getFollowersCount() - 1);  // Không âm
        user.setFollowersCount(newCount);
        userRepository.save(user);
    }

    // Tăng following cho user (khi họ follow ai đó)
    @Transactional
    public void incrementFollowing(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFollowingCount(user.getFollowingCount() + 1);
        userRepository.save(user);
    }

    // Giảm following cho user (khi họ unfollow)
    @Transactional
    public void decrementFollowing(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        int newCount = Math.max(0, user.getFollowingCount() - 1);  // Không âm
        user.setFollowingCount(newCount);
        userRepository.save(user);
    }
}