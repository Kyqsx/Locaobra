package com.locaobra.service;

import com.locaobra.dto.request.DiagnosticoRequest;
import com.locaobra.dto.request.ItemOrdemServicoRequest;
import com.locaobra.dto.request.OrdemServicoRequest;
import com.locaobra.dto.response.OrdemServicoResponse;
import com.locaobra.entity.Funcionario;
import com.locaobra.entity.ItemOrdemServico;
import com.locaobra.entity.OrdemServico;
import com.locaobra.entity.PecaEstoque;
import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.enums.StatusOrdemServico;
import com.locaobra.enums.StatusUnidade;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.FuncionarioRepository;
import com.locaobra.repository.ItemOrdemServicoRepository;
import com.locaobra.repository.OrdemServicoRepository;
import com.locaobra.repository.PecaEstoqueRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdemServicoService {

    private static final List<StatusOrdemServico> STATUS_ABERTOS =
            Arrays.asList(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_ANDAMENTO);

    private final OrdemServicoRepository osRepository;
    private final ItemOrdemServicoRepository itemRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PecaEstoqueRepository pecaRepository;

    public OrdemServicoService(
            OrdemServicoRepository osRepository,
            ItemOrdemServicoRepository itemRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            FuncionarioRepository funcionarioRepository,
            PecaEstoqueRepository pecaRepository) {
        this.osRepository = osRepository;
        this.itemRepository = itemRepository;
        this.unidadeRepository = unidadeRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public OrdemServicoResponse abrir(OrdemServicoRequest request) {
        if (request.getUnidadeId() == null) {
            throw new BusinessException("Unidade é obrigatória para abrir a OS");
        }

        UnidadeEquipamento unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + request.getUnidadeId()));

        if (osRepository.existsByUnidadeIdAndStatusIn(unidade.getId(), STATUS_ABERTOS)) {
            throw new BusinessException("Já existe uma Ordem de Serviço em aberto para esta unidade");
        }

        OrdemServico os = new OrdemServico();
        os.setUnidade(unidade);
        os.setObservacoes(request.getObservacoes());
        os.setStatus(StatusOrdemServico.ABERTA);
        os.setAbertaEm(LocalDateTime.now());

        if (request.getTecnicoId() != null) {
            os.setTecnico(buscarTecnico(request.getTecnicoId()));
        }

        // Máquina entra em manutenção assim que a OS é aberta
        if (unidade.getStatus() != StatusUnidade.EM_MANUTENCAO) {
            unidade.setStatus(StatusUnidade.EM_MANUTENCAO);
            unidadeRepository.save(unidade);
        }

        os = osRepository.save(os);
        return OrdemServicoResponse.from(os);
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarAguardandoManutencao() {
        return osRepository.findByStatusIn(STATUS_ABERTOS)
                .stream()
                .map(OrdemServicoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar(StatusOrdemServico status) {
        List<OrdemServico> ordens = status != null
                ? osRepository.findByStatus(status)
                : osRepository.findAll();
        return ordens.stream().map(OrdemServicoResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarPorUnidade(Long unidadeId) {
        return osRepository.findByUnidadeId(unidadeId)
                .stream()
                .map(OrdemServicoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(Long id) {
        return OrdemServicoResponse.from(findOrThrow(id));
    }

    @Transactional
    public OrdemServicoResponse atualizarDiagnostico(Long id, DiagnosticoRequest request) {
        OrdemServico os = findOrThrow(id);
        garantirNaoFinalizada(os);

        if (request.getDiagnostico() != null) os.setDiagnostico(request.getDiagnostico());
        if (request.getObservacoes() != null) os.setObservacoes(request.getObservacoes());
        if (request.getHorimetroRegistrado() != null) os.setHorimetroRegistrado(request.getHorimetroRegistrado());
        if (request.getTecnicoId() != null) os.setTecnico(buscarTecnico(request.getTecnicoId()));

        if (os.getStatus() == StatusOrdemServico.ABERTA) {
            os.setStatus(StatusOrdemServico.EM_ANDAMENTO);
            os.setIniciadaEm(LocalDateTime.now());
        }

        os = osRepository.save(os);
        return OrdemServicoResponse.from(os);
    }

    @Transactional
    public OrdemServicoResponse adicionarPeca(Long osId, ItemOrdemServicoRequest request) {
        OrdemServico os = findOrThrow(osId);
        garantirNaoFinalizada(os);

        if (request.getPecaId() == null) {
            throw new BusinessException("Peça é obrigatória");
        }
        if (request.getQuantidade() == null || request.getQuantidade() <= 0) {
            throw new BusinessException("Quantidade deve ser maior que zero");
        }

        PecaEstoque peca = pecaRepository.findById(request.getPecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.getPecaId()));

        if (peca.getQuantidadeEmEstoque() < request.getQuantidade()) {
            throw new BusinessException("Estoque insuficiente para \"" + peca.getNome()
                    + "\" (disponível: " + peca.getQuantidadeEmEstoque() + ")");
        }

        // Baixa no estoque
        peca.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque() - request.getQuantidade());
        pecaRepository.save(peca);

        ItemOrdemServico item = new ItemOrdemServico();
        item.setOrdemServico(os);
        item.setPeca(peca);
        item.setQuantidade(request.getQuantidade());
        itemRepository.save(item);

        return OrdemServicoResponse.from(findOrThrow(osId));
    }

    @Transactional
    public OrdemServicoResponse removerPeca(Long osId, Long itemId) {
        OrdemServico os = findOrThrow(osId);
        garantirNaoFinalizada(os);

        ItemOrdemServico item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + itemId));

        if (!item.getOrdemServico().getId().equals(os.getId())) {
            throw new BusinessException("Este item não pertence a esta Ordem de Serviço");
        }

        // Devolve ao estoque
        PecaEstoque peca = item.getPeca();
        peca.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque() + item.getQuantidade());
        pecaRepository.save(peca);

        itemRepository.delete(item);

        return OrdemServicoResponse.from(findOrThrow(osId));
    }

    @Transactional
    public OrdemServicoResponse concluir(Long osId) {
        OrdemServico os = findOrThrow(osId);
        garantirNaoFinalizada(os);

        if (os.getDiagnostico() == null || os.getDiagnostico().isBlank()) {
            throw new BusinessException("Registre o diagnóstico antes de concluir a Ordem de Serviço");
        }

        os.setStatus(StatusOrdemServico.CONCLUIDA);
        os.setConcluidaEm(LocalDateTime.now());
        os = osRepository.save(os);

        // Libera a unidade de volta para a vitrine
        UnidadeEquipamento unidade = os.getUnidade();
        unidade.setStatus(StatusUnidade.DISPONIVEL);
        if (os.getHorimetroRegistrado() != null) {
            unidade.setHorimetroAtual(os.getHorimetroRegistrado());
        }
        unidadeRepository.save(unidade);

        return OrdemServicoResponse.from(os);
    }

    @Transactional
    public OrdemServicoResponse cancelar(Long osId, String motivo) {
        OrdemServico os = findOrThrow(osId);
        garantirNaoFinalizada(os);

        // Devolve estoque de todas as peças já lançadas
        for (ItemOrdemServico item : itemRepository.findByOrdemServicoId(osId)) {
            PecaEstoque peca = item.getPeca();
            peca.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque() + item.getQuantidade());
            pecaRepository.save(peca);
        }

        os.setStatus(StatusOrdemServico.CANCELADA);
        if (motivo != null && !motivo.isBlank()) {
            os.setObservacoes(motivo);
        }
        os = osRepository.save(os);
        return OrdemServicoResponse.from(os);
    }

    private void garantirNaoFinalizada(OrdemServico os) {
        if (os.getStatus() == StatusOrdemServico.CONCLUIDA || os.getStatus() == StatusOrdemServico.CANCELADA) {
            throw new BusinessException("Esta Ordem de Serviço já está " +
                    (os.getStatus() == StatusOrdemServico.CONCLUIDA ? "concluída" : "cancelada"));
        }
    }

    private Funcionario buscarTecnico(Long tecnicoId) {
        Funcionario tecnico = funcionarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + tecnicoId));
        if (tecnico.getCargo() == null || !"TECNICO_MANUTENCAO".equals(tecnico.getCargo().getNome())) {
            throw new BusinessException("Funcionário informado não é Técnico de Manutenção");
        }
        return tecnico;
    }

    private OrdemServico findOrThrow(Long id) {
        return osRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de Serviço não encontrada: " + id));
    }
}
