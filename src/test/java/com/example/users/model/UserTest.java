package com.example.users.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User")
class UserTest {

    @Test
    void trimsUsernameAndNormalizesRoles() {
        Set<String> roles = new LinkedHashSet<>(Arrays.asList(" Admin ", "ADMIN", "developer", "  ", null));

        User user = new User("  Alice  ", roles);

        assertThat(user.username()).isEqualTo("Alice");
        assertThat(user.roles()).containsExactly("admin", "developer");
    }

    @Test
    void usesEmptySetWhenRolesAreNull() {
        assertThat(new User("bob", null).roles()).isEmpty();
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> new User("   ", Set.of("admin")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");

        assertThatThrownBy(() -> new User(null, Set.of("admin")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }
}
