package com.example.users.repository;

import com.example.users.model.User;
import com.example.users.service.UserFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigUserRepository")
class ConfigUserRepositoryTest {

    @Test
    void exposesUsersFromClasspathConfiguration() {
        ConfigUserRepository repository = ConfigUserRepository.fromClasspath("users-test.yml");

        assertThat(repository.findAll())
                .extracting(User::username)
                .containsExactly("alice", "bob", "carol");
    }

    @Test
    void exposesAnImmutableSnapshot() {
        List<User> users = new ConfigUserRepository(List.of(new User("alice", Set.of("admin")))).findAll();

        assertThatThrownBy(() -> users.add(new User("bob", Set.of("qa"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void loadsConfigurationFromFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("users.yml");
        Files.writeString(file, """
                users:
                  - username: dave
                    roles: [qa]
                """, StandardCharsets.UTF_8);

        assertThat(ConfigUserRepository.fromFile(file).findAll())
                .extracting(User::username)
                .containsExactly("dave");
    }

    @Test
    void rejectsNullUsers() {
        assertThatThrownBy(() -> new ConfigUserRepository(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("users");
    }

    @Test
    @DisplayName("filtra os utilizadores do ficheiro de configuracao por role")
    void filtersConfiguredUsersByRole() {
        List<User> developers = new UserFilterService(ConfigUserRepository.fromClasspath("users-test.yml"))
                .filterByRoles(Set.of("DEVELOPER"));

        assertThat(developers)
                .extracting(User::username)
                .containsExactly("alice", "bob");
    }
}
