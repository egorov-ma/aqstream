package ru.aqstream.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;

@UnitTest
@Feature(AllureFeatures.Features.ORGANIZATIONS)
@Story(AllureFeatures.Stories.ORGANIZATION_MEMBERS)
@DisplayName("OrganizationInviteCleanupService")
class OrganizationInviteCleanupServiceTest {

    @Mock
    private OrganizationInviteRepository inviteRepository;

    @InjectMocks
    private OrganizationInviteCleanupService cleanupService;

    @Test
    @Severity(NORMAL)
    @DisplayName("удаляет истёкшие приглашения")
    void cleanupExpiredInvites_HasExpired_DeletesThem() {
        // Given
        when(inviteRepository.deleteExpiredBefore(any())).thenReturn(5);

        // When
        cleanupService.cleanupExpiredInvites();

        // Then
        verify(inviteRepository).deleteExpiredBefore(any());
    }

    @Test
    @Severity(NORMAL)
    @DisplayName("не выбрасывает исключение если нет приглашений для удаления")
    void cleanupExpiredInvites_NoExpired_CompletesNormally() {
        // Given
        when(inviteRepository.deleteExpiredBefore(any())).thenReturn(0);

        // When
        cleanupService.cleanupExpiredInvites();

        // Then
        verify(inviteRepository).deleteExpiredBefore(any());
    }
}
