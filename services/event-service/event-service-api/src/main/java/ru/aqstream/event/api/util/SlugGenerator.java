package ru.aqstream.event.api.util;

import java.text.Normalizer;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Генератор URL-slug из текста.
 * Поддерживает транслитерацию кириллицы.
 */
public final class SlugGenerator {

    private static final int MAX_SLUG_LENGTH = 100;
    private static final int RANDOM_SUFFIX_LENGTH = 6;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    // Таблица транслитерации кириллицы
    private static final Map<Character, String> CYRILLIC_TO_LATIN = Map.ofEntries(
        Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
        Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
        Map.entry('ё', "yo"), Map.entry('ж', "zh"), Map.entry('з', "z"),
        Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
        Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
        Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
        Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('у', "u"),
        Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"),
        Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "shch"),
        Map.entry('ъ', ""), Map.entry('ы', "y"), Map.entry('ь', ""),
        Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya")
    );

    private SlugGenerator() {
        // Утилитарный класс
    }

    /**
     * Генерирует slug из текста.
     *
     * @param text исходный текст
     * @return slug
     */
    public static String generate(String text) {
        if (text == null || text.isBlank()) {
            return generateRandomSlug();
        }

        String slug = text.toLowerCase().trim();

        // Транслитерация кириллицы
        slug = transliterate(slug);

        // Нормализация Unicode (удаление акцентов)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        // Замена пробелов на дефисы
        slug = slug.replace(' ', '-');

        // Удаление всех символов кроме букв, цифр и дефисов
        slug = NON_ALPHANUMERIC.matcher(slug).replaceAll("");

        // Схлопывание множественных дефисов
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");

        // Удаление дефисов в начале и конце
        slug = slug.replaceAll("^-+|-+$", "");

        // Ограничение длины
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH);
            // Не обрезаем слово посередине
            int lastDash = slug.lastIndexOf('-');
            if (lastDash > MAX_SLUG_LENGTH - 20) {
                slug = slug.substring(0, lastDash);
            }
        }

        // Если после всех преобразований slug пустой
        if (slug.isEmpty()) {
            return generateRandomSlug();
        }

        return slug;
    }

    /**
     * Генерирует slug с гарантией уникальности через суффикс.
     *
     * @param text исходный текст
     * @return slug с random суффиксом
     */
    public static String generateWithSuffix(String text) {
        String base = generate(text);
        String suffix = generateRandomSuffix();

        // Укорачиваем base если нужно для суффикса
        int maxBaseLength = MAX_SLUG_LENGTH - RANDOM_SUFFIX_LENGTH - 1; // -1 для дефиса
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
            int lastDash = base.lastIndexOf('-');
            if (lastDash > maxBaseLength - 15) {
                base = base.substring(0, lastDash);
            }
        }

        return base + "-" + suffix;
    }

    /**
     * Транслитерирует кириллицу в латиницу.
     *
     * @param text текст на кириллице
     * @return транслитерированный текст
     */
    private static String transliterate(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = CYRILLIC_TO_LATIN.get(c);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Генерирует случайный slug.
     *
     * @return random slug
     */
    private static String generateRandomSlug() {
        return "event-" + generateRandomSuffix();
    }

    /**
     * Генерирует случайный суффикс из букв и цифр.
     *
     * @return random alphanumeric суффикс
     */
    private static String generateRandomSuffix() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            suffix.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return suffix.toString();
    }
}
