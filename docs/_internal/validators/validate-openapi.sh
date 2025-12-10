#!/bin/bash
# Валидация OpenAPI спецификаций с помощью Spectral

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SPECS_DIR="$SCRIPT_DIR/../../tech-stack/backend/api/specs"
RULES_FILE="$SCRIPT_DIR/spectral.yaml"

echo "Валидация OpenAPI спецификаций..."
echo ""

# Проверяем наличие спецификаций
if [ ! -d "$SPECS_DIR" ] || [ -z "$(ls -A "$SPECS_DIR"/*.yaml 2>/dev/null)" ]; then
    echo "⚠ Нет спецификаций для валидации"
    exit 0
fi

# Проверяем наличие npx
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден. Установите Node.js"
    exit 1
fi

ERRORS=0

for spec in "$SPECS_DIR"/*.yaml; do
    if [ -f "$spec" ]; then
        name=$(basename "$spec")
        printf "  → %-25s" "$name"

        if npx @stoplight/spectral-cli lint "$spec" --ruleset "$RULES_FILE" 2>/dev/null; then
            echo "✓ OK"
        else
            echo "✗ ошибки"
            ERRORS=$((ERRORS + 1))
        fi
    fi
done

echo ""

if [ $ERRORS -gt 0 ]; then
    echo "✗ Найдены ошибки в $ERRORS файлах"
    exit 1
fi

echo "✓ OpenAPI валидация пройдена"
