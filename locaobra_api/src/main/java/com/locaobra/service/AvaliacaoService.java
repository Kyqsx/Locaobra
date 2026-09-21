package com.locaobra.service;

import com.locaobra.dto.request.AvaliacaoRequest;
import com.locaobra.dto.response.AvaliacaoResponse;
import com.locaobra.dto.response.AvaliacoesEquipamentoResponse;
import com.locaobra.dto.response.MinhaAvaliacaoResponse;
import com.locaobra.entity.Avaliacao;
import com.locaobra.entity.Cliente;
import com.locaobra.entity.Equipamento;
import com.locaobra.entity.Usuario;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.AvaliacaoRepository;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.EquipamentoRepository;
import com.locaobra.repository.ItemExpedicaoRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AvaliacaoService {

    private static final String MOTIVO_NAO_RECEBEU =
            "Você poderá avaliar este equipamento depois que a locação terminar e ele for devolvido ao depósito.";

    private final AvaliacaoRepository avaliacaoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemExpedicaoRepository itemExpedicaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            EquipamentoRepository equipamentoRepository,
                            ClienteRepository clienteRepository,
                            UsuarioRepository usuarioRepository,
                            ItemExpedicaoRepository itemExpedicaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.itemExpedicaoRepository = itemExpedicaoRepository;
    }

    // ===================== LEITURA PÚBLICA =====================

    @Transactional(readOnly = true)
    public AvaliacoesEquipamentoResponse listarPorEquipamento(Long equipamentoId) {
        if (!equipamentoRepository.existsById(equipamentoId)) {
            throw new ResourceNotFoundException("Equipamento não encontrado: " + equipamentoId);
        }

        List<AvaliacaoResponse> avaliacoes = avaliacaoRepository
                .findByEquipamentoIdOrderByCriadoEmDesc(equipamentoId)
                .stream()
                .map(AvaliacaoResponse::from)
                .collect(Collectors.toList());

        // Sempre devolve as 5 chaves (mesmo com 0), na ordem 5 -> 1.
        Map<Integer, Long> distribuicao = new LinkedHashMap<>();
        for (int nota = 5; nota >= 1; nota--) distribuicao.put(nota, 0L);
        for (Object[] linha : avaliacaoRepository.distribuicaoPorEquipamento(equipamentoId)) {
            distribuicao.put(((Number) linha[0]).intValue(), ((Number) linha[1]).longValue());
        }

        long total = avaliacoes.size();
        Double media = total == 0 ? null : arredondar(avaliacaoRepository.mediaPorEquipamento(equipamentoId));

        return new AvaliacoesEquipamentoResponse(media, total, distribuicao, avaliacoes);
    }

    // ===================== CLIENTE LOGADO =====================

    @Transactional(readOnly = true)
    public MinhaAvaliacaoResponse minhaSituacao(Long equipamentoId) {
        Cliente cliente = resolverClienteLogado();
        if (!equipamentoRepository.existsById(equipamentoId)) {
            throw new ResourceNotFoundException("Equipamento não encontrado: " + equipamentoId);
        }

        AvaliacaoResponse existente = avaliacaoRepository
                .findByClienteIdAndEquipamentoId(cliente.getId(), equipamentoId)
                .map(AvaliacaoResponse::from)
                .orElse(null);

        // Quem já avaliou sempre pode editar; quem não avaliou precisa ter recebido o equipamento.
        if (existente != null) {
            return new MinhaAvaliacaoResponse(true, null, existente);
        }
        if (!devolveuEquipamento(cliente.getId(), equipamentoId)) {
            return new MinhaAvaliacaoResponse(false, MOTIVO_NAO_RECEBEU, null);
        }
        return new MinhaAvaliacaoResponse(true, null, null);
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        if (request.getEquipamentoId() == null) {
            throw new BusinessException("Informe o equipamento a ser avaliado.");
        }
        Cliente cliente = resolverClienteLogado();
        Equipamento equipamento = equipamentoRepository.findById(request.getEquipamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + request.getEquipamentoId()));

        if (avaliacaoRepository.existsByClienteIdAndEquipamentoId(cliente.getId(), equipamento.getId())) {
            throw new BusinessException("Você já avaliou este equipamento. Edite a avaliação existente.");
        }
        if (!devolveuEquipamento(cliente.getId(), equipamento.getId())) {
            throw new BusinessException(MOTIVO_NAO_RECEBEU);
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setEquipamento(equipamento);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(request.getNota());
        avaliacao.setComentario(normalizarComentario(request.getComentario()));

        return AvaliacaoResponse.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public AvaliacaoResponse atualizar(Long id, AvaliacaoRequest request) {
        Cliente cliente = resolverClienteLogado();
        Avaliacao avaliacao = buscarEntidade(id);

        if (!avaliacao.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("Você só pode editar as suas próprias avaliações.");
        }

        avaliacao.setNota(request.getNota());
        avaliacao.setComentario(normalizarComentario(request.getComentario()));

        return AvaliacaoResponse.from(avaliacaoRepository.save(avaliacao));
    }

    // Cliente exclui a própria avaliação; ADMIN / GERENTE_OPERACOES (moderação)
    // podem excluir qualquer uma — quem pode chamar já é filtrado no SecurityConfig.
    @Transactional
    public void excluir(Long id) {
        Avaliacao avaliacao = buscarEntidade(id);

        if (logadoEhCliente()) {
            Cliente cliente = resolverClienteLogado();
            if (!avaliacao.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("Você só pode excluir as suas próprias avaliações.");
            }
        }
        avaliacaoRepository.delete(avaliacao);
    }

    // ===================== HELPERS =====================

    private boolean devolveuEquipamento(Long clienteId, Long equipamentoId) {
        return itemExpedicaoRepository.contarColetasConcluidas(clienteId, equipamentoId) > 0;
    }

    private Avaliacao buscarEntidade(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada: " + id));
    }

    private String normalizarComentario(String comentario) {
        if (comentario == null) return null;
        String limpo = comentario.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static double arredondar(Double valor) {
        if (valor == null) return 0;
        return Math.round(valor * 10.0) / 10.0;
    }

    private boolean logadoEhCliente() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
    }

    private String emailLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return auth.getPrincipal().toString();
    }

    private Cliente resolverClienteLogado() {
        Usuario usuario = usuarioRepository.findByEmail(emailLogado())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (usuario.getIdCliente() == null) {
            throw new BusinessException("Esse usuário não está vinculado a um cadastro de cliente.");
        }
        return clienteRepository.findById(usuario.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }
}
