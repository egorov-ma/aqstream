#!/bin/bash
# Скачивает OpenAPI спецификации из работающих сервисов

set -e

SERVICES=(
    "gateway:8080"
    "user-service:8081"
    "event-service:8082"
    "payment-service:8083"
    "notification-service:8084"
    "media-service:8085"
    "analytics-service:8086"
)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/../../tech-stack/backend/api/specs"
mkdir -p "$OUTPUT_DIR"

echo "Скачивание OpenAPI спецификаций..."
echo ""

SUCCESS=0
FAILED=0

for service in "${SERVICES[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"

    printf "  → %-25s" "$name"

    if curl -s --fail --max-time 5 "http://localhost:$port/v3/api-docs.yaml" > "$OUTPUT_DIR/$name.yaml" 2>/dev/null; then
        echo "✓ OK"
        SUCCESS=$((SUCCESS + 1))
    else
        echo "✗ недоступен"
        rm -f "$OUTPUT_DIR/$name.yaml"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "Результат: $SUCCESS успешно, $FAILED недоступно"
echo "Спецификации сохранены в: $OUTPUT_DIR"
