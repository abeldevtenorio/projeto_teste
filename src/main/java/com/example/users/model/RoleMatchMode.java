package com.example.users.model;

import java.util.Set;

/**
 * Define como as roles pedidas sao comparadas com as roles do utilizador.
 *
 * <p>Assume que ambos os conjuntos ja foram normalizados por {@link User#normalizeRoles(Set)}.</p>
 */
public enum RoleMatchMode {

    /** O utilizador corresponde se tiver pelo menos uma das roles pedidas. */
    ANY {
        @Override
        public boolean matches(Set<String> userRoles, Set<String> wantedRoles) {
            return wantedRoles.stream().anyMatch(userRoles::contains);
        }
    },

    /** O utilizador corresponde apenas se tiver todas as roles pedidas. */
    ALL {
        @Override
        public boolean matches(Set<String> userRoles, Set<String> wantedRoles) {
            return userRoles.containsAll(wantedRoles);
        }
    };

    public abstract boolean matches(Set<String> userRoles, Set<String> wantedRoles);
}
