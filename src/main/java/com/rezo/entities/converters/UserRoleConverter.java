package com.rezo.entities.converters;

import com.rezo.entities.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleConverter.class);

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        // Backward compatibility for legacy role values kept in old datasets.
        if ("EMPLOI".equals(normalized)) {
            return UserRole.ENTREPRISE;
        }

        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("UserRoleConverter: unknown role '{}' mapped to ETUDIANT to avoid request failure.", dbData);
            return UserRole.ETUDIANT;
        }
    }
}
