package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketImageService")
class TicketImageServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private QrCodeService qrCodeService;

    private TicketImageService ticketImageService;

    private UUID tenantId;
    private UUID eventId;
    private UUID ticketTypeId;
    private UUID registrationId;
    private String eventTitle;
    private String ticketTypeName;
    private String firstName;
    private String lastName;
    private String confirmationCode;

    @BeforeEach
    void setUp() {
        ticketImageService = new TicketImageService(qrCodeService);

        // Генерируем тестовые данные
        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        registrationId = UUID.randomUUID();
        eventTitle = FAKER.book().title();
        ticketTypeName = FAKER.commerce().productName();
        firstName = FAKER.name().firstName();
        lastName = FAKER.name().lastName();
        confirmationCode = generateConfirmationCode();
    }

    @Nested
    @DisplayName("generateTicketImage")
    class GenerateTicketImage {

        @Test
        @DisplayName("генерирует валидный PNG для регистрации")
        void generateTicketImage_ValidRegistration_ReturnsPng() throws Exception {
            // Given
            Registration registration = createRegistration();
            byte[] mockQrCode = createMockQrCode();
            when(qrCodeService.generateQrCode(anyString(), anyInt())).thenReturn(mockQrCode);

            // When
            byte[] ticketImage = ticketImageService.generateTicketImage(registration);

            // Then
            assertThat(ticketImage).isNotEmpty();

            // Проверяем что это валидный PNG
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(ticketImage));
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isEqualTo(TicketImageService.TICKET_WIDTH);
            assertThat(image.getHeight()).isEqualTo(TicketImageService.TICKET_HEIGHT);
        }

        @Test
        @DisplayName("генерирует билет с правильными размерами")
        void generateTicketImage_ValidRegistration_ReturnsCorrectSize() throws Exception {
            // Given
            Registration registration = createRegistration();
            byte[] mockQrCode = createMockQrCode();
            when(qrCodeService.generateQrCode(anyString(), anyInt())).thenReturn(mockQrCode);

            // When
            byte[] ticketImage = ticketImageService.generateTicketImage(registration);

            // Then
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(ticketImage));
            assertThat(image.getWidth()).isEqualTo(600);
            assertThat(image.getHeight()).isEqualTo(400);
        }

        @Test
        @DisplayName("генерирует билет для события без адреса")
        void generateTicketImage_EventWithoutLocation_ReturnsPng() throws Exception {
            // Given
            Registration registration = createRegistrationWithoutLocation();
            byte[] mockQrCode = createMockQrCode();
            when(qrCodeService.generateQrCode(anyString(), anyInt())).thenReturn(mockQrCode);

            // When
            byte[] ticketImage = ticketImageService.generateTicketImage(registration);

            // Then
            assertThat(ticketImage).isNotEmpty();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(ticketImage));
            assertThat(image).isNotNull();
        }

        @Test
        @DisplayName("генерирует билет для длинного названия события")
        void generateTicketImage_LongEventTitle_ReturnsPng() throws Exception {
            // Given
            Registration registration = createRegistrationWithLongTitle();
            byte[] mockQrCode = createMockQrCode();
            when(qrCodeService.generateQrCode(anyString(), anyInt())).thenReturn(mockQrCode);

            // When
            byte[] ticketImage = ticketImageService.generateTicketImage(registration);

            // Then
            assertThat(ticketImage).isNotEmpty();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(ticketImage));
            assertThat(image).isNotNull();
        }
    }

    // === Вспомогательные методы ===

    private Registration createRegistration() {
        Event event = createEvent();
        event.setLocationAddress(FAKER.address().fullAddress());
        TicketType ticketType = createTicketType(event);
        Registration registration = Registration.create(
            event, ticketType, UUID.randomUUID(), confirmationCode, firstName, lastName,
            FAKER.internet().emailAddress()
        );
        setPrivateField(registration, "id", registrationId);
        setPrivateField(registration, "tenantId", tenantId);
        return registration;
    }

    private Registration createRegistrationWithoutLocation() {
        Event event = createEvent();
        // Не устанавливаем locationAddress
        TicketType ticketType = createTicketType(event);
        Registration registration = Registration.create(
            event, ticketType, UUID.randomUUID(), confirmationCode, firstName, lastName,
            FAKER.internet().emailAddress()
        );
        setPrivateField(registration, "id", registrationId);
        setPrivateField(registration, "tenantId", tenantId);
        return registration;
    }

    private Registration createRegistrationWithLongTitle() {
        Event event = Event.create(
            "Очень длинное название события которое должно переноситься "
                + "на несколько строк и проверяет работу переноса текста",
            FAKER.internet().slug(),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        setPrivateField(event, "id", eventId);
        setPrivateField(event, "tenantId", tenantId);
        event.setLocationAddress(FAKER.address().fullAddress());

        TicketType ticketType = createTicketType(event);
        Registration registration = Registration.create(
            event, ticketType, UUID.randomUUID(), confirmationCode, firstName, lastName,
            FAKER.internet().emailAddress()
        );
        setPrivateField(registration, "id", registrationId);
        setPrivateField(registration, "tenantId", tenantId);
        return registration;
    }

    private Event createEvent() {
        Event event = Event.create(
            eventTitle,
            FAKER.internet().slug(),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        setPrivateField(event, "id", eventId);
        setPrivateField(event, "tenantId", tenantId);
        return event;
    }

    private TicketType createTicketType(Event event) {
        TicketType ticketType = TicketType.create(event, ticketTypeName);
        ticketType.setQuantity(100);
        setPrivateField(ticketType, "id", ticketTypeId);
        return ticketType;
    }

    /**
     * Создаёт мок QR-кода (простое PNG изображение).
     */
    private byte[] createMockQrCode() throws Exception {
        BufferedImage image = new BufferedImage(
            TicketImageService.QR_SIZE,
            TicketImageService.QR_SIZE,
            BufferedImage.TYPE_INT_RGB
        );
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(FAKER.random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Устанавливает значение private поля через reflection.
     */
    @SuppressWarnings("SameParameterValue")
    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(object.getClass(), fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось установить поле " + fieldName, e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Поле " + fieldName + " не найдено в иерархии классов");
    }
}
