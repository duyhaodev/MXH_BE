package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.UserCreationRequest;
import CloneThreads.Threads.dto.response.ApiResponse;
import CloneThreads.Threads.dto.response.UserResponse;
import CloneThreads.Threads.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping()
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PostMapping("/verify")
    ApiResponse<UserResponse> verifyUser(@RequestParam String email, @RequestParam String code) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.verifyUser(email, code))
                .build();
    }

    @PostMapping("/resend-otp")
    ApiResponse<String> resendOtp(@RequestParam String email) {
        userService.resendOtp(email);
        return ApiResponse.<String>builder()
                .result("OTP has been resent to your email")
                .build();
    }

    @GetMapping("/myInfo")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{username}")
    ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserByUsername(username))
                .build();
    }
    @PutMapping(
            path = "/editprofile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )

    public ApiResponse<UserResponse> editProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile avatar
    ) throws IOException {

        String username = jwt.getSubject();

        return ApiResponse.<UserResponse>builder()
                .result(userService.editProfile(username, fullName, bio, avatar))
                .build();
    }


}
