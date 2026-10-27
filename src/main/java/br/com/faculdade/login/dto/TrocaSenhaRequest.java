package br.com.faculdade.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TrocaSenha", description = "Senha atual e nova senha do usuário")
public record TrocaSenhaRequest(

        @Schema(description = "Senha atual", example = "joao123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String atual,

        @Schema(description = "Nova senha (máximo 10 caracteres)", example = "nova123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10)
        String nova) {
}
