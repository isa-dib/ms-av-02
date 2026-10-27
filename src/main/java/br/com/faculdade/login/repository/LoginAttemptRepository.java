package br.com.faculdade.login.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acesso à tabela login_attempts (controle de falhas e bloqueio). */
@Repository
public class LoginAttemptRepository {

    private final JdbcClient jdbc;

    public LoginAttemptRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean bloqueado(String username) {
        return jdbc.sql("SELECT locked FROM login_attempts WHERE username = ?")
                .param(username)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /**
     * Registra uma falha e bloqueia o usuário quando a quantidade de falhas
     * ultrapassar maxFalhas. Retorna true se o usuário ficou bloqueado.
     */
    public boolean registrarFalha(String username, int maxFalhas) {
        int alterados = jdbc.sql("""
                UPDATE login_attempts
                   SET failed_attempts = failed_attempts + 1,
                       last_failed_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE username = ?
                """)
                .param(username)
                .update();
        if (alterados == 0) {
            jdbc.sql("""
                    INSERT INTO login_attempts (username, failed_attempts, last_failed_at, updated_at)
                    VALUES (?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)
                    .param(username)
                    .update();
        }
        return jdbc.sql("""
                UPDATE login_attempts
                   SET locked = TRUE,
                       locked_at = CURRENT_TIMESTAMP
                 WHERE username = ? AND locked = FALSE AND failed_attempts > ?
                """)
                .params(username, maxFalhas)
                .update() > 0;
    }

    /** Zera as falhas e remove o bloqueio (login válido ou desbloqueio pelo administrador). */
    public void resetar(String username) {
        jdbc.sql("""
                UPDATE login_attempts
                   SET failed_attempts = 0,
                       locked = FALSE,
                       locked_at = NULL,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE username = ?
                """)
                .param(username)
                .update();
    }

    public long totalBloqueados() {
        return jdbc.sql("SELECT COUNT(*) FROM login_attempts WHERE locked = TRUE")
                .query(Long.class)
                .single();
    }
}
