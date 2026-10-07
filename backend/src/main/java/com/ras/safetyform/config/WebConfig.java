package com.ras.safetyform.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final ActiveUserInterceptor activeUserInterceptor;
    private final CsrfProtectionInterceptor csrfProtectionInterceptor;
    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(
            ActiveUserInterceptor activeUserInterceptor,
            CsrfProtectionInterceptor csrfProtectionInterceptor,
            PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.activeUserInterceptor = activeUserInterceptor;
        this.csrfProtectionInterceptor = csrfProtectionInterceptor;
        this.passwordChangeRequiredInterceptor = passwordChangeRequiredInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::strip)
                .filter((origin) -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    /** All REST controllers are served under /api so the SPA owns every other path. */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                ApiPaths.PREFIX,
                HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] protectedPaths = {
            ApiPaths.PREFIX + "/auth/**",
            ApiPaths.PREFIX + "/users/**",
            ApiPaths.PREFIX + "/sites/**",
            ApiPaths.PREFIX + "/safety-forms/**"
        };
        registry.addInterceptor(activeUserInterceptor).addPathPatterns(protectedPaths);
        registry.addInterceptor(csrfProtectionInterceptor).addPathPatterns(protectedPaths);
        registry.addInterceptor(passwordChangeRequiredInterceptor).addPathPatterns(protectedPaths);
    }

    /** Cross-origin access is opt-in; with the SPA served by this app no origins are needed. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping(ApiPaths.PREFIX + "/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", SessionUser.CSRF_HEADER)
                .exposedHeaders(SessionUser.CSRF_HEADER)
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** Serves the bundled frontend build and falls back to index.html for client-side routes. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    static class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = resourcePath.isEmpty() ? null : location.createRelative(resourcePath);
            if (requested != null && requested.exists() && requested.isReadable()) {
                return requested;
            }
            boolean isApiPath = resourcePath.equals("api") || resourcePath.startsWith("api/");
            boolean looksLikeFile = resourcePath.contains(".");
            if (isApiPath || looksLikeFile) {
                return null;
            }
            Resource index = new ClassPathResource("static/index.html");
            return index.exists() ? index : null;
        }
    }
}
