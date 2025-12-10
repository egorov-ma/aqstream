#!/bin/bash
# Валидация Markdown файлов

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="$SCRIPT_DIR/../.."

echo "Валидация Markdown..."
echo ""

# Проверяем наличие npx
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден. Установите Node.js"
    exit 1
fi

# Запускаем markdownlint
if npx markdownlint-cli2 "$DOCS_DIR/**/*.md" \
    --ignore "$DOCS_DIR/_internal/**" \
    --ignore "$DOCS_DIR/**/node_modules/**" \
    --ignore "$DOCS_DIR/to-dos/**"; then
    echo ""
    echo "✓ Markdown валидация пройдена"
else
    echo ""
    echo "✗ Найдены ошибки в Markdown файлах"
    exit 1
fi
