package ru.aqstream.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationInviteCleanupService")
class OrganizationInviteCleanupServiceTest {

    @Mock
    private OrganizationInviteRepository inviteRepository;

    @InjectMocks
    private OrganizationInviteCleanupService cleanupService;

    @Test
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
