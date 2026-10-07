package com.ras.safetyform.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
public class SessionConfig {

    static final int COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(30).toSeconds();

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${app.session.cookie-secure:false}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("RAS_SESSION");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(COOKIE_MAX_AGE_SECONDS);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
