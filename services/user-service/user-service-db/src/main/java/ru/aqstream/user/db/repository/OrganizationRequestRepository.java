package ru.aqstream.user.db.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;
import ru.aqstream.user.db.entity.OrganizationRequest;

/**
 * Репозиторий для работы с запросами на создание организаций.
 */
@Repository
public interface OrganizationRequestRepository extends JpaRepository<OrganizationRequest, UUID> {

    /**
     * Находит запрос по ID с загрузкой пользователя.
     *
     * @param id идентификатор запроса
     * @return запрос с пользователем
     */
    @Query("SELECT r FROM OrganizationRequest r LEFT JOIN FETCH r.user WHERE r.id = :id")
    Optional<OrganizationRequest> findByIdWithUser(@Param("id") UUID id);

    /**
     * Находит все запросы пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список запросов
     */
    @Query("SELECT r FROM OrganizationRequest r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<OrganizationRequest> findByUserId(@Param("userId") UUID userId);

    /**
     * Проверяет, есть ли у пользователя запрос в статусе PENDING.
     *
     * @param userId идентификатор пользователя
     * @return true если есть pending запрос
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END "
        + "FROM OrganizationRequest r WHERE r.user.id = :userId AND r.status = 'PENDING'")
    boolean existsPendingByUserId(@Param("userId") UUID userId);

    /**
     * Проверяет, занят ли slug среди PENDING запросов.
     *
     * @param slug URL-slug (lowercase)
     * @return true если slug занят
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END "
        + "FROM OrganizationRequest r WHERE LOWER(r.slug) = LOWER(:slug) AND r.status = 'PENDING'")
    boolean existsPendingBySlug(@Param("slug") String slug);

    /**
     * Находит все запросы с указанным статусом.
     *
     * <p>Использует @EntityGraph вместо JOIN FETCH для корректной работы пагинации.
     * JOIN FETCH с пагинацией приводит к выгрузке всех записей в память.</p>
     *
     * @param status   статус запроса
     * @param pageable параметры пагинации
     * @return страница запросов
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM OrganizationRequest r WHERE r.status = :status")
    Page<OrganizationRequest> findByStatus(@Param("status") OrganizationRequestStatus status, Pageable pageable);

    /**
     * Находит все запросы (для админа) с пагинацией.
     *
     * <p>Использует @EntityGraph вместо JOIN FETCH для корректной работы пагинации.</p>
     *
     * @param pageable параметры пагинации
     * @return страница запросов
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM OrganizationRequest r ORDER BY r.createdAt DESC")
    Page<OrganizationRequest> findAllWithUser(Pageable pageable);

    /**
     * Находит все PENDING запросы с пагинацией.
     *
     * <p>Использует @EntityGraph вместо JOIN FETCH для корректной работы пагинации.</p>
     *
     * @param pageable параметры пагинации
     * @return страница запросов
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM OrganizationRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    Page<OrganizationRequest> findPendingWithUser(Pageable pageable);

    /**
     * Находит APPROVED запрос пользователя (для проверки права создать организацию).
     *
     * @param userId идентификатор пользователя
     * @return одобренный запрос или empty
     */
    @Query("SELECT r FROM OrganizationRequest r WHERE r.user.id = :userId AND r.status = 'APPROVED'")
    Optional<OrganizationRequest> findApprovedByUserId(@Param("userId") UUID userId);
}
