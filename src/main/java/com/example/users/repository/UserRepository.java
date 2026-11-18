package com.example.users.repository;

import com.example.users.model.User;

import java.util.List;

/**
 * Fonte de utilizadores. Implementacoes podem ler de um ficheiro, base de dados ou API.
 */
public interface UserRepository {

    List<User> findAll();
}
