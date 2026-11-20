package com.genie.common.security;

import com.genie.common.api.ErrorResponseFactory;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security cho API stateless (implement.md 4.1, TS 9, plan AUTH A04.04).
 * <ul>
 *   <li>Không session, không CSRF (Bearer token; refresh cookie {@code SameSite=Strict, Path=/api/v1/auth});</li>
 *   <li>Ma trận URL theo nhóm đường dẫn API_SPEC 0.1; chi tiết từng API {@code /admin/**} dùng {@code @PreAuthorize} mã quyền;</li>
 *   <li>Kế thừa vai trò (VENDOR ⊃ CUSTOMER, ADMIN ⊃ STAFF) do {@link JwtAuthFilter} cấp sẵn authority.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Endpoint công khai — không cần token. */
    private static final String[] PUBLIC = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/api/v1/payments/vnpay/ipn",
            "/api/v1/payments/vnpay/return",
            "/api/v1/payments/sepay/webhook",
            "/api/v1/shipping/*/webhook",
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/error",
    };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    JwtService jwtService,
                                    TokenRevocationStore revocations,
                                    List<AuthorityProvider> authorityProviders,
                                    SecurityProperties props,
                                    RestAuthEntryPoint entryPoint,
                                    RestAccessDeniedHandler accessDeniedHandler,
                                    ErrorResponseFactory errorResponses) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, revocations, authorityProviders);
        // Tài khoản nội bộ không mua hàng — /me/cart/**, /me/checkout/** (plan B1 D16, hướng B)
        BuyerOnlyFilter buyerOnlyFilter = new BuyerOnlyFilter(errorResponses);
        InternalTokenFilter internalFilter = new InternalTokenFilter(props);

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole("INTERNAL")
                        .requestMatchers("/api/v1/seller/**").hasRole("VENDOR")
                        .requestMatchers("/api/v1/admin/**").hasRole("STAFF")
                        .requestMatchers("/ws/**").permitAll()          // STOMP tự kiểm token ở CONNECT (plan NOTIFICATION / SOCIAL)
                        .anyRequest().authenticated())
                .addFilterBefore(internalFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(buyerOnlyFilter, JwtAuthFilter.class);
        return http.build();
    }

    /**
     * Argon2id theo OWASP 2024 (plan AUTH D10): salt 16 B, hash 32 B, p = 1, m = 19 MiB, t = 2.
     * Hash dạng {@code $argon2id$v=19$m=19456,t=2,p=1$…} — sinh tay bằng {@code scripts/hash-password.py}.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    private static CorsConfigurationSource corsConfigurationSource(SecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language",
                "Idempotency-Key", "X-OTP-Challenge-Id", "X-OTP-Code", "X-Request-Id"));
        cfg.setExposedHeaders(List.of("Retry-After", "X-Request-Id"));
        cfg.setAllowCredentials(true);          // cookie genie_rt cho /auth/sessions/refresh
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
