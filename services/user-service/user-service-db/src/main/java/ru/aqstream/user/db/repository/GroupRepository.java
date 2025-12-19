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
import ru.aqstream.user.db.entity.Group;

/**
 * Репозиторий для работы с группами.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    /**
     * Находит группу по ID с загрузкой связей.
     *
     * @param id идентификатор группы
     * @return группа с загруженными связями
     */
    @Query("SELECT g FROM Group g "
        + "LEFT JOIN FETCH g.organization "
        + "LEFT JOIN FETCH g.createdBy "
        + "WHERE g.id = :id")
    Optional<Group> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Находит группу по инвайт-коду.
     *
     * @param inviteCode код приглашения
     * @return группа с загруженными связями
     */
    @Query("SELECT g FROM Group g "
        + "LEFT JOIN FETCH g.organization "
        + "LEFT JOIN FETCH g.createdBy "
        + "WHERE g.inviteCode = :inviteCode")
    Optional<Group> findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * Находит все группы организации.
     *
     * @param organizationId идентификатор организации
     * @return список групп
     */
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT g FROM Group g WHERE g.organization.id = :organizationId ORDER BY g.createdAt DESC")
    List<Group> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Находит группы организации, в которых пользователь является участником.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return список групп
     */
    @Query("SELECT g FROM Group g "
        + "LEFT JOIN FETCH g.createdBy "
        + "JOIN GroupMember m ON m.group = g "
        + "WHERE g.organization.id = :organizationId AND m.user.id = :userId "
        + "ORDER BY g.createdAt DESC")
    List<Group> findByOrganizationIdAndMemberUserId(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId
    );

    /**
     * Проверяет существование группы с указанным инвайт-кодом.
     *
     * @param inviteCode код приглашения
     * @return true если группа существует
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * Считает количество участников группы.
     *
     * @param groupId идентификатор группы
     * @return количество участников
     */
    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.group.id = :groupId")
    int countMembersByGroupId(@Param("groupId") UUID groupId);

    /**
     * Удаляет все группы организации.
     * Используется при удалении организации.
     *
     * @param organizationId идентификатор организации
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM Group g WHERE g.organization.id = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") UUID organizationId);
}
