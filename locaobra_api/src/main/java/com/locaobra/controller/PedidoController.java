package com.locaobra.controller;

import com.locaobra.dto.request.ConfirmarPedidoRequest;
import com.locaobra.dto.request.PedidoDecisaoRequest;
import com.locaobra.dto.request.PedidoRequest;
import com.locaobra.dto.response.PedidoResponse;
import com.locaobra.dto.response.SugestaoAlocacaoResponse;
import com.locaobra.enums.StatusPedido;
import com.locaobra.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Cliente solicita um orçamento pelo catálogo.
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(request));
    }

    // "Meus pedidos" do cliente logado.
    @GetMapping("/meus")
    public ResponseEntity<List<PedidoResponse>> listarMeus() {
        return ResponseEntity.ok(pedidoService.listarMeus());
    }

    // Fila do consultor: pedidos SOLICITADO aguardando revisão.
    @GetMapping("/fila-consultor")
    public ResponseEntity<List<PedidoResponse>> listarFilaConsultor() {
        return ResponseEntity.ok(pedidoService.listarFilaConsultor());
    }

    // Fila do analista de credenciamento: pedidos CONFIRMADO aguardando crédito.
    @GetMapping("/fila-credito")
    public ResponseEntity<List<PedidoResponse>> listarFilaCredito() {
        return ResponseEntity.ok(pedidoService.listarFilaCredito());
    }

    // Fila do conferente: pedidos APROVADO ainda sem expedição gerada.
    @GetMapping("/fila-conferente")
    public ResponseEntity<List<PedidoResponse>> listarFilaConferente() {
        return ResponseEntity.ok(pedidoService.listarFilaConferente());
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(@RequestParam(required = false) StatusPedido status) {
        if (status != null) {
            return ResponseEntity.ok(pedidoService.listarPorStatus(status));
        }
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    // Sugestão de depósito(s) pra atender esse pedido — o consultor usa isso
    // antes de confirmar (ver front: modal de confirmação em Admin/pedidos.jsx).
    @GetMapping("/{id}/sugestao-depositos")
    public ResponseEntity<SugestaoAlocacaoResponse> sugerirAlocacaoDepositos(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.sugerirAlocacaoDepositos(id));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponse> confirmar(@PathVariable Long id, @RequestBody ConfirmarPedidoRequest request) {
        return ResponseEntity.ok(pedidoService.confirmar(id, request));
    }

    @PatchMapping("/{id}/recusar")
    public ResponseEntity<PedidoResponse> recusar(@PathVariable Long id, @RequestBody PedidoDecisaoRequest request) {
        return ResponseEntity.ok(pedidoService.recusar(id, request));
    }

    @PatchMapping("/{id}/aprovar-credito")
    public ResponseEntity<PedidoResponse> aprovarCredito(@PathVariable Long id, @RequestBody(required = false) PedidoDecisaoRequest request) {
        return ResponseEntity.ok(pedidoService.aprovarCredito(id, request));
    }

    @PatchMapping("/{id}/reprovar-credito")
    public ResponseEntity<PedidoResponse> reprovarCredito(@PathVariable Long id, @RequestBody PedidoDecisaoRequest request) {
        return ResponseEntity.ok(pedidoService.reprovarCredito(id, request));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelar(id));
    }
}
