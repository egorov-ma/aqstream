package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import ru.aqstream.common.api.exception.ForbiddenException;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.OrganizationMembershipDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.client.UserClient;

@UnitTest
@Feature(AllureFeatures.Features.EVENT_MANAGEMENT)
@DisplayName("EventPermissionService")
class EventPermissionServiceTest {

    @Mock
    private UserClient userClient;

    private EventPermissionService service;

    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        service = new EventPermissionService(userClient);
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_PERMISSIONS)
    @DisplayName("validateCreatePermission()")
    class ValidateCreatePermission {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Админ может создавать события для любой организации")
        void validateCreatePermission_Admin_AllowsAnyOrganization() {
            // when/then: не выбрасывает исключение
            assertDoesNotThrow(() ->
                service.validateCreatePermission(userId, organizationId, true)
            );

            // Не вызывает userClient для админов
            verifyNoInteractions(userClient);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("OWNER может создавать события")
        void validateCreatePermission_Owner_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.OWNER));

            // when/then
            assertDoesNotThrow(() ->
                service.validateCreatePermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("MODERATOR может создавать события")
        void validateCreatePermission_Moderator_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.MODERATOR));

            // when/then
            assertDoesNotThrow(() ->
                service.validateCreatePermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Не-член организации получает 403")
        void validateCreatePermission_NotMember_ThrowsForbidden() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.notMember(organizationId, userId));

            // when/then
            assertThatThrownBy(() ->
                service.validateCreatePermission(userId, organizationId, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("не является членом");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Ошибка при вызове userClient выбрасывает ForbiddenException")
        void validateCreatePermission_UserClientError_ThrowsForbidden() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenThrow(new RuntimeException("Connection error"));

            // when/then
            assertThatThrownBy(() ->
                service.validateCreatePermission(userId, organizationId, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Не удалось проверить права");
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_PERMISSIONS)
    @DisplayName("validateManagePermission()")
    class ValidateManagePermission {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Админ может управлять любыми событиями")
        void validateManagePermission_Admin_AllowsAnyOrganization() {
            // when/then: не выбрасывает исключение
            assertDoesNotThrow(() ->
                service.validateManagePermission(userId, organizationId, true)
            );

            // Не вызывает userClient для админов
            verifyNoInteractions(userClient);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("OWNER может управлять событиями")
        void validateManagePermission_Owner_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.OWNER));

            // when/then
            assertDoesNotThrow(() ->
                service.validateManagePermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("MODERATOR может управлять событиями")
        void validateManagePermission_Moderator_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.MODERATOR));

            // when/then
            assertDoesNotThrow(() ->
                service.validateManagePermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Обычный пользователь не может управлять событиями")
        void validateManagePermission_RegularUser_ThrowsForbidden() {
            // given: пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, null));

            // when/then
            assertThatThrownBy(() ->
                service.validateManagePermission(userId, organizationId, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Недостаточно прав");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Не-член организации получает 403")
        void validateManagePermission_NotMember_ThrowsForbidden() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.notMember(organizationId, userId));

            // when/then
            assertThatThrownBy(() ->
                service.validateManagePermission(userId, organizationId, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("не является членом");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Null organizationId выбрасывает ForbiddenException")
        void validateManagePermission_NullOrganization_ThrowsForbidden() {
            // when/then
            assertThatThrownBy(() ->
                service.validateManagePermission(userId, null, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Выберите организацию");
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_PERMISSIONS)
    @DisplayName("validateViewPermission()")
    class ValidateViewPermission {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Админ может просматривать любые события")
        void validateViewPermission_Admin_AllowsAnyOrganization() {
            // when/then: не выбрасывает исключение
            assertDoesNotThrow(() ->
                service.validateViewPermission(userId, organizationId, true)
            );

            // Не вызывает userClient для админов
            verifyNoInteractions(userClient);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("OWNER может просматривать события")
        void validateViewPermission_Owner_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.OWNER));

            // when/then
            assertDoesNotThrow(() ->
                service.validateViewPermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("MODERATOR может просматривать события")
        void validateViewPermission_Moderator_Allows() {
            // given
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, OrganizationRole.MODERATOR));

            // when/then
            assertDoesNotThrow(() ->
                service.validateViewPermission(userId, organizationId, false)
            );
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Обычный пользователь не может просматривать события в dashboard")
        void validateViewPermission_RegularUser_ThrowsForbidden() {
            // given: пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(organizationId, userId))
                .thenReturn(OrganizationMembershipDto.member(organizationId, userId, null));

            // when/then
            assertThatThrownBy(() ->
                service.validateViewPermission(userId, organizationId, false)
            )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Недостаточно прав");
        }
    }
}
