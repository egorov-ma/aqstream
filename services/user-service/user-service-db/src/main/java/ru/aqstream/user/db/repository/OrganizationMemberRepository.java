package ru.aqstream.user.db.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.db.entity.OrganizationMember;

/**
 * Репозиторий для работы с членами организаций.
 */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    /**
     * Находит членство пользователя в организации.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return членство или empty
     */
    @Query("SELECT m FROM OrganizationMember m "
        + "WHERE m.organization.id = :organizationId AND m.user.id = :userId")
    Optional<OrganizationMember> findByOrganizationIdAndUserId(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId
    );

    /**
     * Находит членство по ID с загрузкой связей.
     *
     * @param id идентификатор членства
     * @return членство с загруженными связями
     */
    @Query("SELECT m FROM OrganizationMember m "
        + "LEFT JOIN FETCH m.organization "
        + "LEFT JOIN FETCH m.user "
        + "LEFT JOIN FETCH m.invitedBy "
        + "WHERE m.id = :id")
    Optional<OrganizationMember> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Находит всех членов организации.
     *
     * @param organizationId идентификатор организации
     * @return список членов
     */
    @EntityGraph(attributePaths = {"user", "invitedBy"})
    @Query("SELECT m FROM OrganizationMember m WHERE m.organization.id = :organizationId ORDER BY m.joinedAt")
    List<OrganizationMember> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Находит все организации пользователя (членства).
     *
     * @param userId идентификатор пользователя
     * @return список членств с загруженными организациями
     */
    @EntityGraph(attributePaths = {"organization", "organization.owner"})
    @Query("SELECT m FROM OrganizationMember m WHERE m.user.id = :userId ORDER BY m.joinedAt DESC")
    List<OrganizationMember> findByUserId(@Param("userId") UUID userId);

    /**
     * Проверяет, является ли пользователь членом организации.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return true если пользователь — член организации
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END "
        + "FROM OrganizationMember m "
        + "WHERE m.organization.id = :organizationId AND m.user.id = :userId")
    boolean existsByOrganizationIdAndUserId(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId
    );

    /**
     * Находит владельца организации.
     *
     * @param organizationId идентификатор организации
     * @return членство владельца
     */
    @Query("SELECT m FROM OrganizationMember m "
        + "LEFT JOIN FETCH m.user "
        + "WHERE m.organization.id = :organizationId AND m.role = 'OWNER'")
    Optional<OrganizationMember> findOwnerByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Считает количество членов с указанной ролью.
     *
     * @param organizationId идентификатор организации
     * @param role           роль
     * @return количество членов
     */
    @Query("SELECT COUNT(m) FROM OrganizationMember m "
        + "WHERE m.organization.id = :organizationId AND m.role = :role")
    int countByOrganizationIdAndRole(
        @Param("organizationId") UUID organizationId,
        @Param("role") OrganizationRole role
    );

    /**
     * Удаляет все членства пользователя во всех организациях.
     * Используется при удалении пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM OrganizationMember m WHERE m.user.id = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Удаляет всех членов организации.
     * Используется при удалении организации.
     *
     * @param organizationId идентификатор организации
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM OrganizationMember m WHERE m.organization.id = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") UUID organizationId);
}
