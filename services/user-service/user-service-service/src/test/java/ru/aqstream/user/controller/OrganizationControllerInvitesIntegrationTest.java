package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.InviteMemberRequest;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationInvite;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Интеграционные тесты для приглашений в организацию.
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("OrganizationController Invites Integration Tests")
class OrganizationControllerInvitesIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String BASE_URL = "/api/v1/organizations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private OrganizationInviteRepository inviteRepository;

    @Autowired
    private OrganizationRequestRepository requestRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        inviteRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        requestRepository.deleteAll();

        testUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        testUser = userRepository.save(testUser);

        otherUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        otherUser = userRepository.save(otherUser);
    }

    private String fakeSlug() {
        return FAKER.internet().slug().toLowerCase().replace("_", "-");
    }

    private Organization createOrganization(User owner, String slug) {
        Organization org = Organization.create(
            owner,
            FAKER.company().name(),
            slug,
            FAKER.company().catchPhrase()
        );
        org = organizationRepository.save(org);

        OrganizationMember ownerMember = OrganizationMember.createOwner(org, owner);
        memberRepository.save(ownerMember);

        return org;
    }

    @Nested
    @DisplayName("POST /api/v1/organizations/{id}/invite")
    class CreateInvite {

        @Test
        @DisplayName("OWNER может создать приглашение")
        void createInvite_Owner_ReturnsCreated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            InviteMemberRequest request = new InviteMemberRequest("@telegram_user");

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andExpect(jsonPath("$.telegramDeeplink").exists())
                .andExpect(jsonPath("$.role").value("MODERATOR"));

            var invites = inviteRepository.findByOrganizationId(org.getId());
            assertThat(invites).hasSize(1);
            assertThat(invites.get(0).getTelegramUsername()).isEqualTo("@telegram_user");
        }

        @Test
        @DisplayName("MODERATOR может создать приглашение")
        void createInvite_Moderator_ReturnsCreated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            InviteMemberRequest request = new InviteMemberRequest(null);

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").exists());
        }

        @Test
        @DisplayName("не-член не может создать приглашение")
        void createInvite_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            InviteMemberRequest request = new InviteMemberRequest(null);

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/invites")
    class GetActiveInvites {

        @Test
        @DisplayName("возвращает список активных приглашений")
        void getInvites_Member_ReturnsInviteList() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationInvite invite = OrganizationInvite.create(org, testUser, "@invited_user");
            inviteRepository.save(invite);

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/invites")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inviteCode").value(invite.getInviteCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organizations/join/{inviteCode}")
    class AcceptInvite {

        @Test
        @DisplayName("пользователь может принять приглашение")
        void acceptInvite_ValidCode_JoinsOrganization() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite = inviteRepository.save(invite);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(otherUser.getId().toString()))
                .andExpect(jsonPath("$.role").value("MODERATOR"));

            assertThat(memberRepository.existsByOrganizationIdAndUserId(org.getId(), otherUser.getId()))
                .isTrue();

            OrganizationInvite usedInvite = inviteRepository.findById(invite.getId()).orElseThrow();
            assertThat(usedInvite.isUsed()).isTrue();
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего кода")
        void acceptInvite_InvalidCode_Returns404() throws Exception {
            mockMvc.perform(post(BASE_URL + "/join/invalid_code_12345678901234")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 409 для использованного приглашения")
        void acceptInvite_AlreadyUsed_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite.markAsUsed(otherUser);
            invite = inviteRepository.save(invite);

            User thirdUser = User.createWithEmail(
                FAKER.internet().emailAddress(),
                "hashedPassword",
                FAKER.name().firstName(),
                FAKER.name().lastName()
            );
            thirdUser = userRepository.save(thirdUser);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, thirdUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite_already_used"));
        }

        @Test
        @DisplayName("возвращает 409 если пользователь уже член организации")
        void acceptInvite_AlreadyMember_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember member = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(member);

            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite = inviteRepository.save(invite);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_organization_member"));
        }
    }
}
