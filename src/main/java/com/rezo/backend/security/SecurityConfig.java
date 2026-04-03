package com.rezo.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers("/", "/ping").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/schools/**", "/api/companies/**", "/api/offers/**", "/api/packs/**").permitAll()

                        // Role-based endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/entreprise/**").hasRole("ENTREPRISE")
                        .requestMatchers("/api/ecole/**").hasRole("ECOLE")
                        .requestMatchers(HttpMethod.POST, "/api/companies/**").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.PUT, "/api/companies/**").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.DELETE, "/api/companies/**").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.POST, "/api/schools/**").hasRole("ECOLE")
                        .requestMatchers(HttpMethod.PUT, "/api/schools/**").hasRole("ECOLE")
                        .requestMatchers(HttpMethod.DELETE, "/api/schools/**").hasRole("ECOLE")
                        .requestMatchers(HttpMethod.POST, "/api/offers/**").hasAnyRole("ENTREPRISE", "ECOLE")
                        .requestMatchers(HttpMethod.PUT, "/api/offers/**").hasAnyRole("ENTREPRISE", "ECOLE")
                        .requestMatchers(HttpMethod.DELETE, "/api/offers/**").hasAnyRole("ENTREPRISE", "ECOLE")
                        .requestMatchers(HttpMethod.POST, "/api/packs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/packs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/packs/**").hasRole("ADMIN")

                        // Dev/test: delete all users (authenticated only)
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/users").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/users/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Non authentifie: token JWT manquant ou invalide\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Acces refuse: role insuffisant\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
