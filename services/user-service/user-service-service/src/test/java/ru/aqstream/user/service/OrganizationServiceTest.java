package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.user.api.dto.CreateOrganizationRequest;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UpdateOrganizationRequest;
import ru.aqstream.user.api.exception.InsufficientOrganizationPermissionsException;
import ru.aqstream.user.api.exception.NoApprovedRequestException;
import ru.aqstream.user.api.exception.OrganizationMemberNotFoundException;
import ru.aqstream.user.api.exception.OrganizationNotFoundException;
import ru.aqstream.user.api.exception.OrganizationSlugAlreadyExistsException;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService")
class OrganizationServiceTest {

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
    @DisplayName("Create")
    class Create {

        @Test
        @DisplayName("Создаёт организацию при наличии одобренного запроса")
        void create_ValidRequest_CreatesOrganization() {
            // Given
            CreateOrganizationRequest request = new CreateOrganizationRequest(testName, testDescription);

            OrganizationRequest approvedRequest = OrganizationRequest.create(
                testUser, testName, testSlug, testDescription
            );
            ReflectionTestUtils.setField(approvedRequest, "id", UUID.randomUUID());

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(requestRepository.findApprovedByUserId(testUserId)).thenReturn(Optional.of(approvedRequest));
            when(organizationRepository.existsBySlug(testSlug)).thenReturn(false);
            when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
                Organization saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", testOrgId);
                return saved;
            });
            when(memberRepository.save(any(OrganizationMember.class))).thenAnswer(i -> i.getArgument(0));

            OrganizationDto expectedDto = OrganizationDto.builder()
                .id(testOrgId)
                .name(testName)
                .slug(testSlug)
                .build();
            when(organizationMapper.toDto(any(Organization.class))).thenReturn(expectedDto);

            // When
            OrganizationDto result = service.create(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testOrgId);

            ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(orgCaptor.capture());
            assertThat(orgCaptor.getValue().getName()).isEqualTo(testName);

            ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
            verify(memberRepository).save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getRole()).isEqualTo(OrganizationRole.OWNER);
        }

        @Test
        @DisplayName("Выбрасывает исключение без одобренного запроса")
        void create_NoApprovedRequest_ThrowsException() {
            // Given
            CreateOrganizationRequest request = new CreateOrganizationRequest(testName, testDescription);

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(requestRepository.findApprovedByUserId(testUserId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.create(testUserId, request))
                .isInstanceOf(NoApprovedRequestException.class);

            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Выбрасывает исключение при занятом slug")
        void create_SlugTaken_ThrowsException() {
            // Given
            CreateOrganizationRequest request = new CreateOrganizationRequest(testName, testDescription);

            OrganizationRequest approvedRequest = OrganizationRequest.create(
                testUser, testName, testSlug, testDescription
            );

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(requestRepository.findApprovedByUserId(testUserId)).thenReturn(Optional.of(approvedRequest));
            when(organizationRepository.existsBySlug(testSlug)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(testUserId, request))
                .isInstanceOf(OrganizationSlugAlreadyExistsException.class);

            verify(organizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("GetById")
    class GetById {

        @Test
        @DisplayName("Возвращает организацию члену")
        void getById_Member_ReturnsOrganization() {
            // Given
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(member));
            when(organizationMapper.toDto(testOrg)).thenReturn(
                OrganizationDto.builder().id(testOrgId).name(testName).build()
            );

            // When
            OrganizationDto result = service.getById(testOrgId, testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testOrgId);
        }

        @Test
        @DisplayName("Выбрасывает исключение если организация не найдена")
        void getById_NotFound_ThrowsException() {
            // Given
            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(testOrgId, testUserId))
                .isInstanceOf(OrganizationNotFoundException.class);
        }

        @Test
        @DisplayName("Выбрасывает исключение если пользователь не член")
        void getById_NotMember_ThrowsException() {
            // Given
            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(testOrgId, testUserId))
                .isInstanceOf(OrganizationMemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Test
        @DisplayName("OWNER может обновить организацию")
        void update_Owner_UpdatesOrganization() {
            // Given
            UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "New Name", "New Description", null
            );
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(member));
            when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));
            when(organizationMapper.toDto(any(Organization.class))).thenReturn(
                OrganizationDto.builder().id(testOrgId).name("New Name").build()
            );

            // When
            OrganizationDto result = service.update(testOrgId, testUserId, request);

            // Then
            assertThat(result).isNotNull();
            verify(organizationRepository).save(any(Organization.class));
        }

        @Test
        @DisplayName("MODERATOR может обновить организацию")
        void update_Moderator_UpdatesOrganization() {
            // Given
            UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "New Name", "New Description", null
            );
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.MODERATOR);

            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(member));
            when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));
            when(organizationMapper.toDto(any(Organization.class))).thenReturn(
                OrganizationDto.builder().id(testOrgId).build()
            );

            // When
            OrganizationDto result = service.update(testOrgId, testUserId, request);

            // Then
            assertThat(result).isNotNull();
            verify(organizationRepository).save(any(Organization.class));
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("OWNER может удалить организацию")
        void delete_Owner_DeletesOrganization() {
            // Given
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.OWNER);

            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(member));
            when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

            // When
            service.delete(testOrgId, testUserId);

            // Then
            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
        }

        @Test
        @DisplayName("MODERATOR не может удалить организацию")
        void delete_Moderator_ThrowsException() {
            // Given
            OrganizationMember member = createTestMember(testOrg, testUser, OrganizationRole.MODERATOR);

            when(organizationRepository.findByIdWithOwner(testOrgId)).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(member));

            // When/Then
            assertThatThrownBy(() -> service.delete(testOrgId, testUserId))
                .isInstanceOf(InsufficientOrganizationPermissionsException.class);

            verify(organizationRepository, never()).save(any());
        }
    }

}
