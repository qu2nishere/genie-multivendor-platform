package com.genie.auth.oauth.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.oauth.application.PendingOAuthSignup;
import com.genie.auth.oauth.application.PendingOAuthSignupStore;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** {@code oauth:signup:{token}} = JSON {@link PendingOAuthSignup}, TTL 15 phút (plan D23). */
@Component
public class RedisPendingOAuthSignupStore implements PendingOAuthSignupStore {

    private static final String KEY = "oauth:signup:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisPendingOAuthSignupStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String token, PendingOAuthSignup value, Duration ttl) {
        try {
            redis.opsForValue().set(KEY + token, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public Optional<PendingOAuthSignup> find(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(KEY + token);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, PendingOAuthSignup.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void delete(String token) {
        try {
            redis.delete(KEY + token);
        } catch (DataAccessException ignored) {
            // TTL tự dọn
        }
    }
}
