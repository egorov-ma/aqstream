package ru.aqstream.event.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.common.api.exception.ForbiddenException;
import ru.aqstream.user.api.dto.OrganizationMembershipDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.client.UserClient;

/**
 * Сервис проверки прав на создание и управление событиями.
 * Проверяет членство пользователя в организации через user-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPermissionService {

    private final UserClient userClient;

    /**
     * Проверяет право пользователя на создание события в организации.
     *
     * <p>Правила:
     * <ul>
     *   <li>Админы могут создавать события для любой организации</li>
     *   <li>OWNER и MODERATOR могут создавать события в своей организации</li>
     *   <li>Обычные пользователи не могут создавать события</li>
     * </ul>
     *
     * @param userId         идентификатор пользователя
     * @param organizationId идентификатор организации
     * @param isAdmin        true если пользователь является админом платформы
     * @throws ForbiddenException если пользователь не имеет права создавать события
     */
    public void validateCreatePermission(UUID userId, UUID organizationId, boolean isAdmin) {
        validateOrganizationPermission(userId, organizationId, isAdmin, "создания событий");
    }

    /**
     * Проверяет право пользователя на управление событием.
     * Требуется для: update, delete, publish, unpublish, cancel, complete.
     *
     * <p>Правила:
     * <ul>
     *   <li>Админы могут управлять любыми событиями</li>
     *   <li>OWNER и MODERATOR могут управлять событиями своей организации</li>
     *   <li>Обычные пользователи не могут управлять событиями</li>
     * </ul>
     *
     * @param userId         идентификатор пользователя
     * @param organizationId идентификатор организации (tenant_id события)
     * @param isAdmin        true если пользователь является админом платформы
     * @throws ForbiddenException если пользователь не имеет права управлять событием
     */
    public void validateManagePermission(UUID userId, UUID organizationId, boolean isAdmin) {
        validateOrganizationPermission(userId, organizationId, isAdmin, "управления событиями");
    }

    /**
     * Проверяет право пользователя на просмотр событий в dashboard.
     * Требуется для: findAll, getById, getActivity.
     *
     * <p>Правила:
     * <ul>
     *   <li>Админы могут просматривать любые события</li>
     *   <li>OWNER и MODERATOR могут просматривать события своей организации</li>
     *   <li>Обычные пользователи не имеют доступа к dashboard событий</li>
     * </ul>
     *
     * @param userId         идентификатор пользователя
     * @param organizationId идентификатор организации
     * @param isAdmin        true если пользователь является админом платформы
     * @throws ForbiddenException если пользователь не имеет права просматривать события
     */
    public void validateViewPermission(UUID userId, UUID organizationId, boolean isAdmin) {
        validateOrganizationPermission(userId, organizationId, isAdmin, "просмотра событий");
    }

    /**
     * Общая проверка прав пользователя в организации.
     *
     * @param userId         идентификатор пользователя
     * @param organizationId идентификатор организации
     * @param isAdmin        true если пользователь является админом платформы
     * @param action         описание действия для сообщения об ошибке
     * @throws ForbiddenException если пользователь не имеет прав
     */
    private void validateOrganizationPermission(UUID userId, UUID organizationId, boolean isAdmin, String action) {
        // Админы могут выполнять любые действия
        if (isAdmin) {
            log.debug("Админ выполняет действие: userId={}, organizationId={}, action={}",
                userId, organizationId, action);
            return;
        }

        // Проверяем наличие organizationId
        if (organizationId == null) {
            log.info("Отказ: организация не указана, userId={}, action={}", userId, action);
            throw new ForbiddenException("Выберите организацию для " + action);
        }

        // Для обычных пользователей проверяем членство в организации
        OrganizationMembershipDto membership;
        try {
            membership = userClient.getMembershipRole(organizationId, userId);
        } catch (Exception e) {
            log.error("Ошибка проверки членства в организации: userId={}, organizationId={}, ошибка={}",
                userId, organizationId, e.getMessage());
            throw new ForbiddenException("Не удалось проверить права доступа");
        }

        if (!membership.isMember()) {
            log.info("Отказ: пользователь не член организации, userId={}, organizationId={}, action={}",
                userId, organizationId, action);
            throw new ForbiddenException("Пользователь не является членом организации");
        }

        // Только OWNER и MODERATOR имеют права на управление
        if (membership.role() != OrganizationRole.OWNER && membership.role() != OrganizationRole.MODERATOR) {
            log.info("Отказ: недостаточно прав, userId={}, organizationId={}, role={}, action={}",
                userId, organizationId, membership.role(), action);
            throw new ForbiddenException("Недостаточно прав для " + action + ". Требуется роль OWNER или MODERATOR");
        }

        log.debug("Проверка прав пройдена: userId={}, organizationId={}, role={}, action={}",
            userId, organizationId, membership.role(), action);
    }
}
