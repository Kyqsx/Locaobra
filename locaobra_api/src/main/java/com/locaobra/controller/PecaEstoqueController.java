package com.locaobra.controller;

import com.locaobra.dto.request.PecaEstoqueRequest;
import com.locaobra.dto.response.PecaEstoqueResponse;
import com.locaobra.service.PecaEstoqueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pecas")
public class PecaEstoqueController {

    private final PecaEstoqueService pecaService;

    public PecaEstoqueController(PecaEstoqueService pecaService) {
        this.pecaService = pecaService;
    }

    @PostMapping
    public ResponseEntity<PecaEstoqueResponse> criar(@RequestBody PecaEstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<PecaEstoqueResponse>> listar() {
        return ResponseEntity.ok(pecaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaEstoqueResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PecaEstoqueResponse> atualizar(
            @PathVariable Long id, @RequestBody PecaEstoqueRequest request) {
        return ResponseEntity.ok(pecaService.atualizar(id, request));
    }

    @PatchMapping("/{id}/entrada")
    public ResponseEntity<PecaEstoqueResponse> entradaEstoque(
            @PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(pecaService.entradaEstoque(id, body.get("quantidade")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
