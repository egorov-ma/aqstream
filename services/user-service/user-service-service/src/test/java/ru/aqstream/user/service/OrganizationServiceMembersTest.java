package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.exception.CannotRemoveOwnerException;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Тесты для управления членами OrganizationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService Members")
class OrganizationServiceMembersTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private OrganizationRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OrganizationMapper organizationMapper;

    @Mock
    private OrganizationMemberMapper memberMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ru.aqstream.common.messaging.EventPublisher eventPublisher;

    private OrganizationService service;

    private static final Faker FAKER = new Faker();

    private UUID testUserId;
    private UUID testOrgId;
    private String testName;
    private String testSlug;
    private String testDescription;
    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        service = new OrganizationService(
            organizationRepository,
            memberRepository,
            requestRepository,
            userRepository,
            refreshTokenRepository,
            organizationMapper,
            memberMapper,
            jwtTokenProvider,
            userMapper,
            eventPublisher
        );

        ReflectionTestUtils.setField(service, "accessTokenExpiration", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", Duration.ofDays(7));

        testUserId = UUID.randomUUID();
        testOrgId = UUID.randomUUID();
        testName = FAKER.company().name();
        testSlug = FAKER.internet().slug().toLowerCase().replace("_", "-");
        testDescription = FAKER.company().catchPhrase();

        testUser = createTestUser(testUserId);
        testOrg = createTestOrganization(testOrgId, testUser);
    }

    private User createTestUser(UUID userId) {
        User user = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Organization createTestOrganization(UUID orgId, User owner) {
        Organization org = Organization.create(owner, testName, testSlug, testDescription);
        ReflectionTestUtils.setField(org, "id", orgId);
        return org;
    }

    private OrganizationMember createTestMember(Organization org, User user, OrganizationRole role) {
        OrganizationMember member;
        if (role == OrganizationRole.OWNER) {
            member = OrganizationMember.createOwner(org, user);
        } else {
            member = OrganizationMember.createModerator(org, user, null);
        }
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
        return member;
    }

    @Nested
    @DisplayName("RemoveMember")
    class RemoveMember {

        @Test
        @DisplayName("OWNER может удалить MODERATOR")
        void removeMember_OwnerRemovesModerator_Success() {
            UUID moderatorId = UUID.randomUUID();
            User moderatorUser = createTestUser(moderatorId);

            OrganizationMember ownerMember = createTestMember(testOrg, testUser, OrganizationRole.OWNER);
            OrganizationMember moderatorMember = createTestMember(testOrg, moderatorUser, OrganizationRole.MODERATOR);

            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(ownerMember));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, moderatorId))
                .thenReturn(Optional.of(moderatorMember));

            service.removeMember(testOrgId, testUserId, moderatorId);

            verify(memberRepository).delete(moderatorMember);
        }

        @Test
        @DisplayName("Нельзя удалить OWNER")
        void removeMember_CannotRemoveOwner_ThrowsException() {
            UUID anotherUserId = UUID.randomUUID();
            User anotherUser = createTestUser(anotherUserId);

            OrganizationMember moderatorMember = createTestMember(testOrg, anotherUser, OrganizationRole.MODERATOR);
            OrganizationMember ownerMember = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, anotherUserId))
                .thenReturn(Optional.of(moderatorMember));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(ownerMember));

            assertThatThrownBy(() -> service.removeMember(testOrgId, anotherUserId, testUserId))
                .isInstanceOf(CannotRemoveOwnerException.class);

            verify(memberRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("GetMembers")
    class GetMembers {

        @Test
        @DisplayName("Возвращает список членов организации")
        void getMembers_ValidRequest_ReturnsMembers() {
            OrganizationMember ownerMember = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(ownerMember));
            when(memberRepository.findByOrganizationId(testOrgId))
                .thenReturn(List.of(ownerMember));
            when(memberMapper.toDto(any(OrganizationMember.class))).thenReturn(
                OrganizationMemberDto.builder()
                    .userId(testUserId)
                    .role(OrganizationRole.OWNER)
                    .build()
            );

            List<OrganizationMemberDto> result = service.getMembers(testOrgId, testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).role()).isEqualTo(OrganizationRole.OWNER);
        }
    }

    @Nested
    @DisplayName("GetMyOrganizations")
    class GetMyOrganizations {

        @Test
        @DisplayName("Возвращает список организаций пользователя")
        void getMyOrganizations_ReturnsUserOrganizations() {
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(memberRepository.findByUserId(testUserId)).thenReturn(List.of(member));
            when(organizationMapper.toDto(any(Organization.class))).thenReturn(
                OrganizationDto.builder().id(testOrgId).name(testName).build()
            );

            List<OrganizationDto> result = service.getMyOrganizations(testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(testOrgId);
        }

        @Test
        @DisplayName("Возвращает пустой список если нет организаций")
        void getMyOrganizations_NoOrganizations_ReturnsEmptyList() {
            when(memberRepository.findByUserId(testUserId)).thenReturn(List.of());

            List<OrganizationDto> result = service.getMyOrganizations(testUserId);

            assertThat(result).isEmpty();
        }
    }
}
