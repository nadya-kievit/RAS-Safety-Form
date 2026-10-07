package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class SpaFallbackResolverTest {

    private final WebConfig.SpaFallbackResolver resolver = new WebConfig.SpaFallbackResolver();
    private final Resource location = new ClassPathResource("static/");

    @Test
    void servesClientSideRoutesFromIndexHtml() throws Exception {
        for (String route : new String[] {"", "login", "framer/submissions/12", "admin/sites"}) {
            Resource resource = resolver.getResource(route, location);
            assertNotNull(resource, route);
            assertEquals("index.html", resource.getFilename());
        }
    }

    @Test
    void servesRealStaticFilesAsIs() throws Exception {
        assertEquals("app.js", resolver.getResource("assets/app.js", location).getFilename());
    }

    @Test
    void missingFilesAndUnknownApiRoutesAreNotMaskedByIndexHtml() throws Exception {
        assertNull(resolver.getResource("assets/missing.js", location));
        assertNull(resolver.getResource("favicon.ico", location));
        assertNull(resolver.getResource("api/unknown", location));
        assertNull(resolver.getResource("api", location));
    }
}
