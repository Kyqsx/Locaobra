package com.locaobra.service;

import com.locaobra.dto.request.VistoriaRequest;
import com.locaobra.dto.response.VistoriaResponse;
import com.locaobra.entity.Expedicao;
import com.locaobra.entity.ItemExpedicao;
import com.locaobra.entity.FotoVistoria;
import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.entity.Vistoria;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.TipoExpedicao;
import com.locaobra.enums.TipoVistoria;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ExpedicaoRepository;
import com.locaobra.repository.FotoVistoriaRepository;
import com.locaobra.repository.ItemExpedicaoRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import com.locaobra.repository.UsuarioRepository;
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
    private final ItemExpedicaoRepository itemExpedicaoRepository;
    private final UsuarioRepository usuarioRepository;

    public VistoriaService(
            VistoriaRepository vistoriaRepository,
            ExpedicaoRepository expedicaoRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            FotoVistoriaRepository fotoRepository,
            NotificacaoService notificacaoService,
            ItemExpedicaoRepository itemExpedicaoRepository,
            UsuarioRepository usuarioRepository) {
        this.vistoriaRepository = vistoriaRepository;
        this.expedicaoRepository = expedicaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.fotoRepository = fotoRepository;
        this.notificacaoService = notificacaoService;
        this.itemExpedicaoRepository = itemExpedicaoRepository;
        this.usuarioRepository = usuarioRepository;
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
        exigirTecnico(request.getTipo());
        validarTipoContraExpedicao(expedicao, request.getTipo());

        if (request.getUnidadeId() == null) {
            throw new BusinessException("Unidade é obrigatória para a vistoria");
        }

        UnidadeEquipamento unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + request.getUnidadeId()));

        // A unidade vistoriada precisa fazer parte dessa expedição.
        boolean naExpedicao = itemExpedicaoRepository.findByExpedicaoId(expedicaoId).stream()
                .map(ItemExpedicao::getUnidade)
                .anyMatch(u -> u != null && u.getId().equals(unidade.getId()));
        if (!naExpedicao) {
            throw new BusinessException("Essa unidade não faz parte da expedição.");
        }

        boolean jaExiste = vistoriaRepository.existsByExpedicaoIdAndUnidadeIdAndTipo(
                expedicaoId, request.getUnidadeId(), request.getTipo());
        if (jaExiste) {
            throw new BusinessException("Já existe vistoria de " + request.getTipo() + " para esta unidade nesta expedição");
        }

        String condicao = normalizarCondicao(request.getCondicaoGeral());
        boolean temFotos = temFotos(request);

        // A vistoria de pré-saída é a PROVA do estado em que o equipamento saiu:
        // sem foto, não há como contestar um dano depois. Na devolução, foto é
        // obrigatória quando há dano registrado.
        if (request.getTipo() == TipoVistoria.ENTREGA && !temFotos) {
            throw new BusinessException("A vistoria de entrega exige ao menos uma foto do estado do equipamento.");
        }
        if (request.getTipo() == TipoVistoria.DEVOLUCAO && !temFotos
                && request.getDanosCausados() != null && !request.getDanosCausados().isBlank()) {
            throw new BusinessException("Anexe ao menos uma foto dos danos registrados na devolução.");
        }

        Vistoria vistoria = new Vistoria();
        vistoria.setExpedicao(expedicao);
        vistoria.setUnidade(unidade);
        vistoria.setTipo(request.getTipo());
        vistoria.setCondicaoGeral(condicao);
        aplicarCamposDoTipo(vistoria, request);
        vistoria.setObservacoes(request.getObservacoes());
        vistoria.setRealizadaEm(LocalDateTime.now());
        vistoria.setResponsavel(nomeDoUsuarioLogado());

        vistoria = vistoriaRepository.save(vistoria);

        salvarFotos(vistoria, request);

        // Notifica o Analista Financeiro quando houver danos causados na devolução
        if (request.getTipo() == TipoVistoria.DEVOLUCAO &&
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
    public VistoriaResponse buscarPorId(Long expedicaoId, Long id) {
        return construirResponse(findDaExpedicao(expedicaoId, id));
    }

    // Editar/excluir também é coisa do técnico (antes qualquer cargo com acesso a
    // /api/expedicoes/** conseguia, pela API). A vistoria de ENTREGA fica travada
    // depois do check-out: é a prova do estado em que o equipamento saiu.
    @Transactional
    public VistoriaResponse atualizar(Long expedicaoId, Long id, VistoriaRequest request) {
        Vistoria vistoria = findDaExpedicao(expedicaoId, id);
        exigirTecnico(vistoria.getTipo());
        validarEditavel(vistoria);

        if (request.getCondicaoGeral() != null) vistoria.setCondicaoGeral(normalizarCondicao(request.getCondicaoGeral()));
        if (request.getObservacoes() != null) vistoria.setObservacoes(request.getObservacoes());
        if (vistoria.getTipo() == TipoVistoria.ENTREGA) {
            if (request.getAvariasExistentes() != null) vistoria.setAvariasExistentes(request.getAvariasExistentes());
        } else {
            if (request.getDanosCausados() != null) vistoria.setDanosCausados(request.getDanosCausados());
        }

        salvarFotos(vistoria, request);

        vistoria = vistoriaRepository.save(vistoria);
        return construirResponse(vistoria);
    }

    @Transactional
    public void deletar(Long expedicaoId, Long id) {
        Vistoria vistoria = findDaExpedicao(expedicaoId, id);
        exigirTecnico(vistoria.getTipo());
        validarEditavel(vistoria);
        vistoriaRepository.delete(vistoria);
    }

    // ----------------------------------------------------------------------
    // Regras
    // ----------------------------------------------------------------------

    private void exigirTecnico(TipoVistoria tipo) {
        exigirCargo("TECNICO_MANUTENCAO", tipo == TipoVistoria.ENTREGA
                ? "Somente o técnico de manutenção pode registrar a vistoria de entrega (pré-saída)."
                : "Somente o técnico de manutenção pode registrar a vistoria de devolução.");
    }

    // ENTREGA: só numa entrega Agendada (antes do check-out).
    // DEVOLUCAO: só numa COLETA que já saiu do depósito (em trânsito, confirmada ou concluída).
    private void validarTipoContraExpedicao(Expedicao expedicao, TipoVistoria tipo) {
        if (tipo == TipoVistoria.ENTREGA) {
            if (expedicao.getTipo() != TipoExpedicao.ENTREGA || expedicao.getStatus() != StatusExpedicao.AGENDADO) {
                throw new BusinessException("A vistoria de entrega só pode ser registrada em uma ENTREGA Agendada (antes do check-out).");
            }
        } else {
            boolean statusOk = expedicao.getStatus() == StatusExpedicao.EM_TRANSITO
                    || expedicao.getStatus() == StatusExpedicao.ENTREGUE
                    || expedicao.getStatus() == StatusExpedicao.CONCLUIDO;
            if (expedicao.getTipo() != TipoExpedicao.COLETA || !statusOk) {
                throw new BusinessException("A vistoria de devolução só pode ser registrada em uma COLETA que já saiu do depósito.");
            }
        }
    }

    private void validarEditavel(Vistoria vistoria) {
        if (vistoria.getTipo() == TipoVistoria.ENTREGA
                && vistoria.getExpedicao().getStatus() != StatusExpedicao.AGENDADO) {
            throw new BusinessException("A vistoria de entrega não pode mais ser alterada depois do check-out.");
        }
    }

    private String normalizarCondicao(String condicao) {
        if (condicao == null || condicao.isBlank()) return "BOM";
        String c = condicao.trim().toUpperCase();
        if (!c.equals("BOM") && !c.equals("REGULAR") && !c.equals("RUIM")) {
            throw new BusinessException("Condição geral inválida (use BOM, REGULAR ou RUIM).");
        }
        return c;
    }

    // Cada tipo guarda só o seu campo: "avarias pré-existentes" é da entrega;
    // "danos causados pelo cliente" é da devolução.
    private void aplicarCamposDoTipo(Vistoria vistoria, VistoriaRequest request) {
        if (request.getTipo() == TipoVistoria.ENTREGA) {
            vistoria.setAvariasExistentes(request.getAvariasExistentes());
            vistoria.setDanosCausados(null);
        } else {
            vistoria.setDanosCausados(request.getDanosCausados());
            vistoria.setAvariasExistentes(null);
        }
    }

    private boolean temFotos(VistoriaRequest request) {
        return request.getFotos() != null && request.getFotos().stream().anyMatch(u -> u != null && !u.isBlank());
    }

    private void salvarFotos(Vistoria vistoria, VistoriaRequest request) {
        if (request.getFotos() == null) return;
        for (String url : request.getFotos()) {
            if (url == null || url.isBlank()) continue;
            FotoVistoria foto = new FotoVistoria();
            foto.setVistoria(vistoria);
            foto.setUrl(url);
            fotoRepository.save(foto);
        }
    }

    private String nomeDoUsuarioLogado() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String email = auth.getPrincipal().toString();
        return usuarioRepository.findByEmail(email).map(u -> u.getNome()).orElse(email);
    }

    private Vistoria findDaExpedicao(Long expedicaoId, Long id) {
        Vistoria vistoria = findOrThrow(id);
        if (vistoria.getExpedicao() == null || !vistoria.getExpedicao().getId().equals(expedicaoId)) {
            throw new ResourceNotFoundException("Vistoria não encontrada nesta expedição: " + id);
        }
        return vistoria;
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