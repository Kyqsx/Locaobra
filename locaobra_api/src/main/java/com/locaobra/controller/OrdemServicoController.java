package com.locaobra.controller;

import com.locaobra.dto.request.DiagnosticoRequest;
import com.locaobra.dto.request.ItemOrdemServicoRequest;
import com.locaobra.dto.request.OrdemServicoRequest;
import com.locaobra.dto.response.OrdemServicoResponse;
import com.locaobra.enums.StatusOrdemServico;
import com.locaobra.service.OrdemServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordens-servico")
public class OrdemServicoController {

    private final OrdemServicoService osService;

    public OrdemServicoController(OrdemServicoService osService) {
        this.osService = osService;
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponse> abrir(@RequestBody OrdemServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(osService.abrir(request));
    }

    @GetMapping
    public ResponseEntity<List<OrdemServicoResponse>> listar(
            @RequestParam(required = false) StatusOrdemServico status) {
        return ResponseEntity.ok(osService.listar(status));
    }

    @GetMapping("/aguardando-manutencao")
    public ResponseEntity<List<OrdemServicoResponse>> listarAguardandoManutencao() {
        return ResponseEntity.ok(osService.listarAguardandoManutencao());
    }

    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorUnidade(@PathVariable Long unidadeId) {
        return ResponseEntity.ok(osService.listarPorUnidade(unidadeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(osService.buscarPorId(id));
    }

    @PatchMapping("/{id}/diagnostico")
    public ResponseEntity<OrdemServicoResponse> atualizarDiagnostico(
            @PathVariable Long id, @RequestBody DiagnosticoRequest request) {
        return ResponseEntity.ok(osService.atualizarDiagnostico(id, request));
    }

    @PostMapping("/{id}/itens")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(
            @PathVariable Long id, @RequestBody ItemOrdemServicoRequest request) {
        return ResponseEntity.ok(osService.adicionarPeca(id, request));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    public ResponseEntity<OrdemServicoResponse> removerPeca(
            @PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(osService.removerPeca(id, itemId));
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<OrdemServicoResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(osService.concluir(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<OrdemServicoResponse> cancelar(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(osService.cancelar(id, motivo));
    }
}
