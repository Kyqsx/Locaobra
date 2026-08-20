package com.locaobra.controller;

import com.locaobra.dto.request.ExpedicaoRequest;
import com.locaobra.dto.response.ExpedicaoResponse;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.service.ExpedicaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expedicoes")
public class ExpedicaoController {

    private final ExpedicaoService expedicaoService;

    public ExpedicaoController(ExpedicaoService expedicaoService) {
        this.expedicaoService = expedicaoService;
    }

    @PostMapping
    public ResponseEntity<ExpedicaoResponse> criar(@Valid @RequestBody ExpedicaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expedicaoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<ExpedicaoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusExpedicao status) {

        if (data != null) {
            return ResponseEntity.ok(expedicaoService.listarPorData(data));
        }
        if (inicio != null && fim != null) {
            return ResponseEntity.ok(expedicaoService.listarPorPeriodo(inicio, fim));
        }
        if (status != null) {
            return ResponseEntity.ok(expedicaoService.listarPorStatus(status));
        }
        return ResponseEntity.ok(expedicaoService.listarTodos());
    }

    // Alimenta o select de "qual entrega vou buscar" na tela de nova
    // expedição do tipo COLETA: só entregas CONCLUIDO sem coleta ativa ainda.
    @GetMapping("/entregas-para-coleta")
    public ResponseEntity<List<ExpedicaoResponse>> listarEntregasParaColeta() {
        return ResponseEntity.ok(expedicaoService.listarEntregasParaColeta());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpedicaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(expedicaoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ExpedicaoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        StatusExpedicao status = StatusExpedicao.valueOf(body.get("status"));
        return ResponseEntity.ok(expedicaoService.atualizarStatus(id, status));
    }

    @PatchMapping("/{id}/assinatura")
    public ResponseEntity<Void> atualizarAssinatura(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        expedicaoService.atualizarAssinatura(id, body.get("assinatura"));
        return ResponseEntity.noContent().build();
    }

    // Passo 3 do fluxo: o ENTREGADOR confirma a entrega no local do cliente,
    // entre o check-out (passo 2) e o check-in (passo 4). Assinatura de quem
    // recebeu + foto do equipamento entregue; a data/hora fica por conta do
    // servidor (LocalDateTime.now() no service), nunca vem do front.
    @PostMapping(path = "/{id}/confirmar-entrega", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExpedicaoResponse> confirmarEntrega(
            @PathVariable Long id,
            @RequestPart("assinatura") String assinatura,
            @RequestPart("foto") MultipartFile foto) throws IOException {

        String basePath = System.getProperty("user.dir") + "/uploads/entregas";
        File baseDir = new File(basePath);
        if (!baseDir.exists()) baseDir.mkdirs();

        String filename = System.currentTimeMillis() + "_" + foto.getOriginalFilename().replaceAll("\\s+", "_");
        File dest = new File(baseDir, filename);
        foto.transferTo(dest.getAbsoluteFile());
        String fotoUrl = "/uploads/entregas/" + filename;

        return ResponseEntity.ok(expedicaoService.confirmarEntrega(id, assinatura, fotoUrl));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        expedicaoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        expedicaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}