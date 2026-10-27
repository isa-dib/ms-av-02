package br.com.faculdade.login.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.faculdade.login.config.OpenApiConfig;
import br.com.faculdade.login.dto.CredenciaisRequest;
import br.com.faculdade.login.dto.MensagemResponse;
import br.com.faculdade.login.dto.TrocaSenhaRequest;
import br.com.faculdade.login.dto.UsuarioResponse;
import br.com.faculdade.login.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
public class UsuarioController {

    private final LoginService service;

    public UsuarioController(LoginService service) {
        this.service = service;
    }

    @Tag(name = "autenticação")
    @Operation(summary = "Efetua login", description = "Valida usuário e senha na tabela usuarios. "
            + "Após mais de uma falha consecutiva o usuário é bloqueado. Acesso público.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário e senha válidos",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Usuário e senha inválidos ou usuário bloqueado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))) })
    @PostMapping("/login")
    public MensagemResponse login(@Valid @RequestBody CredenciaisRequest credenciais) {
        service.login(credenciais.username(), credenciais.senha());
        return new MensagemResponse("Login efetuado com sucesso");
    }

    @Tag(name = "autenticação")
    @Operation(summary = "Troca a senha", description = "Troca a senha do usuário após conferir a senha atual. "
            + "Senha atual errada conta como falha para o bloqueio. Acesso público.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nova senha inválida (vazia ou com mais de 10 caracteres)",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Senha atual inválida",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "423", description = "Usuário bloqueado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))) })
    @PutMapping("/trocarsenha/{userid}")
    public MensagemResponse trocarSenha(
            @Parameter(description = "Nome do usuário", example = "joao") @PathVariable String userid,
            @Valid @RequestBody TrocaSenhaRequest troca) {
        service.trocarSenha(userid, troca.atual(), troca.nova());
        return new MensagemResponse("Senha alterada com sucesso");
    }

    @Tag(name = "manutenção")
    @Operation(summary = "Cadastra usuário", description = "Cadastra um novo usuário na tabela usuarios. Requer ROLE_MANUTENCAO.",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais do operador ausentes ou inválidas", content = @Content),
            @ApiResponse(responseCode = "403", description = "Operador sem a role MANUTENCAO", content = @Content),
            @ApiResponse(responseCode = "409", description = "Usuário já cadastrado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))) })
    @PostMapping("/")
    public ResponseEntity<MensagemResponse> cadastrar(@Valid @RequestBody CredenciaisRequest novo) {
        service.cadastrar(novo.username(), novo.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MensagemResponse("Usuário cadastrado com sucesso"));
    }

    @Tag(name = "manutenção")
    @Operation(summary = "Lista usuários", description = "Retorna todos os usuários cadastrados e sua situação de bloqueio. "
            + "Requer ROLE_MANUTENCAO.",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Credenciais do operador ausentes ou inválidas", content = @Content),
            @ApiResponse(responseCode = "403", description = "Operador sem a role MANUTENCAO", content = @Content) })
    @GetMapping("/")
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @Tag(name = "administração")
    @Operation(summary = "Desbloqueia usuário", description = "Remove o bloqueio e zera as falhas de login do usuário. "
            + "Requer ROLE_ADMIN.",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário desbloqueado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais do operador ausentes ou inválidas", content = @Content),
            @ApiResponse(responseCode = "403", description = "Operador sem a role ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))) })
    @PutMapping("/unlock/{userid}")
    public MensagemResponse desbloquear(
            @Parameter(description = "Nome do usuário a desbloquear", example = "joao") @PathVariable String userid) {
        service.desbloquear(userid);
        return new MensagemResponse("Usuário desbloqueado com sucesso");
    }
}
