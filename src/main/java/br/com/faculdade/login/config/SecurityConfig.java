package br.com.faculdade.login.config;

import static org.springframework.security.config.Customizer.withDefaults;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .formLogin(config -> config.disable())
                .httpBasic(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos do serviço
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/trocarsenha/*").permitAll()
                        // Endpoints protegidos por role
                        .requestMatchers(HttpMethod.POST, "/").hasRole("MANUTENCAO")
                        .requestMatchers(HttpMethod.GET, "/").hasRole("MANUTENCAO")
                        .requestMatchers(HttpMethod.PUT, "/unlock/*").hasRole("ADMIN")
                        // Documentação, observabilidade e console H2
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JdbcUserDetailsManager userDetailsService(DataSource datasource) {
        return new JdbcUserDetailsManager(datasource);
    }

    /** Cria os operadores da API (tabelas users/authorities) com senha em BCrypt. */
    @Bean
    ApplicationRunner operadoresIniciais(JdbcUserDetailsManager manager, PasswordEncoder encoder,
            @Value("${seguranca.admin.username}") String adminUser,
            @Value("${seguranca.admin.password}") String adminSenha,
            @Value("${seguranca.manutencao.username}") String manutencaoUser,
            @Value("${seguranca.manutencao.password}") String manutencaoSenha) {
        return args -> {
            if (!manager.userExists(adminUser)) {
                manager.createUser(User.withUsername(adminUser)
                        .password(encoder.encode(adminSenha))
                        .roles("ADMIN")
                        .build());
            }
            if (!manager.userExists(manutencaoUser)) {
                manager.createUser(User.withUsername(manutencaoUser)
                        .password(encoder.encode(manutencaoSenha))
                        .roles("MANUTENCAO")
                        .build());
            }
        };
    }
}
