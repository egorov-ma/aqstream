package ru.aqstream.media.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.media.api.dto.MediaPurpose;
import ru.aqstream.media.api.dto.UploadResponse;
import ru.aqstream.media.service.MediaService;

/**
 * Контроллер для работы с медиа-файлами.
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Загрузка и управление медиа-файлами")
public class MediaController {

    private final MediaService mediaService;

    @Operation(
        summary = "Загрузить файл",
        description = "Загружает файл в хранилище. Максимальный размер: 10MB. "
            + "Для аватаров и обложек допустимы: JPEG, PNG, WebP, GIF."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Файл загружен"),
        @ApiResponse(responseCode = "400", description = "Невалидный файл"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "413", description = "Файл слишком большой")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "purpose", defaultValue = "GENERAL") MediaPurpose purpose,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        UploadResponse response = mediaService.upload(file, purpose, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Загрузить аватар",
        description = "Загружает аватар пользователя. Допустимы: JPEG, PNG, WebP, GIF. Макс. 10MB."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Аватар загружен"),
        @ApiResponse(responseCode = "400", description = "Невалидный файл"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadAvatar(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        UploadResponse response = mediaService.upload(file, MediaPurpose.USER_AVATAR, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Удалить файл",
        description = "Удаляет файл. Доступно только владельцу файла."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Файл удалён"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет доступа к файлу"),
        @ApiResponse(responseCode = "404", description = "Файл не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        mediaService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException();
        }
    }

    /**
     * Исключение: не авторизован.
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException() {
            super("Требуется аутентификация");
        }
    }
}
