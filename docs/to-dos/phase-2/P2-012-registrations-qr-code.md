# P2-012 QR-код для билета

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Регистрации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

После регистрации участник получает билет с QR-кодом в Telegram. QR-код содержит confirmation_code, который сканируется на входе для check-in. Это основной способ идентификации участника.

### Технический контекст

- QR-код генерируется на backend при создании регистрации
- Содержит confirmation_code или URL для проверки
- Отправляется через Notification Service в Telegram
- Check-in сканирует QR и вызывает API для проверки

**Связанные документы:**
- [Functional Requirements FR-6.1.5](../../business/functional-requirements.md#fr-61-процесс-регистрации) — «Билет отправляется в Telegram с QR-кодом»
- [Notification Service](../../tech-stack/backend/services/notification-service.md)

## Цель

Реализовать генерацию QR-кода для билета и интеграцию с check-in процессом.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Генерация QR

- [ ] QR-код генерируется при создании регистрации
- [ ] QR содержит: confirmation_code или check-in URL
- [ ] QR-код сохраняется как изображение (PNG)
- [ ] Размер QR достаточен для сканирования (минимум 200x200 px)
- [ ] QR имеет высокий уровень коррекции ошибок (H level)

### Билет

- [ ] Билет — изображение с информацией о событии + QR-код
- [ ] Содержит: название события, дата/время, место, тип билета, confirmation_code, QR
- [ ] Дизайн простой и читаемый
- [ ] Отправляется в Telegram как изображение

### Check-in

- [ ] QR сканируется через камеру (мобильный браузер)
- [ ] При сканировании — переход на URL проверки или вызов API
- [ ] Отображается информация об участнике
- [ ] Кнопка подтверждения check-in
- [ ] Защита от повторного check-in

### Fallback

- [ ] Confirmation code виден на билете (для ручного ввода)
- [ ] Поиск по коду в интерфейсе check-in

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Код написан согласно code style проекта
- [ ] Unit тесты для генерации QR
- [ ] Integration тесты check-in flow
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `event-service-service` (QR generation), `notification-service` (ticket image)
- [ ] Frontend: страница check-in, сканирование QR
- [ ] Database: —
- [ ] Infrastructure: —

### QR Generation

```java
// Используем ZXing library
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

@Service
public class QrCodeService {

    public byte[] generateQrCode(String content, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height,
            Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
```

### QR Content Format

Вариант 1 — только код:
```
AQ-XYZW1234
```

Вариант 2 — URL для сканирования:
```
https://aqstream.ru/check-in/XYZW1234
```

**Рекомендация:** URL, так как позволяет открыть страницу проверки сразу.

### Ticket Image Generation

```java
@Service
public class TicketImageService {

    public byte[] generateTicketImage(Registration reg, Event event) {
        // 1. Создать изображение (600x400 px)
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 2. Заполнить фон
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 400);

        // 3. Добавить текст
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString(event.getTitle(), 20, 40);

        // 4. Добавить QR-код
        byte[] qrCode = qrCodeService.generateQrCode(
            "https://aqstream.ru/check-in/" + reg.getConfirmationCode(),
            200, 200
        );
        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrCode));
        g.drawImage(qrImage, 380, 180, null);

        // 5. Сохранить
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }
}
```

### Check-in API

```
GET  /api/v1/check-in/{confirmationCode}  — информация о регистрации
POST /api/v1/check-in/{confirmationCode}  — выполнить check-in
```

### Dependencies

```gradle
// ZXing для QR
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.google.zxing:javase:3.5.2'
```

## Зависимости

### Блокирует

- [P2-014](./P2-014-notifications-templates.md) Отправка билета в Telegram

### Зависит от

- [P2-011](./P2-011-registrations-crud.md) Регистрации

## Out of Scope

- Кастомизация дизайна билета организатором
- Apple Wallet / Google Wallet интеграция
- NFC

## Заметки

- QR с URL предпочтительнее, так как работает на любом устройстве
- Рассмотреть добавление логотипа организации на билет
- Для мобильного check-in использовать Web API для камеры (MediaDevices.getUserMedia)
- Тестирование QR на разных устройствах критично
