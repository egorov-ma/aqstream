package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.event.api.dto.CheckInInfoDto;
import ru.aqstream.event.api.dto.CheckInResultDto;
import ru.aqstream.event.service.CheckInService;

/**
 * Контроллер check-in участников.
 *
 * <p>Публичный API для проверки регистрации по QR-коду и выполнения check-in.
 * Доступен без авторизации для упрощения процесса на входе.</p>
 *
 * <p>Безопасность обеспечивается секретностью confirmation code в QR-коде.</p>
 */
@RestController
@RequestMapping("/api/v1/public/check-in")
@RequiredArgsConstructor
@Tag(name = "Check-In", description = "Регистрация участников на входе")
public class CheckInController {

    private final CheckInService checkInService;

    @Operation(
        summary = "Получить информацию о регистрации",
        description = "Возвращает информацию о регистрации по confirmation code. "
            + "Используется для отображения данных участника перед подтверждением check-in."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Информация о регистрации"),
        @ApiResponse(responseCode = "404", description = "Регистрация не найдена")
    })
    @GetMapping("/{confirmationCode}")
    public ResponseEntity<CheckInInfoDto> getCheckInInfo(
        @Parameter(description = "Код подтверждения из QR-кода")
        @PathVariable String confirmationCode
    ) {
        CheckInInfoDto info = checkInService.getCheckInInfo(confirmationCode);
        return ResponseEntity.ok(info);
    }

    @Operation(
        summary = "Выполнить check-in",
        description = "Регистрирует участника на входе. "
            + "Защита от повторного check-in — возвращается ошибка если участник уже прошёл check-in."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check-in выполнен успешно"),
        @ApiResponse(responseCode = "404", description = "Регистрация не найдена"),
        @ApiResponse(responseCode = "409", description = "Участник уже прошёл check-in или check-in невозможен")
    })
    @PostMapping("/{confirmationCode}")
    public ResponseEntity<CheckInResultDto> checkIn(
        @Parameter(description = "Код подтверждения из QR-кода")
        @PathVariable String confirmationCode
    ) {
        CheckInResultDto result = checkInService.checkIn(confirmationCode);
        return ResponseEntity.ok(result);
    }
}
