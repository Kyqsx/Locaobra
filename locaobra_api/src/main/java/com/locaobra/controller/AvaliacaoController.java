package com.locaobra.controller;

import com.locaobra.dto.request.AvaliacaoRequest;
import com.locaobra.dto.response.AvaliacaoResponse;
import com.locaobra.dto.response.AvaliacoesEquipamentoResponse;
import com.locaobra.dto.response.MinhaAvaliacaoResponse;
import com.locaobra.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    // Público: resumo (média, distribuição) + lista de avaliações do equipamento.
    @GetMapping("/equipamento/{equipamentoId}")
    public ResponseEntity<AvaliacoesEquipamentoResponse> listarPorEquipamento(@PathVariable Long equipamentoId) {
        return ResponseEntity.ok(avaliacaoService.listarPorEquipamento(equipamentoId));
    }

    // Cliente logado: já avaliou? pode avaliar?
    @GetMapping("/equipamento/{equipamentoId}/minha")
    public ResponseEntity<MinhaAvaliacaoResponse> minhaSituacao(@PathVariable Long equipamentoId) {
        return ResponseEntity.ok(avaliacaoService.minhaSituacao(equipamentoId));
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.ok(avaliacaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        avaliacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
