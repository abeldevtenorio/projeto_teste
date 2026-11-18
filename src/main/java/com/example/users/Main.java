package com.example.users;

import com.example.users.model.RoleMatchMode;
import com.example.users.model.User;
import com.example.users.repository.ConfigUserRepository;
import com.example.users.service.UserFilterService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Demonstracao: mostra os utilizadores de users.yml que tem as roles pedidas.
 *
 * <pre>java -cp target/classes:$CP com.example.users.Main ANY admin developer</pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: <ANY|ALL> <role> [role...]");
            System.exit(2);
            return;
        }

        RoleMatchMode mode;
        try {
            mode = RoleMatchMode.valueOf(args[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("Modo invalido: " + args[0] + " (use ANY ou ALL)");
            System.exit(2);
            return;
        }

        Set<String> roles = new LinkedHashSet<>(Arrays.asList(args).subList(1, args.length));
        UserFilterService service = new UserFilterService(ConfigUserRepository.fromClasspath("users.yml"));

        List<User> matched = service.filterByRoles(roles, mode);
        if (matched.isEmpty()) {
            System.out.println("Nenhum utilizador corresponde as roles " + roles + " (modo " + mode + ").");
            return;
        }

        System.out.println("Utilizadores com as roles " + roles + " (modo " + mode + "):");
        matched.forEach(user -> System.out.println(" - " + user.username() + " " + user.roles()));
    }
}
