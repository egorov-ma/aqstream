package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.util.List;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.CreateOrganizationRequestRequest;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationRequestDto;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.UserRepository;

@IntegrationTest
@SpringBootTest
@ActiveProfiles("test")
@Feature(AllureFeatures.Features.ORGANIZATIONS)
@Story(AllureFeatures.Stories.ORGANIZATION_REQUESTS)
@DisplayName("OrganizationRequest Approval Integration Test")
class OrganizationRequestApprovalIntegrationTest extends PostgresTestContainer {

    @Autowired
    private OrganizationRequestService requestService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    private static final Faker FAKER = new Faker();

    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        // Создаём тестового пользователя и админа
        testUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        testUser = userRepository.save(testUser);

        testAdmin = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        testAdmin.setAdmin(true);
        testAdmin = userRepository.save(testAdmin);
    }

    @Test
    @Severity(CRITICAL)
    @Transactional
    @DisplayName("При одобрении заявки автоматически создаётся организация и membership")
    void approve_CreatesOrganizationAutomatically() {
        // Given: Пользователь подал заявку
        String orgName = FAKER.company().name();
        String slug = FAKER.internet().slug().toLowerCase().replace("_", "-");
        String description = FAKER.company().catchPhrase();

        CreateOrganizationRequestRequest createRequest = new CreateOrganizationRequestRequest(
            orgName, slug, description
        );

        OrganizationRequestDto request = requestService.create(testUser.getId(), createRequest);
        assertThat(request.status()).isEqualTo(OrganizationRequestStatus.PENDING);

        // When: Админ одобряет заявку
        OrganizationRequestDto approvedRequest = requestService.approve(
            request.id(),
            testAdmin.getId()
        );

        // Then: Заявка одобрена
        assertThat(approvedRequest.status()).isEqualTo(OrganizationRequestStatus.APPROVED);

        // AND: Организация автоматически создана
        List<OrganizationDto> organizations = organizationService.getMyOrganizations(testUser.getId());
        assertThat(organizations).hasSize(1);

        OrganizationDto createdOrg = organizations.get(0);
        assertThat(createdOrg.name()).isEqualTo(orgName);
        assertThat(createdOrg.slug()).isEqualTo(slug.toLowerCase());
        assertThat(createdOrg.description()).isEqualTo(description);
        assertThat(createdOrg.ownerId()).isEqualTo(testUser.getId());

        // AND: OrganizationMember создан с ролью OWNER
        var members = memberRepository.findByOrganizationId(createdOrg.id());
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(members.get(0).isOwner()).isTrue();

        // AND: Организация существует в БД
        assertThat(organizationRepository.existsBySlug(slug.toLowerCase())).isTrue();
    }
}
