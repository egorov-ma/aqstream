# Payment Service

Payment Service отвечает за платежи и интеграцию с платёжными провайдерами.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8083 |
| База данных | postgres-payment (dedicated) |
| Схема | payment_service |

## Ответственности

- Интеграция со Stripe и ЮKassa
- Создание и обработка платежей
- Возвраты (полные и частичные)
- Обработка webhooks
- Финансовая отчётность

## API Endpoints

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Создание платежа |
| GET | `/api/v1/payments/{id}` | Статус платежа |
| POST | `/api/v1/payments/{id}/refund` | Возврат |

### Webhooks

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/webhooks/stripe` | Stripe webhook |
| POST | `/api/v1/webhooks/yookassa` | ЮKassa webhook |

## Платёжные провайдеры

```java
public interface PaymentProvider {
    PaymentSession createSession(CreatePaymentRequest request);
    PaymentStatus getStatus(String providerPaymentId);
    RefundResult refund(String providerPaymentId, int amountCents);
}

@Service
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {
    // Stripe implementation
}

@Service
@ConditionalOnProperty(name = "payment.provider", havingValue = "yookassa")
public class YookassaPaymentProvider implements PaymentProvider {
    // ЮKassa implementation
}
```

## Создание платежа

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProvider paymentProvider;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentDto create(CreatePaymentRequest request, UUID idempotencyKey) {
        // Проверка идемпотентности
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return paymentMapper.toDto(existing.get());
        }
        
        // Создание сессии у провайдера
        PaymentSession session = paymentProvider.createSession(request);
        
        // Сохранение платежа
        Payment payment = new Payment();
        payment.setRegistrationId(request.registrationId());
        payment.setTenantId(request.tenantId());
        payment.setProvider(paymentProvider.getName());
        payment.setProviderPaymentId(session.getId());
        payment.setAmountCents(request.amountCents());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        
        Payment saved = paymentRepository.save(payment);
        
        return paymentMapper.toDto(saved, session.getCheckoutUrl());
    }
}
```

## Webhook Processing

```java
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final StripeWebhookHandler stripeHandler;
    private final YookassaWebhookHandler yookassaHandler;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripe(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        stripeHandler.handle(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/yookassa")
    public ResponseEntity<Void> handleYookassa(
        @RequestBody String payload
    ) {
        yookassaHandler.handle(payload);
        return ResponseEntity.ok().build();
    }
}
```

```java
@Component
@RequiredArgsConstructor
public class StripeWebhookHandler {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(String payload, String signature) {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);
        
        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            case "charge.refunded" -> handleRefunded(event);
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject().orElseThrow();
        
        Payment payment = paymentRepository
            .findByProviderPaymentId(intent.getId())
            .orElseThrow();
        
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(Instant.now());
        paymentRepository.save(payment);
        
        eventPublisher.publish(new PaymentCompletedEvent(
            payment.getId(),
            payment.getRegistrationId()
        ));
    }
}
```

## Возвраты

```java
@Transactional
public RefundDto refund(UUID paymentId, RefundRequest request) {
    Payment payment = findByIdOrThrow(paymentId);
    
    if (payment.getStatus() != PaymentStatus.COMPLETED) {
        throw new InvalidPaymentStateException("Можно вернуть только завершённый платёж");
    }
    
    int refundAmount = request.amountCents() != null 
        ? request.amountCents() 
        : payment.getAmountCents();
    
    // Проверка суммы
    int alreadyRefunded = refundRepository.sumByPaymentId(paymentId);
    if (alreadyRefunded + refundAmount > payment.getAmountCents()) {
        throw new RefundAmountExceededException();
    }
    
    // Возврат через провайдера
    RefundResult result = paymentProvider.refund(
        payment.getProviderPaymentId(), 
        refundAmount
    );
    
    // Сохранение
    Refund refund = new Refund();
    refund.setPayment(payment);
    refund.setAmountCents(refundAmount);
    refund.setReason(request.reason());
    refund.setProviderRefundId(result.getId());
    refund.setStatus(RefundStatus.COMPLETED);
    
    Refund saved = refundRepository.save(refund);
    
    // Обновляем статус платежа
    if (alreadyRefunded + refundAmount == payment.getAmountCents()) {
        payment.setStatus(PaymentStatus.REFUNDED);
    } else {
        payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
    }
    paymentRepository.save(payment);
    
    eventPublisher.publish(new PaymentRefundedEvent(
        payment.getId(),
        payment.getRegistrationId(),
        refundAmount
    ));
    
    return refundMapper.toDto(saved);
}
```

## События (RabbitMQ)

### Публикуемые

| Event | Описание |
|-------|----------|
| `payment.created` | Платёж создан |
| `payment.completed` | Платёж успешен |
| `payment.failed` | Платёж не удался |
| `payment.refunded` | Возврат выполнен |

### Потребляемые

| Event | Действие |
|-------|----------|
| `registration.created` | Создание платежа (для платных билетов) |
| `event.cancelled` | Массовый возврат |

## Конфигурация

```yaml
payment:
  provider: ${PAYMENT_PROVIDER:stripe}

stripe:
  api-key: ${STRIPE_API_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

yookassa:
  shop-id: ${YOOKASSA_SHOP_ID}
  secret-key: ${YOOKASSA_SECRET_KEY}
```

## Дальнейшее чтение

- [Domain Model](../../../data/domain-model.md)
- [Service Topology](../../../architecture/service-topology.md)
