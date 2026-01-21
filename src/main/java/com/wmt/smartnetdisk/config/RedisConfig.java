package com.wmt.smartnetdisk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * <p>
 * Spring Boot 4.x 版本配置
 * </p>
 *
 * @author wmt
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 配置
     * <p>
     * Key 使用 String 序列化，Value 使用 JSON 序列化
     * </p>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // JSON 序列化器（Spring Boot 4.x 推荐方式）
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        // Key 使用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
