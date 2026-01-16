package com.bondhub.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration Properties
 * Binds to jwt.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Secret key for JWT signing and verification
     * Should be at least 256 bits (32 characters) for HS256
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds
     * Default: 3600000 (1 hour)
     */
    private Long accessTokenExpiration = 3600000L;

    /**
     * Refresh token expiration time in milliseconds
     * Default: 604800000 (7 days)
     */
    private Long refreshTokenExpiration = 604800000L;
}
