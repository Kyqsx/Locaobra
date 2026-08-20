package com.locaobra.controller;

import com.locaobra.dto.request.EquipamentoRequest;
import com.locaobra.dto.response.EquipamentoResponse;
import com.locaobra.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipamentoResponse> criarComArquivos(
            @RequestPart("equipamento") String equipamentoJson,
            @RequestPart(name = "imagens", required = false) MultipartFile[] imagens) throws IOException {
        EquipamentoRequest request = objectMapper.readValue(equipamentoJson, EquipamentoRequest.class);

        if (imagens != null && imagens.length > 0) {
            List<String> urls = new ArrayList<>();

            // caminho absoluto — evita o problema do /tmp/tomcat
            String basePath = System.getProperty("user.dir") + "/uploads/equipamentos";
            File baseDir = new File(basePath);
            if (!baseDir.exists())
                baseDir.mkdirs();

            for (MultipartFile f : imagens) {
                if (f == null || f.isEmpty())
                    continue;
                String filename = System.currentTimeMillis() + "_"
                        + f.getOriginalFilename().replaceAll("\\s+", "_");
                File dest = new File(baseDir, filename);
                f.transferTo(dest.getAbsoluteFile()); // .getAbsoluteFile() é o ponto chave
                urls.add("/uploads/equipamentos/" + filename);
            }
            request.setImagens(urls);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(equipamentoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoResponse>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos,
            @RequestParam(required = false) String categoria) {

        if (categoria != null && !categoria.isBlank()) {
            return ResponseEntity.ok(equipamentoService.listarPorCategoria(categoria));
        }
        if (apenasAtivos) {
            return ResponseEntity.ok(equipamentoService.listarAtivos());
        }
        return ResponseEntity.ok(equipamentoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(id));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipamentoResponse> atualizar(
            @PathVariable Long id,
            @RequestPart("equipamento") String equipamentoJson,
            @RequestPart(name = "imagens", required = false) MultipartFile[] imagens) throws IOException {
        EquipamentoRequest request = objectMapper.readValue(equipamentoJson, EquipamentoRequest.class);

        if (imagens != null && imagens.length > 0) {
            List<String> urls = new ArrayList<>();
            String basePath = System.getProperty("user.dir") + "/uploads/equipamentos";
            File baseDir = new File(basePath);
            if (!baseDir.exists())
                baseDir.mkdirs();

            for (MultipartFile f : imagens) {
                if (f == null || f.isEmpty())
                    continue;
                String filename = System.currentTimeMillis() + "_"
                        + f.getOriginalFilename().replaceAll("\\s+", "_");
                File dest = new File(baseDir, filename);
                f.transferTo(dest.getAbsoluteFile());
                urls.add("/uploads/equipamentos/" + filename);
            }
            request.setImagens(urls);
        }

        return ResponseEntity.ok(equipamentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}/imagens")
    public ResponseEntity<Void> deletarImagem(@PathVariable Long id, @RequestParam("url") String url) {
        equipamentoService.deletarImagem(id, url);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/imagens/reorder", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reorderImagens(@PathVariable Long id, @RequestBody List<String> orderedUrls) {
        equipamentoService.reorderImagens(id, orderedUrls);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        equipamentoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        equipamentoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        equipamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
