package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.InviteMemberRequest;
import ru.aqstream.user.api.dto.OrganizationInviteDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.exception.AlreadyOrganizationMemberException;
import ru.aqstream.user.api.exception.OrganizationInviteAlreadyUsedException;
import ru.aqstream.user.api.exception.OrganizationInviteExpiredException;
import ru.aqstream.user.api.exception.OrganizationInviteNotFoundException;
import ru.aqstream.user.api.exception.OrganizationMemberNotFoundException;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationInvite;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.ORGANIZATIONS)
@DisplayName("OrganizationInviteService")
class OrganizationInviteServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private OrganizationInviteRepository inviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMemberMapper memberMapper;

    @Mock
    private OrganizationInviteMapper inviteMapper;

    @InjectMocks
    private OrganizationInviteService inviteService;

    @Captor
    private ArgumentCaptor<OrganizationInvite> inviteCaptor;

    @Captor
    private ArgumentCaptor<OrganizationMember> memberCaptor;

    private UUID organizationId;
    private UUID userId;
    private UUID inviteeUserId;
    private Organization organization;
    private User user;
    private User inviteeUser;
    private OrganizationMember ownerMember;
    private OrganizationMember moderatorMember;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        inviteeUserId = UUID.randomUUID();

        user = createTestUser();
        ReflectionTestUtils.setField(user, "id", userId);

        inviteeUser = createTestUser();
        ReflectionTestUtils.setField(inviteeUser, "id", inviteeUserId);

        organization = Organization.create(
            user,
            FAKER.company().name(),
            FAKER.internet().slug(),
            null
        );
        ReflectionTestUtils.setField(organization, "id", organizationId);

        ownerMember = OrganizationMember.createOwner(organization, user);
        moderatorMember = OrganizationMember.createModerator(organization, user, null);
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_MEMBERS)
    @DisplayName("createInvite")
    class CreateInvite {

        private InviteMemberRequest request;

        @BeforeEach
        void setUp() {
            request = new InviteMemberRequest("@telegram_user");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("создаёт приглашение при роли OWNER")
        void createInvite_OwnerRole_CreatesInvite() {
            // Given
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(ownerMember));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inviteMapper.toDto(any())).thenReturn(createInviteDto());

            // When
            OrganizationInviteDto result = inviteService.createInvite(organizationId, userId, request);

            // Then
            assertThat(result).isNotNull();
            verify(inviteRepository).save(inviteCaptor.capture());
            OrganizationInvite savedInvite = inviteCaptor.getValue();
            assertThat(savedInvite.getOrganization()).isEqualTo(organization);
            assertThat(savedInvite.getTelegramUsername()).isEqualTo("@telegram_user");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("создаёт приглашение при роли MODERATOR")
        void createInvite_ModeratorRole_CreatesInvite() {
            // Given
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(moderatorMember));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inviteMapper.toDto(any())).thenReturn(createInviteDto());

            // When
            OrganizationInviteDto result = inviteService.createInvite(organizationId, userId, request);

            // Then
            assertThat(result).isNotNull();
            verify(inviteRepository).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если не член организации")
        void createInvite_NotMember_ThrowsException() {
            // Given
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> inviteService.createInvite(organizationId, userId, request))
                .isInstanceOf(OrganizationMemberNotFoundException.class);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_MEMBERS)
    @DisplayName("getActiveInvites")
    class GetActiveInvites {

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает активные приглашения")
        void getActiveInvites_HasInvites_ReturnsList() {
            // Given
            OrganizationInvite invite = OrganizationInvite.create(organization, user, "@telegram");
            when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(ownerMember));
            when(inviteRepository.findActiveByOrganizationId(any(), any()))
                .thenReturn(List.of(invite));
            when(inviteMapper.toDto(invite)).thenReturn(createInviteDto());

            // When
            List<OrganizationInviteDto> result = inviteService.getActiveInvites(organizationId, userId);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_MEMBERS)
    @DisplayName("acceptInvite")
    class AcceptInvite {

        private OrganizationInvite validInvite;

        @BeforeEach
        void setUp() {
            validInvite = OrganizationInvite.create(organization, user, "@telegram");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("принимает приглашение успешно")
        void acceptInvite_ValidInvite_JoinsOrganization() {
            // Given
            when(inviteRepository.findByInviteCode(validInvite.getInviteCode()))
                .thenReturn(Optional.of(validInvite));
            when(userRepository.findById(inviteeUserId)).thenReturn(Optional.of(inviteeUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, inviteeUserId))
                .thenReturn(false);
            when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberMapper.toDto(any())).thenReturn(createMemberDto());

            // When
            OrganizationMemberDto result = inviteService.acceptInvite(inviteeUserId, validInvite.getInviteCode());

            // Then
            assertThat(result).isNotNull();
            verify(inviteRepository).save(validInvite);
            assertThat(validInvite.isUsed()).isTrue();
            verify(memberRepository).save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getUser()).isEqualTo(inviteeUser);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если приглашение не найдено")
        void acceptInvite_NotFound_ThrowsException() {
            // Given
            when(inviteRepository.findByInviteCode("invalid-code")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> inviteService.acceptInvite(inviteeUserId, "invalid-code"))
                .isInstanceOf(OrganizationInviteNotFoundException.class);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если приглашение истекло")
        void acceptInvite_Expired_ThrowsException() {
            // Given
            ReflectionTestUtils.setField(validInvite, "expiresAt", Instant.now().minusSeconds(3600));
            when(inviteRepository.findByInviteCode(validInvite.getInviteCode()))
                .thenReturn(Optional.of(validInvite));

            // When/Then
            assertThatThrownBy(() -> inviteService.acceptInvite(inviteeUserId, validInvite.getInviteCode()))
                .isInstanceOf(OrganizationInviteExpiredException.class);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если приглашение использовано")
        void acceptInvite_AlreadyUsed_ThrowsException() {
            // Given
            validInvite.markAsUsed(inviteeUser);
            when(inviteRepository.findByInviteCode(validInvite.getInviteCode()))
                .thenReturn(Optional.of(validInvite));

            // When/Then
            assertThatThrownBy(() -> inviteService.acceptInvite(inviteeUserId, validInvite.getInviteCode()))
                .isInstanceOf(OrganizationInviteAlreadyUsedException.class);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если уже член организации")
        void acceptInvite_AlreadyMember_ThrowsException() {
            // Given
            when(inviteRepository.findByInviteCode(validInvite.getInviteCode()))
                .thenReturn(Optional.of(validInvite));
            when(userRepository.findById(inviteeUserId)).thenReturn(Optional.of(inviteeUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, inviteeUserId))
                .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> inviteService.acceptInvite(inviteeUserId, validInvite.getInviteCode()))
                .isInstanceOf(AlreadyOrganizationMemberException.class);
        }
    }

    /**
     * Создаёт тестового пользователя.
     */
    private User createTestUser() {
        return User.createWithEmail(
            FAKER.internet().emailAddress(),
            "$2a$12$hashedpassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
    }

    private OrganizationInviteDto createInviteDto() {
        return OrganizationInviteDto.builder()
            .id(UUID.randomUUID())
            .organizationId(organizationId)
            .organizationName("Test Org")
            .inviteCode("abc123")
            .invitedById(userId)
            .invitedByName("Test User")
            .telegramUsername("@telegram")
            .role(OrganizationRole.MODERATOR)
            .expiresAt(Instant.now().plusSeconds(86400))
            .createdAt(Instant.now())
            .build();
    }

    private OrganizationMemberDto createMemberDto() {
        return OrganizationMemberDto.builder()
            .id(UUID.randomUUID())
            .userId(inviteeUserId)
            .userName("Test User")
            .role(OrganizationRole.MODERATOR)
            .invitedById(userId)
            .joinedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }
}
