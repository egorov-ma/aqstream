package ru.aqstream.event.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.aqstream.event.api.exception.QrCodeGenerationException;

/**
 * Сервис генерации QR-кодов для билетов.
 *
 * <p>Использует библиотеку ZXing для генерации QR-кодов с высоким уровнем
 * коррекции ошибок (H level — до 30% восстановление данных).</p>
 */
@Service
@Slf4j
public class QrCodeService {

    /**
     * Размер QR-кода по умолчанию (в пикселях).
     */
    public static final int DEFAULT_QR_SIZE = 250;

    /**
     * Минимальный размер QR-кода (в пикселях).
     */
    public static final int MIN_QR_SIZE = 200;

    /**
     * Базовый URL для check-in.
     */
    private final String checkInBaseUrl;

    public QrCodeService(@Value("${app.check-in.base-url:https://aqstream.ru/check-in}") String checkInBaseUrl) {
        this.checkInBaseUrl = checkInBaseUrl;
    }

    /**
     * Генерирует QR-код для confirmation code с размером по умолчанию.
     *
     * @param confirmationCode код подтверждения регистрации
     * @return PNG изображение QR-кода в виде массива байтов
     */
    public byte[] generateQrCode(String confirmationCode) {
        return generateQrCode(confirmationCode, DEFAULT_QR_SIZE);
    }

    /**
     * Генерирует QR-код для confirmation code с указанным размером.
     *
     * @param confirmationCode код подтверждения регистрации
     * @param size             размер QR-кода в пикселях (минимум 200)
     * @return PNG изображение QR-кода в виде массива байтов
     */
    public byte[] generateQrCode(String confirmationCode, int size) {
        if (confirmationCode == null || confirmationCode.isBlank()) {
            throw new IllegalArgumentException("Confirmation code не может быть пустым");
        }
        if (size < MIN_QR_SIZE) {
            throw new IllegalArgumentException("Размер QR-кода должен быть не менее " + MIN_QR_SIZE + " пикселей");
        }

        // Формируем URL для check-in
        String content = buildCheckInUrl(confirmationCode);

        log.debug("Генерация QR-кода: confirmationCode={}, size={}x{}", confirmationCode, size, size);

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                Map.of(
                    // Высокий уровень коррекции ошибок — до 30% восстановление
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    // Поля вокруг QR-кода
                    EncodeHintType.MARGIN, 1,
                    // Кодировка UTF-8
                    EncodeHintType.CHARACTER_SET, "UTF-8"
                )
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            log.debug("QR-код сгенерирован: confirmationCode={}, bytes={}", confirmationCode, outputStream.size());

            return outputStream.toByteArray();
        } catch (WriterException e) {
            log.error("Ошибка генерации QR-кода: confirmationCode={}, ошибка={}", confirmationCode, e.getMessage(), e);
            throw new QrCodeGenerationException(confirmationCode, e);
        } catch (IOException e) {
            log.error("Ошибка записи QR-кода в поток: confirmationCode={}, ошибка={}", confirmationCode, e.getMessage(), e);
            throw new QrCodeGenerationException(e);
        }
    }

    /**
     * Формирует URL для check-in на основе confirmation code.
     *
     * @param confirmationCode код подтверждения
     * @return полный URL для сканирования
     */
    public String buildCheckInUrl(String confirmationCode) {
        return checkInBaseUrl + "/" + confirmationCode;
    }
}
