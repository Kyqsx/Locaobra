package com.locaobra.controller;

import com.locaobra.dto.request.VistoriaRequest;
import com.locaobra.dto.response.VistoriaResponse;
import com.locaobra.service.VistoriaService;
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
@RequestMapping("/api/expedicoes/{expedicaoId}/vistorias")
public class VistoriaController {

    private final VistoriaService vistoriaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VistoriaController(VistoriaService vistoriaService) {
        this.vistoriaService = vistoriaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VistoriaResponse> criar(
            @PathVariable Long expedicaoId,
            @RequestPart("vistoria") String vistoriaJson,
            @RequestPart(name = "fotos", required = false) MultipartFile[] fotos) throws IOException {
        VistoriaRequest request = objectMapper.readValue(vistoriaJson, VistoriaRequest.class);

        if (fotos != null && fotos.length > 0) {
            List<String> urls = new ArrayList<>();

            String basePath = System.getProperty("user.dir") + "/uploads/vistorias";
            File baseDir = new File(basePath);
            if (!baseDir.exists()) baseDir.mkdirs();

            for (MultipartFile f : fotos) {
                if (f == null || f.isEmpty()) continue;
                String filename = System.currentTimeMillis() + "_"
                        + f.getOriginalFilename().replaceAll("\\s+", "_");
                File dest = new File(baseDir, filename);
                f.transferTo(dest.getAbsoluteFile());
                urls.add("/uploads/vistorias/" + filename);
            }
            request.setFotos(urls);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vistoriaService.criar(expedicaoId, request));
    }

    @GetMapping
    public ResponseEntity<List<VistoriaResponse>> listarPorExpedicao(@PathVariable Long expedicaoId) {
        return ResponseEntity.ok(vistoriaService.listarPorExpedicao(expedicaoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VistoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vistoriaService.buscarPorId(id));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VistoriaResponse> atualizar(
            @PathVariable Long id,
            @RequestPart("vistoria") String vistoriaJson,
            @RequestPart(name = "fotos", required = false) MultipartFile[] fotos) throws IOException {
        VistoriaRequest request = objectMapper.readValue(vistoriaJson, VistoriaRequest.class);

        if (fotos != null && fotos.length > 0) {
            List<String> urls = new ArrayList<>();
            String basePath = System.getProperty("user.dir") + "/uploads/vistorias";
            File baseDir = new File(basePath);
            if (!baseDir.exists()) baseDir.mkdirs();

            for (MultipartFile f : fotos) {
                if (f == null || f.isEmpty()) continue;
                String filename = System.currentTimeMillis() + "_"
                        + f.getOriginalFilename().replaceAll("\\s+", "_");
                File dest = new File(baseDir, filename);
                f.transferTo(dest.getAbsoluteFile());
                urls.add("/uploads/vistorias/" + filename);
            }
            request.setFotos(urls);
        }

        return ResponseEntity.ok(vistoriaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        vistoriaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}