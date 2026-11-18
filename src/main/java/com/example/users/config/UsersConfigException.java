package com.example.users.config;

/**
 * Lancada quando o ficheiro de configuracao de utilizadores/roles e invalido ou nao existe.
 */
public class UsersConfigException extends RuntimeException {

    public UsersConfigException(String message) {
        super(message);
    }

    public UsersConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
