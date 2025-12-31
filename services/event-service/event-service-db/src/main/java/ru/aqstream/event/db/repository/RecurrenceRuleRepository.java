package ru.aqstream.event.db.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aqstream.event.db.entity.RecurrenceRule;

/**
 * Репозиторий для правил повторения событий.
 */
@Repository
public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, UUID> {

    /**
     * Находит правило по ID и tenant.
     *
     * @param id       идентификатор правила
     * @param tenantId идентификатор организации
     * @return правило
     */
    Optional<RecurrenceRule> findByIdAndTenantId(UUID id, UUID tenantId);
}
