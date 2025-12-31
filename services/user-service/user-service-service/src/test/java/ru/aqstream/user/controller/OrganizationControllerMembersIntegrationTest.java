package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.hamcrest.Matchers;
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
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UpdateMemberRoleRequest;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.util.CookieUtils;

/**
 * Интеграционные тесты для управления членами организации и переключения контекста.
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("OrganizationController Members Integration Tests")
class OrganizationControllerMembersIntegrationTest extends PostgresTestContainer {

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
    @DisplayName("POST /api/v1/organizations/{id}/switch")
    class SwitchOrganization {

        @Test
        @DisplayName("переключает на организацию и возвращает новые токены")
        void switch_Member_ReturnsNewTokens() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/switch")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                // refreshToken теперь передаётся через httpOnly cookie
                .andExpect(jsonPath("$.refreshToken").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // Проверяем, что refresh token установлен в cookie
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie",
                    Matchers.containsString(CookieUtils.REFRESH_TOKEN_COOKIE_NAME + "=")));
        }

        @Test
        @DisplayName("не-член не может переключиться")
        void switch_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/switch")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/members")
    class GetMembers {

        @Test
        @DisplayName("возвращает список членов организации")
        void getMembers_Member_ReturnsMemberList() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/members")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("не-член не может получить список членов")
        void getMembers_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/members")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/organizations/{id}/members/{userId}")
    class UpdateMemberRole {

        @Test
        @DisplayName("OWNER может изменить роль члена")
        void updateRole_Owner_ReturnsUpdated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(OrganizationRole.OWNER);

            mockMvc.perform(put(BASE_URL + "/" + org.getId() + "/members/" + otherUser.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));

            OrganizationMember oldOwner = memberRepository
                .findByOrganizationIdAndUserId(org.getId(), testUser.getId())
                .orElseThrow();
            assertThat(oldOwner.getRole()).isEqualTo(OrganizationRole.MODERATOR);
        }

        @Test
        @DisplayName("MODERATOR не может изменить роль")
        void updateRole_Moderator_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(OrganizationRole.OWNER);

            mockMvc.perform(put(BASE_URL + "/" + org.getId() + "/members/" + testUser.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/organizations/{id}/members/{userId}")
    class RemoveMember {

        @Test
        @DisplayName("OWNER может удалить MODERATOR")
        void removeMember_OwnerRemovesModerator_ReturnsNoContent() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(delete(BASE_URL + "/" + org.getId() + "/members/" + otherUser.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isNoContent());

            assertThat(memberRepository.findByOrganizationIdAndUserId(org.getId(), otherUser.getId()))
                .isEmpty();
        }

        @Test
        @DisplayName("нельзя удалить OWNER")
        void removeMember_CannotRemoveOwner_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(delete(BASE_URL + "/" + org.getId() + "/members/" + testUser.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot_remove_owner"));
        }
    }
}
