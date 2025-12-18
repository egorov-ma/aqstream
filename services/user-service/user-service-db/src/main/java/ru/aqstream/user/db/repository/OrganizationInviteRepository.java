package ru.aqstream.user.db.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.OrganizationInvite;

/**
 * Репозиторий для работы с приглашениями в организации.
 */
@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, UUID> {

    /**
     * Находит приглашение по коду.
     *
     * @param inviteCode код приглашения
     * @return приглашение с загруженными связями
     */
    @Query("SELECT i FROM OrganizationInvite i "
        + "LEFT JOIN FETCH i.organization "
        + "LEFT JOIN FETCH i.invitedBy "
        + "WHERE i.inviteCode = :inviteCode")
    Optional<OrganizationInvite> findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * Находит все приглашения организации.
     *
     * @param organizationId идентификатор организации
     * @return список приглашений
     */
    @EntityGraph(attributePaths = {"invitedBy", "usedBy"})
    @Query("SELECT i FROM OrganizationInvite i "
        + "WHERE i.organization.id = :organizationId "
        + "ORDER BY i.createdAt DESC")
    List<OrganizationInvite> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Находит активные (не использованные и не истёкшие) приглашения организации.
     *
     * @param organizationId идентификатор организации
     * @param now            текущее время
     * @return список активных приглашений
     */
    @EntityGraph(attributePaths = {"invitedBy"})
    @Query("SELECT i FROM OrganizationInvite i "
        + "WHERE i.organization.id = :organizationId "
        + "AND i.usedAt IS NULL "
        + "AND i.expiresAt > :now "
        + "ORDER BY i.createdAt DESC")
    List<OrganizationInvite> findActiveByOrganizationId(
        @Param("organizationId") UUID organizationId,
        @Param("now") Instant now
    );

    /**
     * Удаляет истёкшие приглашения.
     *
     * @param now текущее время
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM OrganizationInvite i WHERE i.expiresAt < :now AND i.usedAt IS NULL")
    int deleteExpiredBefore(@Param("now") Instant now);

    /**
     * Удаляет все приглашения организации.
     * Используется при удалении организации.
     *
     * @param organizationId идентификатор организации
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM OrganizationInvite i WHERE i.organization.id = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") UUID organizationId);
}
