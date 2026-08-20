package com.locaobra.controller;

import com.locaobra.dto.request.CargoRequest;
import com.locaobra.dto.response.CargoResponse;
import com.locaobra.service.CargoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @PostMapping
    public ResponseEntity<CargoResponse> criar(@Valid @RequestBody CargoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cargoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<CargoResponse>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(cargoService.listarAtivos());
        }
        return ResponseEntity.ok(cargoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CargoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cargoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CargoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CargoRequest request) {
        return ResponseEntity.ok(cargoService.atualizar(id, request));
    }
}