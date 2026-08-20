package com.locaobra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Armazenamento de imagens de upload.
 *
 * Se o Supabase Storage estiver configurado (supabase.url + supabase.service-key),
 * os arquivos são enviados para o bucket "uploads" e devolve-se a URL pública.
 * Caso contrário (dev local sem config), cai no disco local em <user.dir>/uploads.
 *
 * As URLs públicas do Supabase começam com https://... e o front (imageUrl) já
 * devolve essas URLs como estão, então nada muda na tela.
 */
@Service
public class StorageService {

    private final String supabaseUrl;
    private final String supabaseServiceKey;
    private final String bucket;
    private final boolean supabaseAtivo;
    private final HttpClient httpClient;

    public StorageService(
            @Value("${supabase.apiUrl:}") String supabaseUrl,
            @Value("${supabase.service-key:}") String supabaseServiceKey,
            @Value("${supabase.storage.bucket:uploads}") String bucket) {
        this.supabaseUrl = trim(supabaseUrl);
        this.supabaseServiceKey = trim(supabaseServiceKey);
        this.bucket = trim(bucket);
        this.supabaseAtivo = !this.supabaseUrl.isEmpty() && !this.supabaseServiceKey.isEmpty();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public boolean isSupabaseAtivo() {
        return supabaseAtivo;
    }

    /**
     * Sobe o arquivo para a pasta informada e devolve a URL acessível.
     */
    public String salvar(MultipartFile file, String pasta) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String nome = System.currentTimeMillis() + "_"
                + file.getOriginalFilename().replaceAll("\\s+", "_");
        if (supabaseAtivo) {
            return uploadSupabase(file, pasta + "/" + nome);
        }
        return salvarLocal(file, pasta, nome);
    }

    /** Remove o arquivo pelo seu URL. Suporta Supabase https e caminho local. */
    public void remover(String url) {
        if (url == null || url.isBlank()) return;
        if (supabaseAtivo && url.startsWith("https://")) {
            removerSupabase(url);
        } else {
            removerLocal(url);
        }
    }

    // ------------------------------------------------------------------
    // Supabase Storage
    // ------------------------------------------------------------------
    private String uploadSupabase(MultipartFile file, String path) throws IOException {
        String objectUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + encode(path);
        HttpRequest req = HttpRequest.newBuilder(URI.create(objectUrl))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .header("x-upsert", "true")
                .header("Content-Type", mediaType(file))
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new IOException("Falha ao enviar ao Supabase Storage (HTTP "
                        + resp.statusCode() + "): " + resp.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload ao Supabase interrompido", e);
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encode(path);
    }
private void removerSupabase(String url) {
        int idx = url.indexOf("/storage/v1/object/public/");
        if (idx < 0) return;
        String resto = url.substring(idx + "/storage/v1/object/public/".length());
        int barra = resto.indexOf('/');
        if (barra < 0) return;
        String path = resto.substring(barra + 1); // remove <bucket>/
        if (path.isEmpty()) return;

        HttpRequest req = HttpRequest.newBuilder(URI.create(
                        supabaseUrl + "/storage/v1/object/" + bucket + "/" + encode(path)))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .DELETE()
                .build();
        try {
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
            // falha de remoção no storage não deve quebrar a operação principal
        }
    }

    private static String encode(String path) {
        // Apenas cada segmento é codificado, mantendo as "/" entre pastas.
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (sb.length() > 0) sb.append('/');
            sb.append(URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    private static String mediaType(MultipartFile file) {
        String t = file.getContentType();
        return (t != null && !t.isBlank()) ? t : "application/octet-stream";
    }

    // ------------------------------------------------------------------
    // Fallback: disco local
    // ------------------------------------------------------------------
    private String salvarLocal(MultipartFile file, String pasta, String nome) throws IOException {
        File baseDir = new File(System.getProperty("user.dir") + "/uploads/" + pasta);
        if (!baseDir.exists()) baseDir.mkdirs();
        File dest = new File(baseDir, nome);
        file.transferTo(dest.getAbsoluteFile());
        return "/uploads/" + pasta + "/" + nome;
    }

    private void removerLocal(String url) {
        try {
            String base = System.getProperty("user.dir") + "/uploads";
            File f = new File(base, url.replace("/uploads/", ""));
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }
}