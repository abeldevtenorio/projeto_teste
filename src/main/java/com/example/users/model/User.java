package com.example.users.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utilizador com as suas roles normalizadas (minusculas, sem espacos e sem duplicados).
 */
public record User(String username, Set<String> roles) {

    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        username = username.trim();
        roles = normalizeRoles(roles);
    }

    public static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }

    public static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            String value = normalizeRole(role);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
