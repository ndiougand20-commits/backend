package com.rezo.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacyUserRoleNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyUserRoleNormalizer.class);

    private final JdbcTemplate jdbcTemplate;

    public LegacyUserRoleNormalizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void normalizeLegacyRoles() {
        // Legacy data may still contain EMPLOI although UserRole no longer supports it.
        int updated = jdbcTemplate.update(
            "update users set role = 'ENTREPRISE' where upper(role) = 'EMPLOI'"
        );
        if (updated > 0) {
            LOGGER.warn("LegacyUserRoleNormalizer: {} user role(s) migrated from EMPLOI to ENTREPRISE.", updated);
        }
    }
}
