package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.IntrospectRequest;
import CloneThreads.Threads.entity.WebSocketSession;
import CloneThreads.Threads.repository.UserRepository;
import CloneThreads.Threads.repository.WebSocketSessionRepository;
import CloneThreads.Threads.service.AuthenticationService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketHandler {
    SocketIOServer server;
    AuthenticationService authenticationService;
    WebSocketSessionRepository webSocketSessionRepository;
    UserRepository userRepository;

    @OnConnect
    public void clientConnected(SocketIOClient client) {
        try {
            // Get token from request param
            String token = client.getHandshakeData().getSingleUrlParam("token");

            // Verify token
            var introspectResponse = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build()
            );

            // If token is invalid => disconnect
            if (introspectResponse != null && introspectResponse.isValid()) {
                String username = introspectResponse.getUserId(); // This is actually username from JWT subject
                
                var userOptional = userRepository.findByUserName(username);
                if (userOptional.isEmpty()) {
                    log.error("User not found for username: {}", username);
                    client.disconnect();
                    return;
                }
                
                String userUuid = userOptional.get().getId();
                // Persist webSocketSession
                WebSocketSession webSocketSession = WebSocketSession.builder()
                        .socketSessionId(client.getSessionId().toString())
                        .userId(userUuid) // Save UUID instead of username
                        .createdAt(LocalDateTime.now())
                        .build();

                webSocketSessionRepository.save(webSocketSession);

            } else {
                log.error("Authenticated fail: {}", client.getSessionId());
                client.disconnect();
            }
        } catch (Exception e) {
            log.error("Connection authentication error: {}", e.getMessage());
            client.disconnect();
        }
    }

    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        webSocketSessionRepository.deleteBySocketSessionId(client.getSessionId().toString());
    }

    @PostConstruct
    public void startServer() {
        server.start();
        server.addListeners(this);
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
    }
}
