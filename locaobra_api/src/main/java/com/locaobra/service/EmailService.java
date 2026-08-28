package com.locaobra.service;

import com.locaobra.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envio de emails via API REST do Resend (https://resend.com).
 *
 * Configuração (application.properties / variáveis de ambiente):
 *  - RESEND_API_KEY : chave da API (re_.xxx) — obtida no painel do Resend
 *  - MAIL_FROM      : remetente, ex.: "Locaobra <nao-responda@seudominio.com>"
 *                     (o domínio precisa estar verificado no Resend; para testes
 *                     pode usar onboarding@resend.dev)
 *  - FRONTEND_URL   : base para montar o link de verificação
 *
 * Sem RESEND_API_KEY configurada, o link é apenas LOGADO no console — assim
 * o desenvolvimento local continua funcionando sem conta no Resend.
 */
@Service
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${app.mail.from:Locaobra <onboarding@resend.dev>}")
    private String from;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void enviarEmailVerificacao(Usuario usuario, String token) {
        String link = frontendUrl + "/verificar-email?token=" + token;

        String assunto = "Confirme seu email — Locaobra";
        String corpo = "Olá, " + usuario.getNome() + "!\n\n"
                + "Bem-vindo(a) à Locaobra. Para ativar sua conta, confirme seu email:\n\n"
                + link + "\n\n"
                + "O link é válido por 24 horas. Se você não criou esta conta, ignore este email.\n\n"
                + "Equipe Locaobra";

        if (apiKey == null || apiKey.isBlank()) {
            // Fallback de desenvolvimento: sem chave do Resend, só registra no log
            System.out.println("[EmailService] RESEND_API_KEY não configurada — link de verificação para "
                    + usuario.getEmail() + ": " + link);
            return;
        }

        enviarResend(usuario.getEmail(), assunto, corpo);
    }

    private void enviarResend(String para, String assunto, String texto) {
        // JSON montado manualmente (texto simples, sem caracteres problemáticos)
        String payload = "{"
                + "\"from\":\"" + escapeJson(from) + "\","
                + "\"to\":[\"" + escapeJson(para) + "\"],"
                + "\"subject\":\"" + escapeJson(assunto) + "\","
                + "\"text\":\"" + escapeJson(texto) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(RESEND_API_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                System.err.println("[EmailService] Falha ao enviar via Resend (HTTP "
                        + resp.statusCode() + "): " + resp.body());
                throw new RuntimeException("Falha ao enviar email de verificação. Tente novamente.");
            }
        } catch (IOException e) {
            System.err.println("[EmailService] Erro de I/O chamando a API do Resend: " + e.getMessage());
            throw new RuntimeException("Falha ao enviar email de verificação. Tente novamente.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Envio de email interrompido.", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

