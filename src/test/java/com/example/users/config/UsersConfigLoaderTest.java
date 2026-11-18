package com.example.users.config;

import com.example.users.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsersConfigLoader")
class UsersConfigLoaderTest {

    @Test
    @DisplayName("carrega utilizadores e roles do ficheiro de configuracao")
    void loadsUsersAndRolesFromConfiguration() {
        List<User> users = UsersConfigLoader.loadFromClasspath("users-test.yml");

        assertThat(users).extracting(User::username).containsExactly("alice", "bob", "carol");
        assertThat(users.get(0).roles()).containsExactlyInAnyOrder("admin", "developer");
        assertThat(users.get(1).roles()).containsExactly("developer");
    }

    @Test
    void normalizesRepeatedUntidyRolesAndTrimsUsername() {
        List<User> users = load("""
                users:
                  - username: "  Alice  "
                    roles:
                      - " Admin "
                      - ADMIN
                      - developer
                      - "   "
                """);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).username()).isEqualTo("Alice");
        assertThat(users.get(0).roles()).containsExactly("admin", "developer");
    }

    @Test
    void treatsMissingRolesAsEmptySet() {
        List<User> users = load("""
                users:
                  - username: bob
                """);

        assertThat(users.get(0).roles()).isEmpty();
    }

    @Test
    void rejectsConfigurationWithoutUsersList() {
        assertThatThrownBy(() -> load("users: alice"))
                .isInstanceOf(UsersConfigException.class)
                .hasMessageContaining("'users' list");
    }

    @Test
    void rejectsUserWithoutUsername() {
        assertThatThrownBy(() -> load("""
                users:
                  - roles: [admin]
                """))
                .isInstanceOf(UsersConfigException.class)
                .hasMessageContaining("users[0].username");
    }

    @Test
    void rejectsNonStringRole() {
        assertThatThrownBy(() -> load("""
                users:
                  - username: alice
                    roles: [admin, 7]
                """))
                .isInstanceOf(UsersConfigException.class)
                .hasMessageContaining("only strings");
    }

    @Test
    void rejectsInvalidYaml() {
        assertThatThrownBy(() -> load("users: ["))
                .isInstanceOf(UsersConfigException.class)
                .hasMessageContaining("Invalid YAML");
    }

    @Test
    void throwsWhenClasspathResourceIsMissing() {
        assertThatThrownBy(() -> UsersConfigLoader.loadFromClasspath("does-not-exist.yml"))
                .isInstanceOf(UsersConfigException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void loadsConfigurationFromFileSystem(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("users.yml");
        Files.writeString(file, """
                users:
                  - username: dave
                    roles: [qa]
                """, StandardCharsets.UTF_8);

        assertThat(UsersConfigLoader.load(file))
                .extracting(User::username)
                .containsExactly("dave");
    }

    private static List<User> load(String yaml) {
        return UsersConfigLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }
}
