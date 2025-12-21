package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие отклонения запроса на организацию.
 * Публикуется для уведомления пользователя об отклонении.
 */
public class OrganizationRequestRejectedEvent extends DomainEvent {

    private final UUID requestId;
    private final UUID userId;
    private final String organizationName;
    private final String rejectionReason;
    private final UUID rejectedById;

    /**
     * Создаёт событие отклонения запроса.
     *
     * @param requestId       идентификатор запроса
     * @param userId          идентификатор пользователя
     * @param organizationName название организации
     * @param rejectionReason причина отклонения
     * @param rejectedById    идентификатор администратора
     */
    public OrganizationRequestRejectedEvent(
        UUID requestId,
        UUID userId,
        String organizationName,
        String rejectionReason,
        UUID rejectedById
    ) {
        super();
        this.requestId = requestId;
        this.userId = userId;
        this.organizationName = organizationName;
        this.rejectionReason = rejectionReason;
        this.rejectedById = rejectedById;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public UUID getRejectedById() {
        return rejectedById;
    }

    @Override
    public String getEventType() {
        return "organization.request.rejected";
    }

    @Override
    public UUID getAggregateId() {
        return requestId;
    }
}
