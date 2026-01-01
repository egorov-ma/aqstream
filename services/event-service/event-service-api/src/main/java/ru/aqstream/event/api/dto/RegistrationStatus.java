package ru.aqstream.event.api.dto;

/**
 * Статус регистрации на событие.
 *
 * <p>Жизненный цикл в Phase 2:</p>
 * <pre>
 * → CONFIRMED (бесплатные билеты сразу подтверждаются)
 *       ↓
 *   CANCELLED (при отмене участником или организатором)
 * </pre>
 *
 * <p>В Phase 3 добавятся:</p>
 * <ul>
 *   <li>RESERVED → ожидание оплаты</li>
 *   <li>PENDING → обработка платежа</li>
 *   <li>EXPIRED → истекло время резервации</li>
 * </ul>
 */
public enum RegistrationStatus {

    /**
     * Регистрация подтверждена.
     */
    CONFIRMED,

    /**
     * Участник прошёл check-in на событии.
     */
    CHECKED_IN,

    /**
     * Регистрация отменена (участником или организатором).
     */
    CANCELLED,

    /**
     * Зарезервировано, ожидает оплаты (Phase 3).
     */
    RESERVED,

    /**
     * В процессе обработки платежа (Phase 3).
     */
    PENDING,

    /**
     * Резервация истекла (Phase 3).
     */
    EXPIRED
}
