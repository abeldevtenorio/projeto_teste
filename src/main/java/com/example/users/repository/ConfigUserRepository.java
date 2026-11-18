package com.example.users.repository;

import com.example.users.config.UsersConfigLoader;
import com.example.users.model.User;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Repositorio que carrega os utilizadores de um ficheiro de configuracao YAML.
 */
public final class ConfigUserRepository implements UserRepository {

    private final List<User> users;

    public ConfigUserRepository(List<User> users) {
        this.users = List.copyOf(Objects.requireNonNull(users, "users must not be null"));
    }

    public static ConfigUserRepository fromClasspath(String resource) {
        return new ConfigUserRepository(UsersConfigLoader.loadFromClasspath(resource));
    }

    public static ConfigUserRepository fromFile(Path path) {
        return new ConfigUserRepository(UsersConfigLoader.load(path));
    }

    @Override
    public List<User> findAll() {
        return users;
    }
}
