package ru.aqstream.user.db.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.Organization;

/**
 * Репозиторий для работы с организациями.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Находит организацию по ID с загрузкой владельца.
     *
     * @param id идентификатор организации
     * @return организация с владельцем
     */
    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.owner WHERE o.id = :id")
    Optional<Organization> findByIdWithOwner(@Param("id") UUID id);

    /**
     * Находит организацию по slug.
     *
     * @param slug URL-slug
     * @return организация
     */
    Optional<Organization> findBySlug(String slug);

    /**
     * Проверяет, занят ли slug среди активных организаций.
     *
     * @param slug URL-slug (lowercase)
     * @return true если slug занят
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END "
        + "FROM Organization o WHERE LOWER(o.slug) = LOWER(:slug)")
    boolean existsBySlug(@Param("slug") String slug);
}
