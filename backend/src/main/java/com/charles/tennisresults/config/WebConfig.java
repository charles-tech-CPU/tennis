package com.charles.tennisresults.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Origines autorisees a appeler l'API, lues dans la variable d'environnement
 * CORS_ALLOWED_ORIGINS : une liste de motifs separes par des virgules, espaces
 * toleres (ex: "http://*:8180,https://*:8280"). Un motif accepte "*" dans l'hote et
 * "[*]" pour le port (ex: "https://*.ts.net:[*]").
 *
 * Variable absente, vide ou sans motif exploitable : DEFAULT_ALLOWED_ORIGINS, soit le
 * serveur de dev Vite (port 5175) quel que soit l'hote (le front appelle l'API via
 * window.location.hostname) et les pages servies en HTTPS via Tailscale.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    static final String DEFAULT_ALLOWED_ORIGINS = "http://*:5175,https://*.ts.net:[*]";

    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = parseOrigins(allowedOrigins);
    }

    /** Motifs de la liste, sans espaces ni entrees vides ; les motifs par defaut si la liste n'en contient aucun. */
    static String[] parseOrigins(String list) {
        String[] patterns = split(list);
        return patterns.length > 0 ? patterns : split(DEFAULT_ALLOWED_ORIGINS);
    }

    private static String[] split(String list) {
        if (list == null) {
            return new String[0];
        }
        return Arrays.stream(list.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
