package br.com.faculdade.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
@AutoConfigureMockMvc
class LoginServiceApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MeterRegistry registry;

    @Test
    void loginValidoRetorna200() throws Exception {
        String usuario = novoUsuario("abc123");

        login(usuario, "abc123").andExpect(status().isOk());
    }

    @Test
    void loginInvalidoRetorna401() throws Exception {
        String usuario = novoUsuario("abc123");

        login(usuario, "errada").andExpect(status().isUnauthorized());
        login("naoexiste", "qualquer").andExpect(status().isUnauthorized());
    }

    @Test
    void segundaFalhaBloqueiaUsuarioEAdminDesbloqueia() throws Exception {
        String usuario = novoUsuario("abc123");

        login(usuario, "errada").andExpect(status().isUnauthorized());
        login(usuario, "errada").andExpect(status().isUnauthorized());
        // bloqueado: mesmo com a senha correta o login é negado
        login(usuario, "abc123").andExpect(status().isUnauthorized());

        mvc.perform(put("/unlock/" + usuario).with(httpBasic("manutencao", "manut123")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/unlock/" + usuario).with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        login(usuario, "abc123").andExpect(status().isOk());
    }

    @Test
    void loginValidoZeraFalhas() throws Exception {
        String usuario = novoUsuario("abc123");

        login(usuario, "errada").andExpect(status().isUnauthorized());
        login(usuario, "abc123").andExpect(status().isOk());
        login(usuario, "errada").andExpect(status().isUnauthorized());
        login(usuario, "abc123").andExpect(status().isOk());
    }

    @Test
    void trocarSenha() throws Exception {
        String usuario = novoUsuario("abc123");

        trocarSenha(usuario, "errada", "nova123").andExpect(status().isUnauthorized());
        trocarSenha(usuario, "abc123", "senhamuitolonga").andExpect(status().isBadRequest());
        trocarSenha(usuario, "abc123", "nova123").andExpect(status().isOk());
        trocarSenha("naoexiste", "abc123", "nova123").andExpect(status().isNotFound());

        login(usuario, "nova123").andExpect(status().isOk());
    }

    @Test
    void cadastroExigeRoleManutencao() throws Exception {
        String corpo = credenciais("x" + UUID.randomUUID().toString().substring(0, 8), "abc");

        mvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/").with(httpBasic("manutencao", "manut123"))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/").with(httpBasic("manutencao", "manut123"))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    void listagemExigeRoleManutencao() throws Exception {
        mvc.perform(get("/")).andExpect(status().isUnauthorized());
        mvc.perform(get("/").with(httpBasic("admin", "admin123"))).andExpect(status().isForbidden());
        mvc.perform(get("/").with(httpBasic("manutencao", "manut123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'joao')]").exists())
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test
    void metricasDeLogin() throws Exception {
        double validos = registry.get("login.validos").counter().count();
        double invalidos = registry.get("login.invalidos").counter().count();
        double bloqueados = registry.get("usuarios.bloqueados").gauge().value();
        String usuario = novoUsuario("abc123");

        login(usuario, "abc123");
        login(usuario, "errada");
        login(usuario, "errada");

        assertThat(registry.get("login.validos").counter().count()).isEqualTo(validos + 1);
        assertThat(registry.get("login.invalidos").counter().count()).isEqualTo(invalidos + 2);
        assertThat(registry.get("usuarios.bloqueados").gauge().value()).isEqualTo(bloqueados + 1);
    }

    @Test
    void swaggerEPrometheusSaoPublicos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
        // Nos testes o export de métricas é desligado (404); aqui só importa que a segurança libere o acesso
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private String novoUsuario(String senha) throws Exception {
        String usuario = "u" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/").with(httpBasic("manutencao", "manut123"))
                .contentType(MediaType.APPLICATION_JSON).content(credenciais(usuario, senha)))
                .andExpect(status().isCreated());
        return usuario;
    }

    private ResultActions login(String usuario, String senha) throws Exception {
        return mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(credenciais(usuario, senha)));
    }

    private ResultActions trocarSenha(String usuario, String atual, String nova) throws Exception {
        return mvc.perform(put("/trocarsenha/" + usuario).contentType(MediaType.APPLICATION_JSON)
                .content("{\"atual\":\"" + atual + "\",\"nova\":\"" + nova + "\"}"));
    }

    private static String credenciais(String usuario, String senha) {
        return "{\"username\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}";
    }
}
