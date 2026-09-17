package com.locaobra.controller;

import com.locaobra.dto.response.NotificacaoResponse;
import com.locaobra.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/api/notificacoes")
    public ResponseEntity<List<NotificacaoResponse>> listar(
            @RequestParam(required = false) String destinatario,
            @RequestParam(required = false) Boolean naoLidas) {
        if (naoLidas != null && naoLidas) {
            return ResponseEntity.ok(notificacaoService.listarNaoLidas(destinatario));
        }
        return ResponseEntity.ok(notificacaoService.listarPorDestinatario(destinatario));
    }

    @GetMapping("/api/notificacoes/nao-lidas")
    public ResponseEntity<Long> contarNaoLidas(@RequestParam String destinatario) {
        return ResponseEntity.ok(notificacaoService.contarNaoLidas(destinatario));
    }

    @PatchMapping("/api/notificacoes/{id}/lida")
    public ResponseEntity<Void> marcarLida(@PathVariable Long id) {
        notificacaoService.marcarLida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/notificacoes/marcar-todas-lidas")
    public ResponseEntity<Void> marcarTodasLidas(@RequestBody Map<String, String> body) {
        notificacaoService.marcarTodasLidas(body.get("destinatario"));
        return ResponseEntity.noContent().build();
    }
}