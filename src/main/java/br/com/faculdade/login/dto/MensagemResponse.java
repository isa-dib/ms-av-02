package br.com.faculdade.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Mensagem", description = "Mensagem de retorno da operação")
public record MensagemResponse(

        @Schema(description = "Texto da mensagem", example = "Login efetuado com sucesso")
        String mensagem) {
}
