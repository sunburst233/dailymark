package com.dailymark.config;

import java.nio.file.Files;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StorageProperties properties;
    public WebConfig(StorageProperties properties) { this.properties = properties; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(properties.assetsPath().toUri().toString())
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response, Object handler) {
                if (request.getRequestURI().startsWith("/assets/") && request.getRequestURI().toLowerCase().endsWith(".svg")) {
                    response.setHeader("Content-Security-Policy", "script-src 'none'");
                }
                return true;
            }
        });
    }
}
