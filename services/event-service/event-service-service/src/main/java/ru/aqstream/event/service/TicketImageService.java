package ru.aqstream.event.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.event.api.exception.TicketGenerationException;
import ru.aqstream.event.db.entity.Registration;

/**
 * Сервис генерации изображений билетов.
 *
 * <p>Создаёт PNG-изображение билета с информацией о событии и QR-кодом
 * для отправки в Telegram.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketImageService {

    /**
     * Ширина билета в пикселях.
     */
    public static final int TICKET_WIDTH = 600;

    /**
     * Высота билета в пикселях.
     */
    public static final int TICKET_HEIGHT = 400;

    /**
     * Размер QR-кода на билете.
     */
    public static final int QR_SIZE = 180;

    /**
     * Отступы от краёв.
     */
    private static final int PADDING = 25;

    /**
     * Форматтер для даты события.
     */
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"));

    private final QrCodeService qrCodeService;

    /**
     * Генерирует изображение билета для регистрации.
     *
     * @param registration регистрация участника
     * @return PNG изображение билета в виде массива байтов
     */
    public byte[] generateTicketImage(Registration registration) {
        log.info("Генерация билета: registrationId={}, confirmationCode={}",
            registration.getId(), registration.getConfirmationCode());

        try {
            // Создаём изображение
            BufferedImage image = new BufferedImage(TICKET_WIDTH, TICKET_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Включаем сглаживание
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Рисуем билет
            drawBackground(g);
            drawEventInfo(g, registration);
            drawParticipantInfo(g, registration);
            drawQrCode(g, registration);
            drawConfirmationCode(g, registration);

            g.dispose();

            // Сохраняем в PNG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);

            log.info("Билет сгенерирован: registrationId={}, bytes={}",
                registration.getId(), outputStream.size());

            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Ошибка генерации билета: registrationId={}, ошибка={}",
                registration.getId(), e.getMessage(), e);
            throw new TicketGenerationException(registration.getId(), e);
        }
    }

    /**
     * Рисует фон билета.
     */
    private void drawBackground(Graphics2D g) {
        // Белый фон
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, TICKET_WIDTH, TICKET_HEIGHT);

        // Синяя полоса сверху (брендинг)
        g.setColor(new Color(59, 130, 246)); // Tailwind blue-500
        g.fillRect(0, 0, TICKET_WIDTH, 60);

        // Логотип/название платформы
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("AqStream", PADDING, 40);

        // Рамка
        g.setColor(new Color(229, 231, 235)); // Tailwind gray-200
        g.drawRect(0, 0, TICKET_WIDTH - 1, TICKET_HEIGHT - 1);

        // Разделительная линия перед QR
        g.setColor(new Color(229, 231, 235));
        int separatorX = TICKET_WIDTH - QR_SIZE - PADDING * 3;
        g.drawLine(separatorX, 70, separatorX, TICKET_HEIGHT - 10);
    }

    /**
     * Рисует информацию о событии.
     */
    private void drawEventInfo(Graphics2D g, Registration registration) {
        int y = 95;
        int maxTextWidth = TICKET_WIDTH - QR_SIZE - PADDING * 4;

        // Название события
        g.setColor(new Color(17, 24, 39)); // Tailwind gray-900
        g.setFont(new Font("SansSerif", Font.BOLD, 20));

        String eventTitle = registration.getEvent().getTitle();
        drawWrappedText(g, eventTitle, PADDING, y, maxTextWidth, 24);

        // Расчёт высоты заголовка (для позиционирования следующих элементов)
        FontMetrics fm = g.getFontMetrics();
        int lines = calculateLines(fm, eventTitle, maxTextWidth);
        y += lines * 24 + 10;

        // Дата и время
        g.setColor(new Color(75, 85, 99)); // Tailwind gray-600
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String dateTime = formatDateTime(registration.getEvent().getStartsAt());
        g.drawString(dateTime, PADDING, y);

        y += 25;

        // Место проведения
        String location = registration.getEvent().getLocationAddress();
        if (location != null && !location.isBlank()) {
            g.drawString(location, PADDING, y);
            y += 20;
        }

        // Тип билета
        y += 10;
        g.setColor(new Color(37, 99, 235)); // Tailwind blue-600
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(registration.getTicketType().getName(), PADDING, y);
    }

    /**
     * Рисует информацию об участнике.
     */
    private void drawParticipantInfo(Graphics2D g, Registration registration) {
        int y = TICKET_HEIGHT - 80;
        int maxTextWidth = TICKET_WIDTH - QR_SIZE - PADDING * 4;

        // Метка
        g.setColor(new Color(107, 114, 128)); // Tailwind gray-500
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("УЧАСТНИК", PADDING, y);

        y += 20;

        // Имя участника
        g.setColor(new Color(17, 24, 39)); // Tailwind gray-900
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String fullName = registration.getFirstName() + " " + registration.getLastName();
        drawWrappedText(g, fullName, PADDING, y, maxTextWidth, 20);
    }

    /**
     * Рисует QR-код на билете.
     */
    private void drawQrCode(Graphics2D g, Registration registration) throws IOException {
        // Генерируем QR-код
        byte[] qrCodeBytes = qrCodeService.generateQrCode(registration.getConfirmationCode(), QR_SIZE);
        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrCodeBytes));

        // Позиция QR-кода (справа)
        int qrX = TICKET_WIDTH - QR_SIZE - PADDING;
        int qrY = 80;

        // Рисуем QR-код
        g.drawImage(qrImage, qrX, qrY, null);
    }

    /**
     * Рисует код подтверждения под QR-кодом.
     */
    private void drawConfirmationCode(Graphics2D g, Registration registration) {
        int qrX = TICKET_WIDTH - QR_SIZE - PADDING;
        int y = 80 + QR_SIZE + 20;

        // Код подтверждения (для ручного ввода)
        g.setColor(new Color(17, 24, 39)); // Tailwind gray-900
        g.setFont(new Font("Monospaced", Font.BOLD, 16));

        // Центрируем код под QR
        String code = registration.getConfirmationCode();
        FontMetrics fm = g.getFontMetrics();
        int codeWidth = fm.stringWidth(code);
        int codeX = qrX + (QR_SIZE - codeWidth) / 2;

        g.drawString(code, codeX, y);

        // Подсказка
        y += 18;
        g.setColor(new Color(107, 114, 128)); // Tailwind gray-500
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String hint = "Код для check-in";
        int hintWidth = g.getFontMetrics().stringWidth(hint);
        int hintX = qrX + (QR_SIZE - hintWidth) / 2;
        g.drawString(hint, hintX, y);
    }

    /**
     * Форматирует дату и время события.
     */
    private String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant.atZone(ZoneId.of("Europe/Moscow")));
    }

    /**
     * Рисует текст с переносом строк.
     */
    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                line = new StringBuilder(testLine);
            } else {
                if (!line.isEmpty()) {
                    g.drawString(line.toString(), x, y);
                    y += lineHeight;
                }
                line = new StringBuilder(word);
            }
        }

        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, y);
        }
    }

    /**
     * Вычисляет количество строк для текста.
     */
    private int calculateLines(FontMetrics fm, String text, int maxWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 1;

        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                line = new StringBuilder(testLine);
            } else {
                lines++;
                line = new StringBuilder(word);
            }
        }

        return lines;
    }
}
