package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.event.api.dto.DashboardStatsDto;
import ru.aqstream.event.service.DashboardService;

/**
 * Контроллер статистики для dashboard организатора.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Статистика для dashboard организатора")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
        summary = "Получить статистику dashboard",
        description = "Возвращает агрегированную статистику для dashboard организатора: "
            + "количество активных событий, регистрации за 30 дней, процент посещаемости, "
            + "список ближайших событий."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Статистика получена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        DashboardStatsDto stats = dashboardService.getStats();
        return ResponseEntity.ok(stats);
    }
}
