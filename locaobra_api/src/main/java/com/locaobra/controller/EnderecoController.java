package com.locaobra.controller;

import com.locaobra.dto.request.EnderecoRequest;
import com.locaobra.dto.response.EnderecoResponse;
import com.locaobra.service.ClienteService;
import com.locaobra.service.EnderecoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Duas famílias de endpoints pro mesmo CRUD de endereços:
// - /api/clientes/meus-enderecos      → o próprio cliente logado gerencia os dele
// - /api/clientes/{clienteId}/enderecos → funcionário/admin gerencia os de qualquer cliente
// Ambas delegam pro mesmo EnderecoService, só muda de onde vem o clienteId.
@RestController
@RequestMapping("/api/clientes")
public class EnderecoController {

    private final EnderecoService enderecoService;
    private final ClienteService clienteService;

    public EnderecoController(EnderecoService enderecoService, ClienteService clienteService) {
        this.enderecoService = enderecoService;
        this.clienteService = clienteService;
    }

    // ===================== AUTOATENDIMENTO (cliente logado) =====================

    @GetMapping("/meus-enderecos")
    public ResponseEntity<List<EnderecoResponse>> listarMeus(Authentication authentication) {
        Long clienteId = clienteService.resolverClienteIdLogado(authentication.getName());
        return ResponseEntity.ok(enderecoService.listarPorCliente(clienteId));
    }

    @PostMapping("/meus-enderecos")
    public ResponseEntity<EnderecoResponse> adicionarMeu(Authentication authentication, @Valid @RequestBody EnderecoRequest request) {
        Long clienteId = clienteService.resolverClienteIdLogado(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoService.adicionar(clienteId, request));
    }

    @PutMapping("/meus-enderecos/{enderecoId}")
    public ResponseEntity<EnderecoResponse> atualizarMeu(Authentication authentication, @PathVariable Long enderecoId, @Valid @RequestBody EnderecoRequest request) {
        Long clienteId = clienteService.resolverClienteIdLogado(authentication.getName());
        return ResponseEntity.ok(enderecoService.atualizar(clienteId, enderecoId, request));
    }

    @DeleteMapping("/meus-enderecos/{enderecoId}")
    public ResponseEntity<Void> removerMeu(Authentication authentication, @PathVariable Long enderecoId) {
        Long clienteId = clienteService.resolverClienteIdLogado(authentication.getName());
        enderecoService.remover(clienteId, enderecoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/meus-enderecos/{enderecoId}/principal")
    public ResponseEntity<EnderecoResponse> definirPrincipalMeu(Authentication authentication, @PathVariable Long enderecoId) {
        Long clienteId = clienteService.resolverClienteIdLogado(authentication.getName());
        return ResponseEntity.ok(enderecoService.definirPrincipal(clienteId, enderecoId));
    }

    // ===================== GESTÃO PELA EQUIPE (admin/funcionário) =====================

    @GetMapping("/{clienteId}/enderecos")
    public ResponseEntity<List<EnderecoResponse>> listar(@PathVariable Long clienteId) {
        return ResponseEntity.ok(enderecoService.listarPorCliente(clienteId));
    }

    @PostMapping("/{clienteId}/enderecos")
    public ResponseEntity<EnderecoResponse> adicionar(@PathVariable Long clienteId, @Valid @RequestBody EnderecoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoService.adicionar(clienteId, request));
    }

    @PutMapping("/{clienteId}/enderecos/{enderecoId}")
    public ResponseEntity<EnderecoResponse> atualizar(@PathVariable Long clienteId, @PathVariable Long enderecoId, @Valid @RequestBody EnderecoRequest request) {
        return ResponseEntity.ok(enderecoService.atualizar(clienteId, enderecoId, request));
    }

    @DeleteMapping("/{clienteId}/enderecos/{enderecoId}")
    public ResponseEntity<Void> remover(@PathVariable Long clienteId, @PathVariable Long enderecoId) {
        enderecoService.remover(clienteId, enderecoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{clienteId}/enderecos/{enderecoId}/principal")
    public ResponseEntity<EnderecoResponse> definirPrincipal(@PathVariable Long clienteId, @PathVariable Long enderecoId) {
        return ResponseEntity.ok(enderecoService.definirPrincipal(clienteId, enderecoId));
    }
}
