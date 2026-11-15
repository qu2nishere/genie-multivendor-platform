package com.genie.auth.oauth.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.oauth.application.OAuthState;
import com.genie.auth.oauth.application.OAuthStateStore;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** {@code oauth:state:{state}} = JSON {@link OAuthState}, TTL 10 phút; đọc bằng GETDEL (dùng một lần, nguyên tử). */
@Component
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final String KEY = "oauth:state:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisOAuthStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String state, OAuthState value, Duration ttl) {
        try {
            redis.opsForValue().set(KEY + state, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public Optional<OAuthState> take(String state) {
        if (state == null || state.isBlank() || state.length() > 200) {
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().getAndDelete(KEY + state);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, OAuthState.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
