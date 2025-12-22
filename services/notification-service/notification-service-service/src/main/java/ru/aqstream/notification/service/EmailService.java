package ru.aqstream.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки email через SMTP.
 *
 * <p>Используется только для аутентификации:
 * <ul>
 *     <li>Верификация email при регистрации</li>
 *     <li>Сброс пароля</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@aqstream.ru}")
    private String fromAddress;

    @Value("${spring.mail.from-name:AqStream}")
    private String fromName;

    /**
     * Отправляет email.
     *
     * @param to      email получателя
     * @param subject тема письма
     * @param body    тело письма (HTML)
     * @return true если письмо отправлено успешно
     */
    public boolean send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);  // HTML

            mailSender.send(message);
            log.debug("Email отправлен: to={}, subject={}", maskEmail(to), subject);
            return true;

        } catch (MessagingException e) {
            log.error("Ошибка создания email: to={}, error={}", maskEmail(to), e.getMessage());
            return false;
        } catch (MailException e) {
            log.error("Ошибка отправки email: to={}, error={}", maskEmail(to), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке email: to={}, error={}", maskEmail(to), e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет доступность SMTP сервера.
     *
     * @return true если SMTP сервер доступен
     */
    public boolean isAvailable() {
        try {
            // Попытка создать соединение
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("SMTP сервер недоступен: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Маскирует email для логов.
     *
     * @param email email адрес
     * @return замаскированный email (t***@example.com)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
