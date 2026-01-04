package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.CreateOrganizationRequestRequest;
import ru.aqstream.user.api.dto.OrganizationRequestDto;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;
import ru.aqstream.user.api.dto.RejectOrganizationRequestRequest;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.api.exception.OrganizationRequestAlreadyReviewedException;
import ru.aqstream.user.api.exception.OrganizationRequestNotFoundException;
import ru.aqstream.user.api.exception.OrganizationSlugAlreadyExistsException;
import ru.aqstream.user.api.exception.PendingRequestAlreadyExistsException;
import ru.aqstream.user.api.exception.SlugAlreadyExistsException;
import ru.aqstream.user.api.event.OrganizationRequestApprovedEvent;
import ru.aqstream.user.api.event.OrganizationRequestCreatedEvent;
import ru.aqstream.user.api.event.OrganizationRequestRejectedEvent;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.ORGANIZATIONS)
@DisplayName("OrganizationRequestService")
class OrganizationRequestServiceTest {

    @Mock
    private OrganizationRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRequestMapper requestMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationService organizationService;

    private OrganizationRequestService service;

    private static final Faker FAKER = new Faker();

    private UUID testUserId;
    private UUID testAdminId;
    private UUID testRequestId;
    private String testName;
    private String testSlug;
    private String testDescription;
    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        service = new OrganizationRequestService(
            requestRepository, userRepository, requestMapper, eventPublisher, organizationRepository, organizationService
        );

        // Генерируем свежие тестовые данные для каждого теста
        testUserId = UUID.randomUUID();
        testAdminId = UUID.randomUUID();
        testRequestId = UUID.randomUUID();
        testName = FAKER.company().name();
        testSlug = FAKER.internet().slug().toLowerCase().replace("_", "-");
        testDescription = FAKER.company().catchPhrase();

