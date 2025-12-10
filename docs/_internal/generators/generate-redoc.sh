#!/bin/bash
# Генерирует ReDoc HTML из OpenAPI спецификаций

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SPECS_DIR="$SCRIPT_DIR/../../tech-stack/backend/api/specs"
OUTPUT_DIR="$SCRIPT_DIR/../../tech-stack/backend/api/redoc"
mkdir -p "$OUTPUT_DIR"

echo "Генерация ReDoc HTML..."
echo ""

# Проверяем наличие npx
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден. Установите Node.js"
    exit 1
fi

# Проверяем наличие спецификаций
if [ ! -d "$SPECS_DIR" ] || [ -z "$(ls -A "$SPECS_DIR"/*.yaml 2>/dev/null)" ]; then
    echo "⚠ Нет спецификаций для генерации"
    echo "Сначала запустите: ./generate-openapi.sh"
    exit 0
fi

SUCCESS=0
FAILED=0

for spec in "$SPECS_DIR"/*.yaml; do
    if [ -f "$spec" ]; then
        name=$(basename "$spec" .yaml)
        printf "  → %-25s" "$name"

        if npx @redocly/cli build-docs "$spec" \
            --output "$OUTPUT_DIR/$name.html" \
            --title "AqStream $name API" \
            2>/dev/null; then
            echo "✓ OK"
            SUCCESS=$((SUCCESS + 1))
        else
            echo "✗ ошибка"
            FAILED=$((FAILED + 1))
        fi
    fi
done

echo ""
echo "Результат: $SUCCESS успешно, $FAILED с ошибками"
echo "HTML файлы сохранены в: $OUTPUT_DIR"
