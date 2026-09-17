package com.locaobra.controller;

import com.locaobra.dto.request.DepositoRequest;
import com.locaobra.dto.response.DepositoResponse;
import com.locaobra.service.DepositoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depositos")
public class DepositoController {

    private final DepositoService depositoService;

    public DepositoController(DepositoService depositoService) {
        this.depositoService = depositoService;
    }

    @PostMapping
    public ResponseEntity<DepositoResponse> criar(@RequestBody DepositoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<DepositoResponse>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(depositoService.listarAtivos());
        }
        return ResponseEntity.ok(depositoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(depositoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepositoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody DepositoRequest request) {
        return ResponseEntity.ok(depositoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        depositoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
