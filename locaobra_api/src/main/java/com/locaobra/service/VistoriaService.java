package com.locaobra.service;

import com.locaobra.dto.request.VistoriaRequest;
import com.locaobra.dto.response.VistoriaResponse;
import com.locaobra.entity.Expedicao;
import com.locaobra.entity.FotoVistoria;
import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.entity.Vistoria;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ExpedicaoRepository;
import com.locaobra.repository.FotoVistoriaRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import com.locaobra.repository.VistoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final ExpedicaoRepository expedicaoRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final FotoVistoriaRepository fotoRepository;
    private final NotificacaoService notificacaoService;

    public VistoriaService(
            VistoriaRepository vistoriaRepository,
            ExpedicaoRepository expedicaoRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            FotoVistoriaRepository fotoRepository,
            NotificacaoService notificacaoService) {
        this.vistoriaRepository = vistoriaRepository;
        this.expedicaoRepository = expedicaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.fotoRepository = fotoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public VistoriaResponse criar(Long expedicaoId, VistoriaRequest request) {
        Expedicao expedicao = expedicaoRepository.findById(expedicaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Expedição não encontrada: " + expedicaoId));

        if (request.getTipo() == null) {
            throw new BusinessException("Tipo de vistoria é obrigatório (ENTREGA ou DEVOLUCAO)");
        }

        // Regra de negócio: tanto a vistoria de pré-saída (ENTREGA) quanto a
        // de devolução são feitas pelo TÉCNICO DE MANUTENÇÃO — é quem tem o
        // olho técnico pra avaliar o estado do equipamento antes de sair e
        // quando ele volta. A Ordem de Serviço, feita depois (também pelo
        // técnico), é onde a decisão de reparo de fato acontece.
        if (request.getTipo() == com.locaobra.enums.TipoVistoria.ENTREGA) {
            exigirCargo("TECNICO_MANUTENCAO", "Somente o técnico de manutenção pode registrar a vistoria de entrega (pré-saída).");
        } else {
            exigirCargo("TECNICO_MANUTENCAO", "Somente o técnico de manutenção pode registrar a vistoria de devolução.");
        }

        if (request.getUnidadeId() == null) {
            throw new BusinessException("Unidade é obrigatória para a vistoria");
        }

        UnidadeEquipamento unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + request.getUnidadeId()));

        boolean jaExiste = vistoriaRepository.existsByExpedicaoIdAndUnidadeIdAndTipo(
                expedicaoId, request.getUnidadeId(), request.getTipo());
        if (jaExiste) {
            throw new BusinessException("Já existe vistoria de " + request.getTipo() + " para esta unidade nesta expedição");
        }

        Vistoria vistoria = new Vistoria();
        vistoria.setExpedicao(expedicao);
        vistoria.setUnidade(unidade);
        vistoria.setTipo(request.getTipo());
        vistoria.setCondicaoGeral(request.getCondicaoGeral());
        vistoria.setAvariasExistentes(request.getAvariasExistentes());
        vistoria.setDanosCausados(request.getDanosCausados());
        vistoria.setObservacoes(request.getObservacoes());
        vistoria.setRealizadaEm(LocalDateTime.now());

        vistoria = vistoriaRepository.save(vistoria);

        // Salva fotos da vistoria
        if (request.getFotos() != null && !request.getFotos().isEmpty()) {
            for (String url : request.getFotos()) {
                if (url == null || url.isBlank()) continue;
                FotoVistoria foto = new FotoVistoria();
                foto.setVistoria(vistoria);
                foto.setUrl(url);
                fotoRepository.save(foto);
            }
        }

        // Notifica o Analista Financeiro quando houver danos causados na devolução
        if (request.getTipo() == com.locaobra.enums.TipoVistoria.DEVOLUCAO &&
                (request.getDanosCausados() != null && !request.getDanosCausados().isBlank())) {
            String patrimonio = unidade.getCodigoPatrimonio() != null ? unidade.getCodigoPatrimonio() : "sem patrimônio";
            String clienteNome = expedicao.getCliente() != null ? expedicao.getCliente().getNome() : "Cliente não informado";
            notificacaoService.criar(
                    "AVARIA",
                    "Avarias registradas na devolução",
                    "Equipamento " + patrimonio + " retornou com avarias. Cliente: " + clienteNome +
                            ". Danos: " + request.getDanosCausados(),
                    "ANALISTA_FINANCEIRO",
                    null,
                    "VISTORIA",
                    vistoria.getId()
            );
        }

        return construirResponse(vistoria);
    }

    @Transactional(readOnly = true)
    public List<VistoriaResponse> listarPorExpedicao(Long expedicaoId) {
        return vistoriaRepository.findByExpedicaoId(expedicaoId)
                .stream()
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VistoriaResponse buscarPorId(Long id) {
        return construirResponse(findOrThrow(id));
    }

    @Transactional
    public VistoriaResponse atualizar(Long id, VistoriaRequest request) {
        Vistoria vistoria = findOrThrow(id);

        if (request.getCondicaoGeral() != null) vistoria.setCondicaoGeral(request.getCondicaoGeral());
        if (request.getAvariasExistentes() != null) vistoria.setAvariasExistentes(request.getAvariasExistentes());
        if (request.getDanosCausados() != null) vistoria.setDanosCausados(request.getDanosCausados());
        if (request.getObservacoes() != null) vistoria.setObservacoes(request.getObservacoes());

        // Adiciona novas fotos se houver
        if (request.getFotos() != null && !request.getFotos().isEmpty()) {
            for (String url : request.getFotos()) {
                if (url == null || url.isBlank()) continue;
                FotoVistoria foto = new FotoVistoria();
                foto.setVistoria(vistoria);
                foto.setUrl(url);
                fotoRepository.save(foto);
            }
        }

        vistoria = vistoriaRepository.save(vistoria);
        return construirResponse(vistoria);
    }

    @Transactional
    public void deletar(Long id) {
        Vistoria vistoria = findOrThrow(id);
        vistoriaRepository.delete(vistoria);
    }

    private Vistoria findOrThrow(Long id) {
        return vistoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + id));
    }

    // ADMIN e GERENTE_OPERACOES sempre podem, por supervisionarem a operação.
    private void exigirCargo(String cargoEsperado, String mensagemErro) {
        exigirCargoDentre(new String[] { cargoEsperado }, mensagemErro);
    }

    private void exigirCargoDentre(String[] cargosPermitidos, String mensagemErro) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean temPermissao = auth != null && auth.getAuthorities().stream().anyMatch(a -> {
            String autoridade = a.getAuthority();
            if (autoridade.equals("ROLE_ADMIN") || autoridade.equals("ROLE_GERENTE_OPERACOES")) return true;
            for (String cargo : cargosPermitidos) {
                if (autoridade.equals("ROLE_" + cargo)) return true;
            }
            return false;
        });
        if (!temPermissao) {
            throw new BusinessException(mensagemErro);
        }
    }

    // IMPORTANTE: nunca chame vistoria.getFotos().clear()/.addAll() aqui.
    // Vistoria.fotos é @OneToMany(orphanRemoval = true); limpar essa coleção
    // gerenciada pelo Hibernate agenda a EXCLUSÃO das fotos no banco, mesmo que
    // as mesmas fotos sejam adicionadas de volta em seguida. Por isso montamos a
    // resposta com uma lista buscada direto do repositório, sem tocar na entidade.
    private VistoriaResponse construirResponse(Vistoria vistoria) {
        return VistoriaResponse.from(vistoria, fotoRepository.findByVistoriaId(vistoria.getId()));
    }
}