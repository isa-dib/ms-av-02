package br.com.faculdade.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "Credenciais", description = "Usuário e senha usados no login e no cadastro")
public record CredenciaisRequest(

        @Schema(description = "Nome do usuário", example = "joao", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String username,

        @Schema(description = "Senha do usuário (máximo 10 caracteres)", example = "joao123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10)
        String senha) {
}
