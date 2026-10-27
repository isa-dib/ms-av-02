package br.com.faculdade.login.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Login Service", version = "1.0.0",
        description = "Login, troca de senha, cadastro e desbloqueio de usuários. "
                + "Endpoints de manutenção e administração exigem HTTP Basic de um operador com a role adequada."))
@SecurityScheme(name = OpenApiConfig.BASIC_AUTH, type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";
}
