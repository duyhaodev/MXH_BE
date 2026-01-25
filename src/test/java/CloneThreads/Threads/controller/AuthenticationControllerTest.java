package CloneThreads.Threads.controller;

import CloneThreads.Threads.dto.request.AuthenticationRequest;
import CloneThreads.Threads.dto.request.IntrospectRequest;
import CloneThreads.Threads.dto.request.LogoutRequest;
import CloneThreads.Threads.dto.response.AuthenticationResponse;
import CloneThreads.Threads.dto.response.IntrospectResponse;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import CloneThreads.Threads.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt Spring Security Filter để tập trung test logic Controller
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest authenticationRequest;
    private AuthenticationResponse authenticationResponse;
    private IntrospectRequest introspectRequest;
    private IntrospectResponse introspectResponse;
    private LogoutRequest logoutRequest;

    @BeforeEach
    void initData() {
        authenticationRequest = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        authenticationResponse = AuthenticationResponse.builder()
                .token("test-token")
                .authenticated(true)
                .build();

        introspectRequest = IntrospectRequest.builder()
                .token("test-token")
                .build();

        introspectResponse = IntrospectResponse.builder()
                .valid(true)
                .build();

        logoutRequest = LogoutRequest.builder()
                .token("test-token")
                .build();
    }

    @Test
    void authenticate_validRequest_success() throws Exception {
        // GIVEN
        Mockito.when(authenticationService.authenticate(ArgumentMatchers.any())).thenReturn(authenticationResponse);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("result.token").value("test-token"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.authenticated").value(true));
    }

    @Test
    void authenticate_invalidRequest_fail() throws Exception {
        // GIVEN
        Mockito.when(authenticationService.authenticate(ArgumentMatchers.any()))
                .thenThrow(new AppException(ErrorCode.WRONG_EMAIL_PASSWORD));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(MockMvcResultMatchers.status().isForbidden()) // WRONG_EMAIL_PASSWORD maps to 403 in ErrorCode
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1022))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Wrong email or password. Please try again."));
    }

    @Test
    void introspect_validToken_success() throws Exception {
        // GIVEN
        Mockito.when(authenticationService.introspect(ArgumentMatchers.any())).thenReturn(introspectResponse);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/introspect")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(introspectRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("result.valid").value(true));
    }

    @Test
    void logout_validToken_success() throws Exception {
        // GIVEN
        Mockito.doNothing().when(authenticationService).logout(ArgumentMatchers.any());

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000));
    }
}
