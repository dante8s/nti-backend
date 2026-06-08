package com.nti.nti_backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import static com.nti.nti_backend.config.CacheNames.*;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // FIX: Use NON_FINAL for type info, but apply it strictly.
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(Map.ofEntries(
                        // Public — long-lived
                        Map.entry(ORGANIZATIONS_PUBLIC,  base.entryTtl(Duration.ofHours(1))),
                                Map.entry(PROGRAMS_PUBLIC,       base.entryTtl(Duration.ofHours(1))),
                                Map.entry(MENTORS_PUBLIC,        base.entryTtl(Duration.ofHours(1))),
                                Map.entry(CALLS_OPEN,            base.entryTtl(Duration.ofHours(1))),

                        // Org
                                Map.entry(ORGANIZATIONS,         base.entryTtl(Duration.ofMinutes(30))),
                                Map.entry(ORGANIZATION,          base.entryTtl(Duration.ofMinutes(30))),
                                Map.entry(ORG_MEMBERS,           base.entryTtl(Duration.ofMinutes(15))),

                        // Mentorship
                                Map.entry(MENTORSHIPS_MY,        base.entryTtl(Duration.ofMinutes(10))),
                                Map.entry(MENTORSHIP,            base.entryTtl(Duration.ofMinutes(20))),

                        // Milestone
                                Map.entry(MILESTONE,             base.entryTtl(Duration.ofMinutes(20))),
                                Map.entry(MILESTONES_PENDING,    base.entryTtl(Duration.ofMinutes(5))),

                        // Application
                                Map.entry(APPLICATIONS_MY,       base.entryTtl(Duration.ofMinutes(10))),
                                Map.entry(APPLICATIONS_ALL,      base.entryTtl(Duration.ofMinutes(5))),
                                Map.entry(APPLICATIONS_CALL,     base.entryTtl(Duration.ofMinutes(15))),

                        // Call
                                Map.entry(CALL,                  base.entryTtl(Duration.ofMinutes(30))),
                                Map.entry(CALLS_BY_PROGRAM,      base.entryTtl(Duration.ofMinutes(30))),

                        // Criteria & Evaluation — short TTL, changes during active evaluation
                                Map.entry(CRITERIA,              base.entryTtl(Duration.ofMinutes(30))),
                                Map.entry(EVALUATIONS,           base.entryTtl(Duration.ofMinutes(5))),
                                Map.entry(SCORE_WEIGHTED,        base.entryTtl(Duration.ofMinutes(5))),
                                Map.entry(SCORE_AVERAGE,         base.entryTtl(Duration.ofMinutes(5))),

                        // Student Profile
                                Map.entry(STUDENT_PROFILE,       base.entryTtl(Duration.ofMinutes(20))),

                        // Team
                                Map.entry(TEAM,                  base.entryTtl(Duration.ofMinutes(15))),
                                Map.entry(TEAM_FOR_USER,         base.entryTtl(Duration.ofMinutes(10))),
                        Map.entry(TEAM_INVITES,          base.entryTtl(Duration.ofMinutes(5))),
                        Map.entry(ELIGIBLE_TEAMS,        base.entryTtl(Duration.ofMinutes(10)))
                ))
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(15)))
                .build();
    }
}