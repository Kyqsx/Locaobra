package com.locaobra.controller;

import com.locaobra.dto.request.ArtigoRequest;
import com.locaobra.dto.response.ArtigoResponse;
import com.locaobra.service.ArtigoService;
import com.locaobra.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/artigos")
public class ArtigoController {

    private final ArtigoService artigoService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArtigoController(ArtigoService artigoService, StorageService storageService) {
        this.artigoService = artigoService;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArtigoResponse> criarComImagem(
            @RequestPart("artigo") String artigoJson,
            @RequestPart(name = "imagemCapa", required = false) MultipartFile imagemCapa) throws IOException {
        ArtigoRequest request = objectMapper.readValue(artigoJson, ArtigoRequest.class);

        if (imagemCapa != null && !imagemCapa.isEmpty()) {
            request.setImagemCapa(storageService.salvar(imagemCapa, "artigos"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(artigoService.criar(request));
    }

    // Criação sem imagem (JSON puro) — útil para integrações que não sobem arquivo.
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArtigoResponse> criar(@Valid @RequestBody ArtigoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artigoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<ArtigoResponse>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasPublicados) {
        if (apenasPublicados) {
            return ResponseEntity.ok(artigoService.listarPublicados());
        }
        return ResponseEntity.ok(artigoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtigoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(artigoService.buscarPorId(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArtigoResponse> buscarPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(artigoService.buscarPorSlug(slug));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArtigoResponse> atualizarComImagem(
            @PathVariable Long id,
            @RequestPart("artigo") String artigoJson,
            @RequestPart(name = "imagemCapa", required = false) MultipartFile imagemCapa) throws IOException {
        ArtigoRequest request = objectMapper.readValue(artigoJson, ArtigoRequest.class);

        if (imagemCapa != null && !imagemCapa.isEmpty()) {
            request.setImagemCapa(storageService.salvar(imagemCapa, "artigos"));
        }

        return ResponseEntity.ok(artigoService.atualizar(id, request));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArtigoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ArtigoRequest request) {
        return ResponseEntity.ok(artigoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        artigoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
