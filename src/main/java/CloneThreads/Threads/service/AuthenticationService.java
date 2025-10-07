package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.request.AuthenticationRequest;
import CloneThreads.Threads.dto.response.AuthenticationResponse;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("User not found"));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!authenticated) {
            throw new AppException("Wrong password");
        }

        return AuthenticationResponse.builder()
                .authenticated(true)
                .build();
    }
}
