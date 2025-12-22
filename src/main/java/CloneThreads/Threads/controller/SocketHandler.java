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
import java.util.List;

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
                
                // Store userId in client attributes for easy access on disconnect
                client.set("userId", userUuid);

                // Check if user is already online (has other sessions)
                boolean wasAlreadyOnline = webSocketSessionRepository.countByUserId(userUuid) > 0;

                // Persist webSocketSession
                WebSocketSession webSocketSession = WebSocketSession.builder()
                        .socketSessionId(client.getSessionId().toString())
                        .userId(userUuid)
                        .createdAt(LocalDateTime.now())
                        .build();

                webSocketSessionRepository.save(webSocketSession);

                // 1. Send CURRENT online user list to the newly connected user
                List<String> onlineUserIds = webSocketSessionRepository.findAllActiveUserIds();
                client.sendEvent("online_users_list", onlineUserIds);

                // 2. Broadcast to ALL others that this user is now online (only if it's their first session)
                if (!wasAlreadyOnline) {
                    server.getBroadcastOperations().sendEvent("user_status_change", 
                        java.util.Map.of("userId", userUuid, "status", "online"));
                }

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
        String userId = client.get("userId");
        String sessionId = client.getSessionId().toString();
        
        webSocketSessionRepository.deleteBySocketSessionId(sessionId);

        if (userId != null) {
            // Check if this was the last session
            boolean isStillOnline = webSocketSessionRepository.countByUserId(userId) > 0;
            
            if (!isStillOnline) {
                // Broadcast to ALL that this user is now offline
                server.getBroadcastOperations().sendEvent("user_status_change", 
                    java.util.Map.of("userId", userId, "status", "offline"));
            }
        }
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
