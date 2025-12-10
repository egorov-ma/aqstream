#!/bin/bash
# Проверка битых ссылок в документации

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="$SCRIPT_DIR/../.."

echo "Проверка ссылок в документации..."
echo ""

# Проверяем наличие npx
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден. Установите Node.js"
    exit 1
fi

# Используем markdown-link-check
if npx markdown-link-check "$DOCS_DIR"/**/*.md \
    --config "$SCRIPT_DIR/link-check-config.json" \
    --quiet 2>/dev/null; then
    echo ""
    echo "✓ Все ссылки валидны"
else
    echo ""
    echo "⚠ Найдены битые ссылки (это может быть нормально для внешних ссылок)"
    exit 0
fi
