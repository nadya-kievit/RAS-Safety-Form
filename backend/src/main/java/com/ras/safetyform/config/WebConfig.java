package com.ras.safetyform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;

    public WebConfig(PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor) {
        this.passwordChangeRequiredInterceptor = passwordChangeRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordChangeRequiredInterceptor)
                .addPathPatterns(
                        "/auth/**",
                        "/users/**",
                        "/sites/**",
                        "/safety-forms/**");
    }
}
