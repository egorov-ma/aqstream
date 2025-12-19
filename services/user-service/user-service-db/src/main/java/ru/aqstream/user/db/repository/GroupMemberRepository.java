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
import ru.aqstream.user.db.entity.GroupMember;

/**
 * Репозиторий для работы с участниками групп.
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    /**
     * Находит участие пользователя в группе.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @return участие или empty
     */
    @Query("SELECT m FROM GroupMember m "
        + "WHERE m.group.id = :groupId AND m.user.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(
        @Param("groupId") UUID groupId,
        @Param("userId") UUID userId
    );

    /**
     * Находит всех участников группы.
     *
     * @param groupId идентификатор группы
     * @return список участников
     */
    @EntityGraph(attributePaths = {"user", "invitedBy"})
    @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId ORDER BY m.joinedAt")
    List<GroupMember> findByGroupId(@Param("groupId") UUID groupId);

    /**
     * Находит все группы пользователя (участия).
     *
     * @param userId идентификатор пользователя
     * @return список участий с загруженными группами
     */
    @EntityGraph(attributePaths = {"group", "group.organization"})
    @Query("SELECT m FROM GroupMember m WHERE m.user.id = :userId ORDER BY m.joinedAt DESC")
    List<GroupMember> findByUserId(@Param("userId") UUID userId);

    /**
     * Проверяет, является ли пользователь участником группы.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @return true если пользователь — участник группы
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END "
        + "FROM GroupMember m "
        + "WHERE m.group.id = :groupId AND m.user.id = :userId")
    boolean existsByGroupIdAndUserId(
        @Param("groupId") UUID groupId,
        @Param("userId") UUID userId
    );

    /**
     * Удаляет всех участников группы.
     * Используется при удалении группы.
     *
     * @param groupId идентификатор группы
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM GroupMember m WHERE m.group.id = :groupId")
    int deleteByGroupId(@Param("groupId") UUID groupId);

    /**
     * Удаляет все участия пользователя во всех группах.
     * Используется при удалении пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM GroupMember m WHERE m.user.id = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
