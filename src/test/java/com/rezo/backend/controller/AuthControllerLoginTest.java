package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.auth.LoginRequest;
import com.rezo.backend.service.JwtService;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PackRepository packRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private Environment environment;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /** Un User valide avec un hash BCrypt du mot de passe "secret". */
    private User buildUser(String email, String rawPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Pack pack = new Pack();
        pack.setNom("FREE");

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setRole(UserRole.ETUDIANT);
        user.setPack(pack);
        return user;
    }

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                userRepository,
                packRepository,
                profileRepository,
                companyRepository,
                schoolRepository,
                jwtService,
                environment
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnTokenOnSuccessfulLogin() throws Exception {
        User user = buildUser("alice@rezo.com", "secret");
        when(userRepository.findByEmail("alice@rezo.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake.jwt.token");

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@rezo.com");
        request.setPassword("secret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"));
    }

    @Test
    void shouldReturn401WhenPasswordIsWrong() throws Exception {
        User user = buildUser("bob@rezo.com", "correct");
        when(userRepository.findByEmail("bob@rezo.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("bob@rezo.com");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void shouldReturn401WhenEmailNotFound() throws Exception {
        when(userRepository.findByEmail("unknown@rezo.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@rezo.com");
        request.setPassword("anypassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void shouldReturn400WhenEmailIsMissing() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("secret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email et mot de passe obligatoires"));
    }

    @Test
    void shouldReturn400WhenPasswordIsMissing() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@rezo.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email et mot de passe obligatoires"));
    }
}
