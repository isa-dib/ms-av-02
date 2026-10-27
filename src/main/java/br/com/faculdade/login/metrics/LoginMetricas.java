package br.com.faculdade.login.metrics;

import org.springframework.stereotype.Component;

import br.com.faculdade.login.repository.LoginAttemptRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Indicadores expostos em /actuator/metrics e /actuator/prometheus:
 * login_validos_total, login_invalidos_total e usuarios_bloqueados.
 */
@Component
public class LoginMetricas {

    private final Counter loginsValidos;
    private final Counter loginsInvalidos;

    public LoginMetricas(MeterRegistry registry, LoginAttemptRepository attempts) {
        loginsValidos = Counter.builder("login.validos")
                .description("Quantidade de logins válidos")
                .register(registry);
        loginsInvalidos = Counter.builder("login.invalidos")
                .description("Quantidade de logins inválidos")
                .register(registry);
        Gauge.builder("usuarios.bloqueados", attempts, LoginAttemptRepository::totalBloqueados)
                .description("Total de usuários bloqueados")
                .register(registry);
    }

    public void loginValido() {
        loginsValidos.increment();
    }

    public void loginInvalido() {
        loginsInvalidos.increment();
    }
}
