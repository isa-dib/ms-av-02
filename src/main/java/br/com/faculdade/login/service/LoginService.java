package br.com.faculdade.login.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.faculdade.login.dto.UsuarioResponse;
import br.com.faculdade.login.exception.NegocioException;
import br.com.faculdade.login.metrics.LoginMetricas;
import br.com.faculdade.login.repository.LoginAttemptRepository;
import br.com.faculdade.login.repository.UsuarioRepository;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UsuarioRepository usuarios;
    private final LoginAttemptRepository attempts;
    private final LoginMetricas metricas;
    private final int maxFalhas;

    public LoginService(UsuarioRepository usuarios, LoginAttemptRepository attempts, LoginMetricas metricas,
            @Value("${login.max-falhas:1}") int maxFalhas) {
        this.usuarios = usuarios;
        this.attempts = attempts;
        this.metricas = metricas;
        this.maxFalhas = maxFalhas;
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public void login(String username, String senha) {
        if (attempts.bloqueado(username)) {
            metricas.loginInvalido();
            log.warn("Login negado para usuário bloqueado: {}", username);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Usuário bloqueado");
        }

        String senhaCadastrada = usuarios.buscarSenha(username).orElse(null);
        if (senhaCadastrada == null || !senhaConfere(senhaCadastrada, senha)) {
            metricas.loginInvalido();
            if (senhaCadastrada != null && attempts.registrarFalha(username, maxFalhas)) {
                log.warn("Usuário {} bloqueado por excesso de falhas de login", username);
                throw new NegocioException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos. Usuário bloqueado");
            }
            log.info("Login inválido para usuário: {}", username);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }

        attempts.resetar(username);
        metricas.loginValido();
        log.info("Login válido para usuário: {}", username);
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public void trocarSenha(String username, String atual, String nova) {
        String senhaCadastrada = usuarios.buscarSenha(username)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (attempts.bloqueado(username)) {
            throw new NegocioException(HttpStatus.LOCKED, "Usuário bloqueado");
        }

        // A senha atual errada conta como falha para evitar tentativa e erro por este endpoint público
        if (!senhaConfere(senhaCadastrada, atual)) {
            if (attempts.registrarFalha(username, maxFalhas)) {
                log.warn("Usuário {} bloqueado por excesso de falhas na troca de senha", username);
                throw new NegocioException(HttpStatus.UNAUTHORIZED, "Senha atual inválida. Usuário bloqueado");
            }
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Senha atual inválida");
        }

        usuarios.atualizarSenha(username, nova);
        attempts.resetar(username);
        log.info("Senha alterada para usuário: {}", username);
    }

    @Transactional
    public void cadastrar(String username, String senha) {
        if (usuarios.existe(username)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Usuário já cadastrado");
        }
        usuarios.inserir(username, senha);
        log.info("Usuário cadastrado: {}", username);
    }

    @Transactional
    public void desbloquear(String username) {
        if (!usuarios.existe(username)) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }
        attempts.resetar(username);
        log.info("Usuário desbloqueado: {}", username);
    }

    public List<UsuarioResponse> listar() {
        return usuarios.listar();
    }

    private static boolean senhaConfere(String cadastrada, String informada) {
        return MessageDigest.isEqual(cadastrada.getBytes(StandardCharsets.UTF_8),
                informada.getBytes(StandardCharsets.UTF_8));
    }
}
