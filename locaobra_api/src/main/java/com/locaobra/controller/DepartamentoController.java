package com.locaobra.controller;

import com.locaobra.dto.request.DepartamentoRequest;
import com.locaobra.dto.response.DepartamentoResponse;
import com.locaobra.service.DepartamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    public DepartamentoController(DepartamentoService departamentoService) {
        this.departamentoService = departamentoService;
    }

    @PostMapping
    public ResponseEntity<DepartamentoResponse> criar(@Valid @RequestBody DepartamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departamentoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<DepartamentoResponse>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(departamentoService.listarAtivos());
        }
        return ResponseEntity.ok(departamentoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(departamentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody DepartamentoRequest request) {
        return ResponseEntity.ok(departamentoService.atualizar(id, request));
    }
}