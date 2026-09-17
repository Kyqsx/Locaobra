package com.locaobra.controller;

import com.locaobra.dto.request.UnidadeEquipamentoRequest;
import com.locaobra.dto.response.UnidadeEquipamentoResponse;
import com.locaobra.enums.StatusUnidade;
import com.locaobra.service.UnidadeEquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UnidadeEquipamentoController {

    private final UnidadeEquipamentoService unidadeService;

    public UnidadeEquipamentoController(UnidadeEquipamentoService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @PostMapping("/api/equipamentos/{equipamentoId}/unidades")
    public ResponseEntity<UnidadeEquipamentoResponse> criar(
            @PathVariable Long equipamentoId,
            @RequestBody UnidadeEquipamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(unidadeService.criar(equipamentoId, request));
    }

    @GetMapping("/api/equipamentos/{equipamentoId}/unidades")
    public ResponseEntity<List<UnidadeEquipamentoResponse>> listarPorEquipamento(
            @PathVariable Long equipamentoId) {
        return ResponseEntity.ok(unidadeService.listarPorEquipamento(equipamentoId));
    }

    @GetMapping("/api/unidades/alertas-manutencao")
    public ResponseEntity<List<UnidadeEquipamentoResponse>> listarAlertasManutencaoPreventiva() {
        return ResponseEntity.ok(unidadeService.listarAlertasManutencaoPreventiva());
    }

    @PutMapping("/api/unidades/{id}")
    public ResponseEntity<UnidadeEquipamentoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody UnidadeEquipamentoRequest request) {
        return ResponseEntity.ok(unidadeService.atualizar(id, request));
    }

    @PatchMapping("/api/unidades/{id}/status")
    public ResponseEntity<UnidadeEquipamentoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        StatusUnidade status = StatusUnidade.valueOf(body.get("status"));
        return ResponseEntity.ok(unidadeService.atualizarStatus(id, status));
    }

    @DeleteMapping("/api/unidades/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        unidadeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
