package com.example.users.service;

import com.example.users.model.RoleMatchMode;
import com.example.users.model.User;
import com.example.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFilterService - filtro de utilizadores por roles")
class UserFilterServiceTest {

    private static final User ALICE = new User("alice", Set.of("admin", "developer"));
    private static final User BOB = new User("bob", Set.of("developer"));
    private static final User CAROL = new User("carol", Set.of("qa", "developer"));
    private static final User DAVE = new User("dave", Set.of("admin"));

    @Mock
    private UserRepository repository;

    private UserFilterService service;

    @BeforeEach
    void setUp() {
        service = new UserFilterService(repository);
    }

    @Test
    @DisplayName("devolve apenas quem tem a role pedida (modo ANY por omissao)")
    void returnsUsersWithRequestedRole() {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB, CAROL, DAVE));

        assertThat(service.filterByRoles(Set.of("admin")))
                .extracting(User::username)
                .containsExactly("alice", "dave");
    }

    @Test
    @DisplayName("modo ANY devolve a uniao de quem tem pelo menos uma das roles")
    void anyModeReturnsUsersMatchingAtLeastOneRole() {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB, CAROL, DAVE));

        assertThat(service.filterByRoles(Set.of("qa", "developer"), RoleMatchMode.ANY))
                .extracting(User::username)
                .containsExactly("alice", "bob", "carol");
    }

    @ParameterizedTest(name = "modo {0} com roles developer+qa devolve [{1}]")
    @CsvSource(value = {
            "ANY; alice,bob,carol",
            "ALL; carol"
    }, delimiter = ';')
    void appliesRequestedMatchMode(RoleMatchMode mode, String expectedUsers) {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB, CAROL, DAVE));

        assertThat(service.filterByRoles(Set.of("developer", "qa"), mode))
                .extracting(User::username)
                .containsExactly(expectedUsers.split(","));
    }

    @Test
    void ignoresCaseAndSurroundingSpacesInRoles() {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB, CAROL, DAVE));

        assertThat(service.filterByRoles(Set.of("  ADMIN ")))
                .extracting(User::username)
                .containsExactly("alice", "dave");
    }

    @Test
    void returnsEachUserOnlyOnceWhenItMatchesSeveralRoles() {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB));

        assertThat(service.filterByRoles(Set.of("admin", "developer"), RoleMatchMode.ANY))
                .extracting(User::username)
                .containsExactly("alice", "bob");
    }

    @Test
    void returnsEmptyListWhenNobodyHasTheRole() {
        when(repository.findAll()).thenReturn(List.of(ALICE, BOB, CAROL, DAVE));

        assertThat(service.filterByRoles(Set.of("finance"))).isEmpty();
    }

    @Test
    void queriesTheRepositoryOnlyOnce() {
        when(repository.findAll()).thenReturn(List.of(ALICE, DAVE));

        service.filterByRoles(Set.of("admin"));

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("falha sem roles para nunca devolver todos os utilizadores por engano")
    void rejectsMissingRoles() {
        assertThatThrownBy(() -> service.filterByRoles(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one role");
        assertThatThrownBy(() -> service.filterByRoles(Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one role");
        assertThatThrownBy(() -> service.filterByRoles(Set.of(" ", "\t")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one role");

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullMatchMode() {
        assertThatThrownBy(() -> service.filterByRoles(Set.of("admin"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mode");

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullRepository() {
        assertThatThrownBy(() -> new UserFilterService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userRepository");
    }

    @Test
    void propagatesRepositoryFailures() {
        when(repository.findAll()).thenThrow(new IllegalStateException("configuracao indisponivel"));

        assertThatThrownBy(() -> service.filterByRoles(Set.of("admin")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("configuracao indisponivel");
    }
}
