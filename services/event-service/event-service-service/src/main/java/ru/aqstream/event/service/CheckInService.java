package ru.aqstream.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.event.api.dto.CheckInInfoDto;
import ru.aqstream.event.api.dto.CheckInResultDto;
import ru.aqstream.event.api.exception.AlreadyCheckedInException;
import ru.aqstream.event.api.exception.CheckInNotAllowedException;
import ru.aqstream.event.api.exception.RegistrationNotFoundByCodeException;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.repository.RegistrationRepository;

/**
 * Сервис check-in участников на событие.
 *
 * <p>Обрабатывает проверку регистрации по QR-коду и выполнение check-in.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final RegistrationRepository registrationRepository;

    /**
     * Получает информацию о регистрации по confirmation code.
     * Используется для отображения данных перед подтверждением check-in.
     *
     * @param confirmationCode код подтверждения
     * @return информация о регистрации
     * @throws RegistrationNotFoundByCodeException если регистрация не найдена
     */
    @Transactional(readOnly = true)
    public CheckInInfoDto getCheckInInfo(String confirmationCode) {
        log.debug("Запрос информации о check-in: confirmationCode={}", confirmationCode);

        Registration registration = findByConfirmationCode(confirmationCode);

        return mapToCheckInInfo(registration);
    }

    /**
     * Выполняет check-in участника по confirmation code.
     *
     * @param confirmationCode код подтверждения
     * @return результат check-in
     * @throws RegistrationNotFoundByCodeException если регистрация не найдена
     * @throws AlreadyCheckedInException           если участник уже прошёл check-in
     * @throws CheckInNotAllowedException          если check-in невозможен
     */
    @Transactional
    public CheckInResultDto checkIn(String confirmationCode) {
        log.info("Выполнение check-in: confirmationCode={}", confirmationCode);

        Registration registration = findByConfirmationCode(confirmationCode);

        // Проверяем, не прошёл ли участник check-in ранее
        if (registration.isCheckedIn()) {
            log.warn("Повторный check-in: confirmationCode={}, checkedInAt={}",
                confirmationCode, registration.getCheckedInAt());
            throw new AlreadyCheckedInException(
                registration.getId(),
                confirmationCode,
                registration.getCheckedInAt()
            );
        }

        // Проверяем статус регистрации
        if (!registration.isConfirmed()) {
            log.warn("Check-in для неподтверждённой регистрации: confirmationCode={}, status={}",
                confirmationCode, registration.getStatus());
            throw new CheckInNotAllowedException(
                registration.getId(),
                confirmationCode,
                registration.getStatus()
            );
        }

        // Выполняем check-in
        registration.checkIn();
        registrationRepository.save(registration);

        log.info("Check-in выполнен: registrationId={}, confirmationCode={}, eventId={}",
            registration.getId(), confirmationCode, registration.getEvent().getId());

        return CheckInResultDto.success(
            registration.getId(),
            registration.getConfirmationCode(),
            registration.getEvent().getTitle(),
            registration.getTicketType().getName(),
            registration.getFirstName(),
            registration.getLastName(),
            registration.getCheckedInAt()
        );
    }

    /**
     * Проверяет, прошёл ли участник check-in.
     *
     * @param confirmationCode код подтверждения
     * @return true если участник уже прошёл check-in
     * @throws RegistrationNotFoundByCodeException если регистрация не найдена
     */
    @Transactional(readOnly = true)
    public boolean isCheckedIn(String confirmationCode) {
        Registration registration = findByConfirmationCode(confirmationCode);
        return registration.isCheckedIn();
    }

    /**
     * Находит регистрацию по confirmation code.
     *
     * @param confirmationCode код подтверждения
     * @return регистрация
     * @throws RegistrationNotFoundByCodeException если регистрация не найдена
     */
    private Registration findByConfirmationCode(String confirmationCode) {
        return registrationRepository.findByConfirmationCode(confirmationCode)
            .orElseThrow(() -> {
                log.debug("Регистрация не найдена: confirmationCode={}", confirmationCode);
                return new RegistrationNotFoundByCodeException(confirmationCode);
            });
    }

    /**
     * Преобразует регистрацию в DTO для check-in.
     */
    private CheckInInfoDto mapToCheckInInfo(Registration registration) {
        return new CheckInInfoDto(
            registration.getId(),
            registration.getConfirmationCode(),
            registration.getEvent().getId(),
            registration.getEvent().getTitle(),
            registration.getEvent().getStartsAt(),
            registration.getTicketType().getName(),
            registration.getFirstName(),
            registration.getLastName(),
            registration.getEmail(),
            registration.getStatus(),
            registration.isCheckedIn(),
            registration.getCheckedInAt()
        );
    }
}
