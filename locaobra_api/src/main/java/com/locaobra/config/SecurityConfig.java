package com.locaobra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final java.util.List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}") String allowedOriginsConfig) {
        this.jwtAuthFilter = jwtAuthFilter;
        if (allowedOriginsConfig == null || allowedOriginsConfig.isBlank()) {
            this.allowedOrigins = java.util.Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:5173",
                    "http://localhost:8081",
                    "http://192.168.0.98:5173",
                    "http://172.17.19.249:5173",
                    "https://locaobra.onrender.com",
                    "https://locaobra.vercel.app"
            );
        } else {
            this.allowedOrigins = java.util.Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .filter(o -> !o.isEmpty())
                    .toList();
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ===================== ROTAS PÚBLICAS =====================
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // Health check do Render (e do balanceador): precisa ser público
                // pra não retornar 403 e o serviço ficar marcado como insalubre.
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Catálogo público (loja): qualquer visitante pode listar/ver equipamentos.
                // Atenção: usa "*" (um segmento) e não "**", pra NÃO liberar
                // sub-rotas como /api/equipamentos/{id}/unidades, que são internas.
                .requestMatchers(HttpMethod.GET, "/api/equipamentos", "/api/equipamentos/*").permitAll()
                // Consulta de CEP é usada no cadastro, antes do login
                .requestMatchers(HttpMethod.GET, "/api/v1/enderecos/**").permitAll()

                // ===================== ADMINISTRAÇÃO DE USUÁRIOS =====================
                // Criar/editar/excluir conta de login é só ADMIN. Mas RH e
                // GERENTE_OPERACOES precisam LER a lista pra vincular um
                // funcionário a uma conta existente (funcionarios.jsx usa isso).
                .requestMatchers(HttpMethod.GET, "/api/usuarios/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                // ===================== RH / FUNCIONÁRIOS =====================
                // Gestão de cadastro (criar/editar/excluir) é RH/GERENTE/ADMIN.
                // Leitura também é liberada pra quem só precisa escolher um nome
                // em um dropdown: técnico (responsável pela OS), entregador/conferente
                // e consultor (motorista da expedição).
                .requestMatchers(HttpMethod.GET, "/api/funcionarios/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES", "TECNICO_MANUTENCAO",
                            "ENTREGADOR", "CONFERENTE", "CONSULTOR_LOCACAO")
                .requestMatchers("/api/funcionarios/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES")

                // ===================== CLIENTES =====================
                // Excluir cadastro de cliente é ação sensível (apaga histórico) —
                // mais restrita do que ver/editar clientes no dia a dia.
                .requestMatchers(HttpMethod.DELETE, "/api/clientes/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES")
                // Leitura também é liberada pra entregador/conferente, que
                // escolhem o cliente ao montar uma expedição.
                .requestMatchers(HttpMethod.GET, "/api/clientes/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO",
                            "ANALISTA_CREDENCIAMENTO", "ENTREGADOR", "CONFERENTE")
                .requestMatchers("/api/clientes/**")
                    .hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO", "ANALISTA_CREDENCIAMENTO")

                // ===================== CATÁLOGO (escrita) =====================
                // Criar/editar/excluir modelos de equipamento é gestão de catálogo.
                // Apagar uma imagem é edição normal de catálogo (mesmo grupo do POST/PUT);
                // já excluir o equipamento inteiro é mais restrito, abaixo.
                .requestMatchers(HttpMethod.DELETE, "/api/equipamentos/*/imagens")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO")
                .requestMatchers(HttpMethod.POST, "/api/equipamentos/*/unidades")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")
                .requestMatchers(HttpMethod.POST, "/api/equipamentos/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO")
                .requestMatchers(HttpMethod.PUT, "/api/equipamentos/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO")
                .requestMatchers(HttpMethod.PATCH, "/api/equipamentos/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "CONSULTOR_LOCACAO")
                .requestMatchers(HttpMethod.DELETE, "/api/equipamentos/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")

                // ===================== UNIDADES FÍSICAS =====================
                // Edição completa (patrimônio, número de série etc.) é gestão de frota
                .requestMatchers(HttpMethod.PUT, "/api/unidades/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")
                .requestMatchers(HttpMethod.DELETE, "/api/unidades/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")
                // Ver lista/alertas e mudar status (disponível/limpeza/manutenção) é
                // operação do dia a dia de conferente, técnico e faxineiro; consultor
                // também enxerga essa aba na tela de equipamentos.
                .requestMatchers("/api/unidades/**", "/api/equipamentos/*/unidades")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "CONFERENTE", "TECNICO_MANUTENCAO",
                            "FAXINEIRO", "CONSULTOR_LOCACAO")

                // ===================== EXPEDIÇÃO / LOGÍSTICA =====================
                .requestMatchers(HttpMethod.DELETE, "/api/expedicoes/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")
                .requestMatchers("/api/expedicoes/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "ENTREGADOR", "CONFERENTE", "CONSULTOR_LOCACAO", "TECNICO_MANUTENCAO")

                // ===================== MANUTENÇÃO =====================
                .requestMatchers("/api/ordens-servico/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "TECNICO_MANUTENCAO")
                .requestMatchers("/api/pecas/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES", "TECNICO_MANUTENCAO")

                // ===================== DASHBOARD =====================
                .requestMatchers("/api/dashboard/**")
                    .hasAnyRole("ADMIN", "GERENTE_OPERACOES")

                // ===================== NOTIFICAÇÕES =====================
                // Qualquer funcionário/admin autenticado vê as próprias notificações
                .requestMatchers("/api/notificacoes/**").hasAnyRole("ADMIN", "FUNCIONARIO")

                // ===================== CARGOS E DEPARTAMENTOS =====================
                // Gestão de cargos e departamentos: ADMIN, RH, GERENTE_OPERACOES
                .requestMatchers("/api/cargos/**").hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES")
                .requestMatchers("/api/departamentos/**").hasAnyRole("ADMIN", "RH", "GERENTE_OPERACOES")

                // ===================== ENDEREÇOS (escrita) =====================
                .requestMatchers("/api/v1/enderecos/**", "/api/v1/endereco").authenticated()

                // Demais rotas exigem apenas estar logado
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**");
    }
}