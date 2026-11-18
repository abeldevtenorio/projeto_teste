package com.example.users.config;

import com.example.users.model.User;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Le a configuracao YAML com o seguinte formato:
 *
 * <pre>
 * users:
 *   - username: alice
 *     roles: [admin, developer]
 * </pre>
 */
public final class UsersConfigLoader {

    private static final String USERS_KEY = "users";

    private UsersConfigLoader() {
    }

    public static List<User> loadFromClasspath(String resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        try (InputStream input = UsersConfigLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new UsersConfigException("Users configuration not found on classpath: " + resource);
            }
            return load(input);
        } catch (IOException e) {
            throw new UsersConfigException("Unable to close users configuration: " + resource, e);
        }
    }

    public static List<User> load(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path)) {
            return load(input);
        } catch (IOException e) {
            throw new UsersConfigException("Unable to read users configuration: " + path, e);
        }
    }

    public static List<User> load(InputStream input) {
        Objects.requireNonNull(input, "input must not be null");

        Object root;
        try {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
        } catch (RuntimeException e) {
            throw new UsersConfigException("Invalid YAML configuration", e);
        }

        if (!(root instanceof Map<?, ?> config)) {
            throw new UsersConfigException("Configuration root must be a mapping with a 'users' key");
        }
        if (!(config.get(USERS_KEY) instanceof List<?> entries)) {
            throw new UsersConfigException("Configuration must contain a 'users' list");
        }

        List<User> users = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            users.add(toUser(entries.get(index), index));
        }
        return List.copyOf(users);
    }

    private static User toUser(Object entry, int index) {
        if (!(entry instanceof Map<?, ?> user)) {
            throw new UsersConfigException(
                    "users[" + index + "] must be a mapping with 'username' and optional 'roles'");
        }
        if (!(user.get("username") instanceof String username) || username.isBlank()) {
            throw new UsersConfigException("users[" + index + "].username must be a non-blank string");
        }
        return new User(username, toRoles(user.get("roles"), index));
    }

    private static Set<String> toRoles(Object node, int index) {
        if (node == null) {
            return Set.of();
        }
        if (!(node instanceof List<?> values)) {
            throw new UsersConfigException("users[" + index + "].roles must be a list of strings");
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof String role)) {
                throw new UsersConfigException("users[" + index + "].roles must contain only strings");
            }
            roles.add(role);
        }
        return roles;
    }
}
