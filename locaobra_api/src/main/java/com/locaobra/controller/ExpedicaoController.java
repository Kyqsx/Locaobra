package com.locaobra.controller;

import com.locaobra.dto.request.ExpedicaoRequest;
import com.locaobra.dto.response.ExpedicaoRastreioResponse;
import com.locaobra.dto.response.ExpedicaoResponse;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.exception.BusinessException;
import com.locaobra.service.ExpedicaoService;
import com.locaobra.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expedicoes")
public class ExpedicaoController {

    private final ExpedicaoService expedicaoService;
    private final StorageService storageService;

    public ExpedicaoController(ExpedicaoService expedicaoService, StorageService storageService) {
        this.expedicaoService = expedicaoService;
        this.storageService = storageService;
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

    // ===================== RASTREIO DO CLIENTE (app mobile) =====================
    // Mesmo padrão de "/api/pedidos/meus": o próprio cliente logado acompanha
    // as expedições em que é o destinatário. Retorna a versão enxuta
    // (ExpedicaoRastreioResponse), sem dados internos de operação.

    @GetMapping("/minhas")
    public ResponseEntity<List<ExpedicaoRastreioResponse>> minhas() {
        return ResponseEntity.ok(expedicaoService.listarRastreioDoCliente());
    }

    @GetMapping("/minhas/{id}")
    public ResponseEntity<ExpedicaoRastreioResponse> minhaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(expedicaoService.buscarRastreioDoCliente(id));
    }

    // Chamado a partir da tela "Meus Pedidos": mostra o rastreio da(s)
    // expedição(ões) geradas por aquele pedido (pode ser mais de uma quando
    // os itens saíram de depósitos diferentes).
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<ExpedicaoRastreioResponse>> porPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(expedicaoService.listarRastreioPorPedido(pedidoId));
    }

    // Check-out (EM_TRANSITO) e check-in (CONCLUIDO). O check-out recebe a lista de
    // itens que o conferente marcou na conferência ("itensConferidos": [ids]).
    // ENTREGUE só é alcançado por /confirmar-entrega; CANCELADO por /cancelar.
    @PatchMapping("/{id}/status")
    public ResponseEntity<ExpedicaoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Object bruto = body.get("status");
        StatusExpedicao status;
        try {
            status = StatusExpedicao.valueOf(String.valueOf(bruto));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + bruto);
        }

        List<Long> itensConferidos = new ArrayList<>();
        if (body.get("itensConferidos") instanceof List<?> lista) {
            for (Object o : lista) {
                if (o instanceof Number n) {
                    itensConferidos.add(n.longValue());
                }
            }
        }
        return ResponseEntity.ok(expedicaoService.atualizarStatus(id, status, itensConferidos));
    }

    // Passo 3 do fluxo: o ENTREGADOR confirma a entrega (ou coleta) no local do
    // cliente. Prova exigida: nome + documento de quem assinou, a assinatura
    // DESENHADA (imagem) e a foto; a data/hora é do servidor, nunca vem do front.
    @PostMapping(path = "/{id}/confirmar-entrega", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExpedicaoResponse> confirmarEntrega(
            @PathVariable Long id,
            @RequestPart("assinatura") String assinatura,
            @RequestPart("documento") String documento,
            @RequestPart("assinaturaImagem") MultipartFile assinaturaImagem,
            @RequestPart("foto") MultipartFile foto,
            @RequestPart(name = "observacao", required = false) String observacao) throws IOException {

        exigirImagem(foto, "A foto da entrega precisa ser uma imagem.");
        exigirImagem(assinaturaImagem, "A assinatura precisa ser uma imagem.");

        String fotoUrl = storageService.salvar(foto, "entregas");
        String assinaturaUrl = storageService.salvar(assinaturaImagem, "assinaturas");

        return ResponseEntity.ok(expedicaoService.confirmarEntrega(id, assinatura, documento, assinaturaUrl, fotoUrl, observacao));
    }

    // O entregador chegou e não conseguiu entregar/coletar.
    @PostMapping("/{id}/nao-realizada")
    public ResponseEntity<ExpedicaoResponse> naoRealizada(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(expedicaoService.registrarNaoRealizada(id, body.get("motivo")));
    }

    private void exigirImagem(MultipartFile arquivo, String mensagem) {
        String tipo = arquivo != null ? arquivo.getContentType() : null;
        if (arquivo == null || arquivo.isEmpty() || tipo == null || !tipo.toLowerCase().startsWith("image/")) {
            throw new BusinessException(mensagem);
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        if (motivo == null || motivo.trim().length() < 3) {
            throw new BusinessException("Informe o motivo do cancelamento.");
        }
        expedicaoService.cancelar(id, motivo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        expedicaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}