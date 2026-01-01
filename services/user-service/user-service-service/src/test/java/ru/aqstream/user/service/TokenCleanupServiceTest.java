package ru.aqstream.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenCleanupService")
class TokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Nested
    @DisplayName("cleanupTokens")
    class CleanupTokens {

        @Test
        @DisplayName("очищает истёкшие refresh токены")
        void cleanupTokens_HasExpiredRefresh_DeletesThem() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(10);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(5);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(0);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(refreshTokenRepository).deleteExpiredBefore(any());
            verify(refreshTokenRepository).deleteRevokedBefore(any());
        }

        @Test
        @DisplayName("очищает отозванные refresh токены старше 30 дней")
        void cleanupTokens_HasOldRevoked_DeletesThem() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(15);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(0);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(refreshTokenRepository).deleteRevokedBefore(any());
        }

        @Test
        @DisplayName("очищает истёкшие токены верификации")
        void cleanupTokens_HasExpiredVerification_DeletesThem() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(20);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(0);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(verificationTokenRepository).deleteExpiredBefore(any());
        }

        @Test
        @DisplayName("очищает использованные токены верификации старше 7 дней")
        void cleanupTokens_HasOldUsedVerification_DeletesThem() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(8);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(verificationTokenRepository).deleteUsedBefore(any());
        }

        @Test
        @DisplayName("успешно завершается если нет токенов для очистки")
        void cleanupTokens_NoTokensToClean_CompletesNormally() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(0);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(refreshTokenRepository).deleteExpiredBefore(any());
            verify(refreshTokenRepository).deleteRevokedBefore(any());
            verify(verificationTokenRepository).deleteExpiredBefore(any());
            verify(verificationTokenRepository).deleteUsedBefore(any());
        }

        @Test
        @DisplayName("очищает все типы токенов за один вызов")
        void cleanupTokens_AllTypes_CleansAllTypes() {
            // Given
            when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(5);
            when(refreshTokenRepository.deleteRevokedBefore(any())).thenReturn(3);
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(7);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(2);

            // When
            tokenCleanupService.cleanupTokens();

            // Then
            verify(refreshTokenRepository).deleteExpiredBefore(any());
            verify(refreshTokenRepository).deleteRevokedBefore(any());
            verify(verificationTokenRepository).deleteExpiredBefore(any());
            verify(verificationTokenRepository).deleteUsedBefore(any());
        }
    }
}
