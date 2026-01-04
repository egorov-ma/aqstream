package ru.aqstream.common.test.allure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Allure;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified логгер для тестов: пишет одновременно в Allure attachments и SLF4J.
 *
 * <p>Использование:</p>
 * <pre>
 * TestLogger.info("Событие создано: eventId={}", eventId);
 * TestLogger.debug("Токены обновлены: userId={}", userId);
 * TestLogger.error("Ошибка оплаты: registrationId={}", regId, exception);
 * TestLogger.attachJson("Request", requestObject);
 * TestLogger.attachText("SQL Query", query);
 * </pre>
 */
public final class TestLogger {

    private static final Logger LOG = LoggerFactory.getLogger(TestLogger.class);
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /**
     * INFO уровень - важные бизнес-события.
     *
     * @param message сообщение (с {} placeholders для SLF4J)
     * @param args аргументы для подстановки
     */
    public static void info(String message, Object... args) {
        String formatted = formatMessage(message, args);
        LOG.info(formatted);
        Allure.addAttachment("INFO: " + formatted, "text/plain", formatted, ".txt");
    }

    /**
     * DEBUG уровень - технические детали.
     *
     * @param message сообщение (с {} placeholders)
     * @param args аргументы для подстановки
     */
    public static void debug(String message, Object... args) {
        String formatted = formatMessage(message, args);
        LOG.debug(formatted);
        Allure.addAttachment("DEBUG: " + formatted, "text/plain", formatted, ".txt");
    }

    /**
     * WARN уровень - предупреждения.
     *
     * @param message сообщение (с {} placeholders)
     * @param args аргументы для подстановки
     */
    public static void warn(String message, Object... args) {
        String formatted = formatMessage(message, args);
        LOG.warn(formatted);
        Allure.addAttachment("WARN: " + formatted, "text/plain", formatted, ".txt");
    }

    /**
     * ERROR уровень - ошибки без исключения.
     *
     * @param message сообщение (с {} placeholders)
     * @param args аргументы для подстановки
     */
    public static void error(String message, Object... args) {
        String formatted = formatMessage(message, args);
        LOG.error(formatted);
        Allure.addAttachment("ERROR: " + formatted, "text/plain", formatted, ".txt");
    }

    /**
     * ERROR уровень - ошибки с исключением.
     *
     * @param message сообщение (с {} placeholders)
     * @param throwable исключение
     * @param args аргументы для подстановки (НЕ включают throwable)
     */
    public static void error(String message, Throwable throwable, Object... args) {
        String formatted = formatMessage(message, args);
        LOG.error(formatted, throwable);

        String errorDetails = formatted + "\n\nStack trace:\n" + getStackTrace(throwable);
        Allure.addAttachment("ERROR: " + formatted, "text/plain", errorDetails, ".txt");
    }

    /**
     * Прикрепить JSON объект к отчёту.
     *
     * @param name название attachment
     * @param object объект для сериализации
     */
    public static void attachJson(String name, Object object) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(object);
            Allure.addAttachment(name, "application/json", json, ".json");
            LOG.debug("Attached JSON: {}", name);
        } catch (JsonProcessingException e) {
            LOG.warn("Не удалось сериализовать JSON для attachment: {}", name, e);
        }
    }

    /**
     * Прикрепить текст к отчёту.
     *
     * @param name название attachment
     * @param content текстовое содержимое
     */
    public static void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", content, ".txt");
        LOG.debug("Attached text: {} (length={})", name, content != null ? content.length() : 0);
    }

    /**
     * Прикрепить SQL запрос к отчёту.
     *
     * @param name название attachment
     * @param sqlQuery SQL запрос
     */
    public static void attachSql(String name, String sqlQuery) {
        Allure.addAttachment(name, "text/sql", sqlQuery, ".sql");
        LOG.debug("Attached SQL: {}", name);
    }

    /**
     * Прикрепить HTML к отчёту.
     *
     * @param name название attachment
     * @param htmlContent HTML содержимое
     */
    public static void attachHtml(String name, String htmlContent) {
        Allure.addAttachment(name, "text/html", htmlContent, ".html");
        LOG.debug("Attached HTML: {}", name);
    }

    /**
     * Прикрепить XML к отчёту.
     *
     * @param name название attachment
     * @param xmlContent XML содержимое
     */
    public static void attachXml(String name, String xmlContent) {
        Allure.addAttachment(name, "application/xml", xmlContent, ".xml");
        LOG.debug("Attached XML: {}", name);
    }

    // Вспомогательные методы

    /**
     * Форматирует сообщение с подстановкой аргументов (аналогично SLF4J).
     *
     * @param message сообщение с {} placeholders
     * @param args аргументы для подстановки
     * @return отформатированное сообщение
     */
    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        String result = message;
        for (Object arg : args) {
            // Пропускаем Throwable - они обрабатываются отдельно
            if (arg instanceof Throwable) {
                continue;
            }
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }

    /**
     * Получает stack trace исключения как строку.
     *
     * @param throwable исключение
     * @return stack trace
     */
    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private TestLogger() {
        // Утилитный класс
    }
}
