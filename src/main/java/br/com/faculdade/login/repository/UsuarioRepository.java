package br.com.faculdade.login.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.com.faculdade.login.dto.UsuarioResponse;

/** Acesso à tabela usuarios (usuários do sistema validados pelo POST /login). */
@Repository
public class UsuarioRepository {

    private final JdbcClient jdbc;

    public UsuarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> buscarSenha(String username) {
        return jdbc.sql("SELECT senha FROM usuarios WHERE username = ?")
                .param(username)
                .query(String.class)
                .optional();
    }

    public boolean existe(String username) {
        return buscarSenha(username).isPresent();
    }

    public void inserir(String username, String senha) {
        jdbc.sql("INSERT INTO usuarios (username, senha) VALUES (?, ?)")
                .params(username, senha)
                .update();
    }

    public void atualizarSenha(String username, String senha) {
        jdbc.sql("UPDATE usuarios SET senha = ? WHERE username = ?")
                .params(senha, username)
                .update();
    }

    public List<UsuarioResponse> listar() {
        return jdbc.sql("""
                SELECT u.username,
                       COALESCE(a.failed_attempts, 0) AS failed_attempts,
                       COALESCE(a.locked, FALSE) AS locked
                  FROM usuarios u
                  LEFT JOIN login_attempts a ON a.username = u.username
                 ORDER BY u.username
                """)
                .query((rs, i) -> new UsuarioResponse(
                        rs.getString("username"),
                        rs.getInt("failed_attempts"),
                        rs.getBoolean("locked")))
                .list();
    }
}
