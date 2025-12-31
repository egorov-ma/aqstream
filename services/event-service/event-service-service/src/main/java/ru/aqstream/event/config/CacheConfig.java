package ru.aqstream.event.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Конфигурация кэширования Redis для event-service.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Название кэша для названий организаций.
     */
    public static final String ORGANIZER_NAME_CACHE = "organizerNames";

    /**
     * TTL для кэша названий организаций — 15 минут.
     */
    private static final Duration ORGANIZER_NAME_TTL = Duration.ofMinutes(15);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        RedisCacheConfiguration organizerNameConfig = defaultConfig.entryTtl(ORGANIZER_NAME_TTL);

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration(ORGANIZER_NAME_CACHE, organizerNameConfig)
            .build();
    }
}
