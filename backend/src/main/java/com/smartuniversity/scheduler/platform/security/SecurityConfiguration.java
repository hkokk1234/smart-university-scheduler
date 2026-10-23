package com.smartuniversity.scheduler.platform.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.platform.api.ErrorResponseWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods =false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        var roles =new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        var converter =new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        var entryPoint =(org.springframework.security.web.AuthenticationEntryPoint) (request, response, error) ->
                ErrorResponseWriter.write(mapper, request, response, 401, "UNAUTHORIZED", "Authentication is required");
        var denied =(org.springframework.security.web.access.AccessDeniedHandler) (request, response, error) ->
                ErrorResponseWriter.write(mapper, request, response, 403, "FORBIDDEN", "Access is denied");
        return http
                // Tokens are accepted only via Authorization headers; no cookie or session authentication.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)
                                .withHttpMethod(HttpMethod.GET)).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/semesters", "/api/v1/courses", "/api/v1/courses/**", "/api/v1/sections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/schedules", "/api/v1/schedules/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/v1/copilot/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**").hasAnyRole("STUDENT", "ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .build();
    }
}
