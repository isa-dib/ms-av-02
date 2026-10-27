package br.com.faculdade.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Usuario", description = "Usuário cadastrado e sua situação de bloqueio")
public record UsuarioResponse(

        @Schema(description = "Nome do usuário", example = "joao")
        String username,

        @Schema(description = "Quantidade de falhas de login consecutivas", example = "0")
        int tentativasFalhas,

        @Schema(description = "Indica se o usuário está bloqueado", example = "false")
        boolean bloqueado) {
}
