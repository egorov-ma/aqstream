package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.aqstream.common.api.exception.ValidationException;

@DisplayName("QrCodeService")
class QrCodeServiceTest {

    private static final Faker FAKER = new Faker();
    private static final String CHECK_IN_BASE_URL = "https://aqstream.ru/check-in";

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService(CHECK_IN_BASE_URL);
    }

    @Nested
    @DisplayName("generateQrCode")
    class GenerateQrCode {

        @Test
        @DisplayName("генерирует валидный PNG для confirmation code")
        void generateQrCode_ValidCode_ReturnsPng() throws Exception {
            // Given
            String confirmationCode = generateConfirmationCode();

            // When
            byte[] qrCode = qrCodeService.generateQrCode(confirmationCode);

            // Then
            assertThat(qrCode).isNotEmpty();

            // Проверяем что это валидный PNG
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrCode));
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isEqualTo(QrCodeService.DEFAULT_QR_SIZE);
            assertThat(image.getHeight()).isEqualTo(QrCodeService.DEFAULT_QR_SIZE);
        }

        @Test
        @DisplayName("генерирует QR-код с указанным размером")
        void generateQrCode_CustomSize_ReturnsCorrectSize() throws Exception {
            // Given
            String confirmationCode = generateConfirmationCode();
            int customSize = 300;

            // When
            byte[] qrCode = qrCodeService.generateQrCode(confirmationCode, customSize);

            // Then
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrCode));
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isEqualTo(customSize);
            assertThat(image.getHeight()).isEqualTo(customSize);
        }

        @Test
        @DisplayName("выбрасывает исключение для null confirmation code")
        void generateQrCode_NullCode_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> qrCodeService.generateQrCode(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не может быть пустым");
        }

        @Test
        @DisplayName("выбрасывает исключение для пустого confirmation code")
        void generateQrCode_EmptyCode_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> qrCodeService.generateQrCode(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не может быть пустым");
        }

        @Test
        @DisplayName("выбрасывает исключение для пробельного confirmation code")
        void generateQrCode_BlankCode_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> qrCodeService.generateQrCode("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не может быть пустым");
        }

        @Test
        @DisplayName("выбрасывает исключение для размера меньше минимального")
        void generateQrCode_SizeTooSmall_ThrowsException() {
            // Given
            String confirmationCode = generateConfirmationCode();
            int tooSmallSize = QrCodeService.MIN_QR_SIZE - 1;

            // When & Then
            assertThatThrownBy(() -> qrCodeService.generateQrCode(confirmationCode, tooSmallSize))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(String.valueOf(QrCodeService.MIN_QR_SIZE));
        }

        @Test
        @DisplayName("генерирует минимальный размер QR-кода")
        void generateQrCode_MinSize_ReturnsCorrectSize() throws Exception {
            // Given
            String confirmationCode = generateConfirmationCode();

            // When
            byte[] qrCode = qrCodeService.generateQrCode(confirmationCode, QrCodeService.MIN_QR_SIZE);

            // Then
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrCode));
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isEqualTo(QrCodeService.MIN_QR_SIZE);
        }
    }

    @Nested
    @DisplayName("buildCheckInUrl")
    class BuildCheckInUrl {

        @Test
        @DisplayName("формирует корректный URL для check-in")
        void buildCheckInUrl_ValidCode_ReturnsCorrectUrl() {
            // Given
            String confirmationCode = "ABC12345";

            // When
            String url = qrCodeService.buildCheckInUrl(confirmationCode);

            // Then
            assertThat(url).isEqualTo(CHECK_IN_BASE_URL + "/" + confirmationCode);
        }

        @Test
        @DisplayName("формирует URL с разными кодами")
        void buildCheckInUrl_DifferentCodes_ReturnsDifferentUrls() {
            // Given
            String code1 = generateConfirmationCode();
            String code2 = generateConfirmationCode();

            // When
            String url1 = qrCodeService.buildCheckInUrl(code1);
            String url2 = qrCodeService.buildCheckInUrl(code2);

            // Then
            assertThat(url1).isNotEqualTo(url2);
            assertThat(url1).contains(code1);
            assertThat(url2).contains(code2);
        }
    }

    /**
     * Генерирует тестовый confirmation code (8 символов).
     */
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(FAKER.random().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
