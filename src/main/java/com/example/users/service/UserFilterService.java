package com.example.users.service;

import com.example.users.model.RoleMatchMode;
import com.example.users.model.User;
import com.example.users.repository.UserRepository;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filtra utilizadores por roles.
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>As roles sao comparadas sem distincao de maiusculas/minusculas e sem espacos extra.</li>
 *   <li>Sem roles pedidas o filtro falha, para nunca expor todos os utilizadores por engano.</li>
 *   <li>O resultado e ordenado por username (ignorando maiusculas/minusculas).</li>
 * </ul>
 */
public final class UserFilterService {

    private final UserRepository userRepository;

    public UserFilterService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public List<User> filterByRoles(Set<String> roles) {
        return filterByRoles(roles, RoleMatchMode.ANY);
    }

    public List<User> filterByRoles(Set<String> roles, RoleMatchMode mode) {
        Objects.requireNonNull(mode, "mode must not be null");
        Set<String> wantedRoles = normalize(roles);
        if (wantedRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be provided to filter users");
        }

        return userRepository.findAll().stream()
                .filter(user -> mode.matches(user.roles(), wantedRoles))
                .sorted(Comparator.comparing(User::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Set<String> normalize(Set<String> roles) {
        if (roles == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            String value = User.normalizeRole(role);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }
}
