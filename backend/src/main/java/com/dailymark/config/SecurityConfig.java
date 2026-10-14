package com.dailymark.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StorageProperties properties) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/**").permitAll()
                    .requestMatchers("/assets/**", "/actuator/health", "/", "/index.html", "/*.js", "/*.css", "/*.ico").permitAll()
                    .anyRequest().permitAll())
            .addFilterBefore(new ApiKeyFilter(properties.getApiKey()), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
