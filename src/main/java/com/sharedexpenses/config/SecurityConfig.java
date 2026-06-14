package com.sharedexpenses.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sharedexpenses.security.JwtAuthenticationEntryPoint;
import com.sharedexpenses.security.JwtAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS — must be enabled so Spring Security delegates to our CorsConfigurationSource bean.
            //    Without this, the browser's OPTIONS preflight is rejected before reaching any endpoint.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. CSRF — disabled for a stateless JWT API (no cookies, no sessions).
            .csrf(AbstractHttpConfigurer::disable)

            // 3. Sessions — never created or used; every request carries its own JWT.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Return 401 (not a redirect to /login) for unauthenticated requests.
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

            // 5. Route authorisation rules.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .anyRequest().authenticated())

            // 6. Validate the JWT on every request before Spring's own auth filter runs.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS policy bean.
     *
     * During development the Vite dev-server runs on http://localhost:5173 and proxies
     * /api calls to the Spring Boot backend on http://localhost:8080.  The proxy strips
     * the Origin header, so no CORS headers are needed for that case.  However, if the
     * frontend is ever served from a different origin (e.g. a deployed domain, or a
     * mobile app), this bean ensures the correct CORS headers are returned.
     *
     * Allowed origins, methods, and headers can be tightened for production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the Vite dev server and any localhost port during development.
        // Replace / extend these with your actual production origins before deploying.
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));

        // Methods the browser is allowed to use.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers the browser is allowed to send — Authorization carries the JWT.
        config.setAllowedHeaders(List.of("*"));

        // Allow the browser to read the Authorization header in responses.
        config.setExposedHeaders(List.of("Authorization"));

        // Cache the preflight response for 1 hour to reduce OPTIONS round-trips.
        config.setMaxAge(3600L);

        // Required when the frontend sends credentials (Authorization header).
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
