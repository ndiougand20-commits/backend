package com.rezo.backend.controller;

import com.rezo.backend.service.UserMediaStorageService;
import com.rezo.entities.User;
import com.rezo.entities.UserMediaFile;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.UserMediaFileRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserMediaControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMediaFileRepository userMediaFileRepository;
    @Mock private UserMediaStorageService userMediaStorageService;

    private MockMvc mockMvc;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID mediaId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        UserMediaController controller = new UserMediaController(userRepository, userMediaFileRepository, userMediaStorageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadPhotoShouldReturn201() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(userMediaStorageService.storePhoto(any(UUID.class), any())).thenReturn(
                new UserMediaStorageService.StoredMedia("PHOTO", "photo.png", "image/png", "/uploads/users/" + userId + "/photos/file.png")
        );
        when(userMediaFileRepository.save(any(UserMediaFile.class))).thenAnswer(invocation -> {
            UserMediaFile file = invocation.getArgument(0);
            setField(file, "id", mediaId);
            setField(file, "createdAt", LocalDateTime.of(2026, 4, 13, 10, 0));
            return file;
        });

        MockMultipartFile multipartFile = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/users/me/media/photos")
                        .file(multipartFile)
                        .principal(principal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(mediaId.toString()))
                .andExpect(jsonPath("$.category").value("PHOTO"))
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void uploadPdfShouldReturn400WhenInvalid() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(userMediaStorageService.storeJustificatifPdf(any(UUID.class), any()))
                .thenThrow(new UserMediaStorageService.MediaValidationException("Seuls les fichiers PDF sont autorises pour les justificatifs"));

        MockMultipartFile multipartFile = new MockMultipartFile("file", "doc.txt", "text/plain", "bad".getBytes());

        mockMvc.perform(multipart("/api/users/me/media/justificatifs")
                        .file(multipartFile)
                        .principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Seuls les fichiers PDF sont autorises pour les justificatifs"));
    }

    @Test
    void listMediaShouldFilterByCategory() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));

        UserMediaFile media = new UserMediaFile();
        setField(media, "id", mediaId);
        media.setUserId(userId);
        media.setCategory("JUSTIFICATIF_PDF");
        media.setOriginalFileName("attestation.pdf");
        media.setContentType("application/pdf");
        media.setFileUrl("/uploads/users/" + userId + "/justificatifs/file.pdf");
        setField(media, "createdAt", LocalDateTime.of(2026, 4, 13, 10, 10));

        when(userMediaFileRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, "JUSTIFICATIF_PDF"))
                .thenReturn(List.of(media));

        mockMvc.perform(get("/api/users/me/media")
                        .param("category", "justificatif_pdf")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("JUSTIFICATIF_PDF"))
                .andExpect(jsonPath("$[0].originalFileName").value("attestation.pdf"));
    }

    @Test
    void deleteMediaShouldReturn200ForOwner() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(userMediaFileRepository.deleteByIdAndUserId(mediaId, userId)).thenReturn(1);

        mockMvc.perform(delete("/api/users/me/media/{id}", mediaId)
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media supprime avec succes"));

        verify(userMediaFileRepository).deleteByIdAndUserId(mediaId, userId);
    }

    private User buildUser() {
        User user = new User();
        setField(user, "id", userId);
        user.setEmail("media@rezo.com");
        user.setPrenom("Media");
        user.setNom("User");
        user.setRole(UserRole.ETUDIANT);
        return user;
    }

    private Principal principal() {
        return userId::toString;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