        testUser = createTestUser(testUserId);
        testAdmin = createTestUser(testAdminId);
    }

    private User createTestUser(UUID userId) {
        User user = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        // Устанавливаем ID через reflection
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private OrganizationRequest createTestRequest(User user, OrganizationRequestStatus status) {
        OrganizationRequest request = OrganizationRequest.create(user, testName, testSlug, testDescription);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "id", testRequestId);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "status", status);
        return request;
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
    @DisplayName("Create")
    class Create {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Создаёт запрос при валидных данных")
        void create_ValidRequest_CreatesRequest() {
            // Given
            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                testName, testSlug, testDescription
            );

            when(requestRepository.existsPendingByUserId(testUserId)).thenReturn(false);
            when(requestRepository.existsPendingBySlug(testSlug.toLowerCase())).thenReturn(false);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(requestRepository.save(any(OrganizationRequest.class))).thenAnswer(invocation -> {
                OrganizationRequest saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", testRequestId);
                return saved;
            });

            OrganizationRequestDto expectedDto = OrganizationRequestDto.builder()
                .id(testRequestId)
                .userId(testUserId)
                .name(testName)
                .slug(testSlug.toLowerCase())
                .status(OrganizationRequestStatus.PENDING)
                .build();
            when(requestMapper.toDto(any(OrganizationRequest.class))).thenReturn(expectedDto);

            // When
            OrganizationRequestDto result = service.create(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testRequestId);
            assertThat(result.status()).isEqualTo(OrganizationRequestStatus.PENDING);

            ArgumentCaptor<OrganizationRequest> captor = ArgumentCaptor.forClass(OrganizationRequest.class);
            verify(requestRepository).save(captor.capture());
            OrganizationRequest savedRequest = captor.getValue();
            assertThat(savedRequest.getName()).isEqualTo(testName);
            assertThat(savedRequest.getSlug()).isEqualTo(testSlug.toLowerCase());

            // Проверяем содержимое опубликованного события
            ArgumentCaptor<OrganizationRequestCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrganizationRequestCreatedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            OrganizationRequestCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getRequestId()).isEqualTo(testRequestId);
            assertThat(event.getUserId()).isEqualTo(testUserId);
            assertThat(event.getOrganizationName()).isEqualTo(testName);
            assertThat(event.getSlug()).isEqualTo(testSlug.toLowerCase());
            assertThat(event.getEventType()).isEqualTo("organization.request.created");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение при наличии активного запроса")
        void create_PendingRequestExists_ThrowsException() {
            // Given
            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                testName, testSlug, testDescription
            );

            when(requestRepository.existsPendingByUserId(testUserId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(testUserId, request))
                .isInstanceOf(PendingRequestAlreadyExistsException.class);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение при занятом slug")
        void create_SlugTaken_ThrowsException() {
            // Given
            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                testName, testSlug, testDescription
            );

            when(requestRepository.existsPendingByUserId(testUserId)).thenReturn(false);
            when(requestRepository.existsPendingBySlug(testSlug.toLowerCase())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(testUserId, request))
                .isInstanceOf(SlugAlreadyExistsException.class);

            verify(requestRepository, never()).save(any());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
    @DisplayName("GetById")
    class GetById {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Возвращает запрос владельцу")
        void getById_Owner_ReturnsRequest() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));
            when(requestMapper.toDto(request)).thenReturn(
                OrganizationRequestDto.builder().id(testRequestId).build()
            );

            // When
            OrganizationRequestDto result = service.getById(testRequestId, testUserId, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testRequestId);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Возвращает любой запрос админу")
        void getById_Admin_ReturnsAnyRequest() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));
            when(requestMapper.toDto(request)).thenReturn(
                OrganizationRequestDto.builder().id(testRequestId).build()
            );

            // When
            OrganizationRequestDto result = service.getById(testRequestId, testAdminId, true);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение при доступе к чужому запросу")
        void getById_OtherUser_ThrowsAccessDenied() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));

            UUID otherUserId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> service.getById(testRequestId, otherUserId, false))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение если запрос не найден")
        void getById_NotFound_ThrowsException() {
            // Given
            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(testRequestId, testUserId, false))
                .isInstanceOf(OrganizationRequestNotFoundException.class);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
    @DisplayName("Approve")
    class Approve {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Одобряет pending запрос и автоматически создаёт организацию")
        void approve_PendingRequest_ApprovesAndCreatesOrganization() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(testAdminId)).thenReturn(Optional.of(testAdmin));
            when(requestRepository.save(any(OrganizationRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(requestMapper.toDto(any(OrganizationRequest.class))).thenReturn(
                OrganizationRequestDto.builder()
                    .id(testRequestId)
                    .status(OrganizationRequestStatus.APPROVED)
                    .build()
            );

            // Мокируем создание организации
            Organization mockOrg = Organization.create(testUser, testName, testSlug, testDescription);
            when(organizationService.createFromApprovedRequest(any(OrganizationRequest.class)))
                .thenReturn(mockOrg);

            // When
            OrganizationRequestDto result = service.approve(testRequestId, testAdminId);

            // Then
            assertThat(result.status()).isEqualTo(OrganizationRequestStatus.APPROVED);
            verify(requestRepository).save(any(OrganizationRequest.class));

            // Проверяем, что организация создана автоматически
            ArgumentCaptor<OrganizationRequest> orgCaptor = ArgumentCaptor.forClass(OrganizationRequest.class);
            verify(organizationService).createFromApprovedRequest(orgCaptor.capture());
            OrganizationRequest capturedRequest = orgCaptor.getValue();
            assertThat(capturedRequest.getId()).isEqualTo(testRequestId);

            // Проверяем событие одобрения
            ArgumentCaptor<OrganizationRequestApprovedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrganizationRequestApprovedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            OrganizationRequestApprovedEvent event = eventCaptor.getValue();
            assertThat(event.getRequestId()).isEqualTo(testRequestId);
            assertThat(event.getUserId()).isEqualTo(testUserId);
            assertThat(event.getOrganizationName()).isEqualTo(testName);
            assertThat(event.getSlug()).isEqualTo(testSlug.toLowerCase());
            assertThat(event.getApprovedById()).isEqualTo(testAdminId);
            assertThat(event.getEventType()).isEqualTo("organization.request.approved");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение при одобрении уже рассмотренного запроса")
        void approve_AlreadyReviewed_ThrowsException() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.APPROVED);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> service.approve(testRequestId, testAdminId))
                .isInstanceOf(OrganizationRequestAlreadyReviewedException.class);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Одобряет запрос, но не откатывает если slug уже занят")
        void approve_SlugAlreadyExists_ApprovesButLogsWarning() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(testAdminId)).thenReturn(Optional.of(testAdmin));
            when(requestRepository.save(any(OrganizationRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(requestMapper.toDto(any(OrganizationRequest.class))).thenReturn(
                OrganizationRequestDto.builder()
                    .id(testRequestId)
                    .status(OrganizationRequestStatus.APPROVED)
                    .build()
            );

            // Slug уже занят - выбрасываем исключение
            when(organizationService.createFromApprovedRequest(any(OrganizationRequest.class)))
                .thenThrow(new OrganizationSlugAlreadyExistsException(testSlug));

            // When
            OrganizationRequestDto result = service.approve(testRequestId, testAdminId);

            // Then
            // Approve успешен, несмотря на ошибку создания организации
            assertThat(result.status()).isEqualTo(OrganizationRequestStatus.APPROVED);
            verify(requestRepository).save(any(OrganizationRequest.class));
            verify(eventPublisher).publish(any(OrganizationRequestApprovedEvent.class));
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
    @DisplayName("Reject")
    class Reject {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Отклоняет pending запрос с причиной")
        void reject_PendingRequest_RejectsSuccessfully() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.PENDING);
            String rejectComment = FAKER.lorem().sentence(10);
            RejectOrganizationRequestRequest rejectRequest = new RejectOrganizationRequestRequest(rejectComment);

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(testAdminId)).thenReturn(Optional.of(testAdmin));
            when(requestRepository.save(any(OrganizationRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(requestMapper.toDto(any(OrganizationRequest.class))).thenReturn(
                OrganizationRequestDto.builder()
                    .id(testRequestId)
                    .status(OrganizationRequestStatus.REJECTED)
                    .reviewComment(rejectComment)
                    .build()
            );

            // When
            OrganizationRequestDto result = service.reject(testRequestId, testAdminId, rejectRequest);

            // Then
            assertThat(result.status()).isEqualTo(OrganizationRequestStatus.REJECTED);
            assertThat(result.reviewComment()).isEqualTo(rejectComment);
            verify(requestRepository).save(any(OrganizationRequest.class));

            // Проверяем содержимое опубликованного события
            ArgumentCaptor<OrganizationRequestRejectedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrganizationRequestRejectedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            OrganizationRequestRejectedEvent event = eventCaptor.getValue();
            assertThat(event.getRequestId()).isEqualTo(testRequestId);
            assertThat(event.getUserId()).isEqualTo(testUserId);
            assertThat(event.getOrganizationName()).isEqualTo(testName);
            assertThat(event.getRejectionReason()).isEqualTo(rejectComment);
            assertThat(event.getRejectedById()).isEqualTo(testAdminId);
            assertThat(event.getEventType()).isEqualTo("organization.request.rejected");
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает исключение при отклонении уже рассмотренного запроса")
        void reject_AlreadyReviewed_ThrowsException() {
            // Given
            OrganizationRequest request = createTestRequest(testUser, OrganizationRequestStatus.REJECTED);
            RejectOrganizationRequestRequest rejectRequest = new RejectOrganizationRequestRequest("причина");

            when(requestRepository.findByIdWithUser(testRequestId)).thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> service.reject(testRequestId, testAdminId, rejectRequest))
                .isInstanceOf(OrganizationRequestAlreadyReviewedException.class);

            verify(requestRepository, never()).save(any());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
    @DisplayName("GetByUser")
    class GetByUser {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Возвращает все запросы пользователя")
        void getByUser_ReturnsUserRequests() {
            // Given
            OrganizationRequest request1 = createTestRequest(testUser, OrganizationRequestStatus.PENDING);
            OrganizationRequest request2 = createTestRequest(testUser, OrganizationRequestStatus.APPROVED);

            when(requestRepository.findByUserId(testUserId)).thenReturn(List.of(request1, request2));
            when(requestMapper.toDto(any(OrganizationRequest.class))).thenReturn(
                OrganizationRequestDto.builder().id(testRequestId).build()
            );

            // When
            List<OrganizationRequestDto> result = service.getByUser(testUserId);

            // Then
            assertThat(result).hasSize(2);
        }
    }
}
