package com.locaobra.service;

import com.locaobra.dto.request.UnidadeEquipamentoRequest;
import com.locaobra.dto.response.UnidadeEquipamentoResponse;
import com.locaobra.entity.Deposito;
import com.locaobra.entity.Equipamento;
import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.enums.StatusUnidade;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.DepositoRepository;
import com.locaobra.repository.EquipamentoRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UnidadeEquipamentoService {

    private final UnidadeEquipamentoRepository unidadeRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final DepositoRepository depositoRepository;

    public UnidadeEquipamentoService(
            UnidadeEquipamentoRepository unidadeRepository,
            EquipamentoRepository equipamentoRepository,
            DepositoRepository depositoRepository) {
        this.unidadeRepository = unidadeRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.depositoRepository = depositoRepository;
    }

    @Transactional
    public UnidadeEquipamentoResponse criar(Long equipamentoId, UnidadeEquipamentoRequest request) {
        Equipamento equipamento = equipamentoRepository.findById(equipamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com ID: " + equipamentoId));

        if (request.getCodigoPatrimonio() != null && !request.getCodigoPatrimonio().isBlank() &&
                unidadeRepository.existsByCodigoPatrimonio(request.getCodigoPatrimonio())) {
            throw new BusinessException("Código de patrimônio já cadastrado: " + request.getCodigoPatrimonio());
        }

        if (request.getNumeroDeSerie() != null && !request.getNumeroDeSerie().isBlank() &&
                unidadeRepository.existsByNumeroDeSerie(request.getNumeroDeSerie())) {
            throw new BusinessException("Número de série já cadastrado: " + request.getNumeroDeSerie());
        }

        UnidadeEquipamento unidade = new UnidadeEquipamento();
        unidade.setEquipamento(equipamento);
        unidade.setCodigoPatrimonio(request.getCodigoPatrimonio());
        unidade.setNumeroDeSerie(request.getNumeroDeSerie());
        unidade.setStatus(request.getStatus() != null ? request.getStatus() : StatusUnidade.DISPONIVEL);
        unidade.setHorimetroAtual(request.getHorimetroAtual());
        unidade.setHorimetroLimiteManutencao(request.getHorimetroLimiteManutencao());
        unidade.setDeposito(resolverDeposito(request.getDepositoId()));

        unidade = unidadeRepository.save(unidade);
        return UnidadeEquipamentoResponse.from(unidade);
    }

    @Transactional(readOnly = true)
    public List<UnidadeEquipamentoResponse> listarPorEquipamento(Long equipamentoId) {
        return unidadeRepository.findByEquipamentoId(equipamentoId)
                .stream()
                .map(UnidadeEquipamentoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UnidadeEquipamentoResponse> listarAlertasManutencaoPreventiva() {
        return unidadeRepository.findComAlertaManutencaoPreventiva()
                .stream()
                .map(UnidadeEquipamentoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UnidadeEquipamentoResponse atualizarStatus(Long unidadeId, StatusUnidade status) {
        UnidadeEquipamento unidade = findOrThrow(unidadeId);
        unidade.setStatus(status);
        unidade = unidadeRepository.save(unidade);
        return UnidadeEquipamentoResponse.from(unidade);
    }

    @Transactional
    public UnidadeEquipamentoResponse atualizar(Long unidadeId, UnidadeEquipamentoRequest request) {
        UnidadeEquipamento unidade = findOrThrow(unidadeId);

        if (request.getCodigoPatrimonio() != null && !request.getCodigoPatrimonio().isBlank() &&
                !request.getCodigoPatrimonio().equals(unidade.getCodigoPatrimonio()) &&
                unidadeRepository.existsByCodigoPatrimonio(request.getCodigoPatrimonio())) {
            throw new BusinessException("Código de patrimônio já cadastrado: " + request.getCodigoPatrimonio());
        }

        if (request.getNumeroDeSerie() != null && !request.getNumeroDeSerie().isBlank() &&
                !request.getNumeroDeSerie().equals(unidade.getNumeroDeSerie()) &&
                unidadeRepository.existsByNumeroDeSerie(request.getNumeroDeSerie())) {
            throw new BusinessException("Número de série já cadastrado: " + request.getNumeroDeSerie());
        }

        unidade.setCodigoPatrimonio(request.getCodigoPatrimonio());
        unidade.setNumeroDeSerie(request.getNumeroDeSerie());
        if (request.getStatus() != null) {
            unidade.setStatus(request.getStatus());
        }
        if (request.getHorimetroAtual() != null) {
            unidade.setHorimetroAtual(request.getHorimetroAtual());
        }
        if (request.getHorimetroLimiteManutencao() != null) {
            unidade.setHorimetroLimiteManutencao(request.getHorimetroLimiteManutencao());
        }
        if (request.getDepositoId() != null) {
            // 0 é o valor que o frontend manda pra "Sem depósito" (select vazio
            // vira depositoId ausente, mas queremos permitir desvincular
            // explicitamente também) — ver resolverDeposito().
            unidade.setDeposito(resolverDeposito(request.getDepositoId()));
        }

        unidade = unidadeRepository.save(unidade);
        return UnidadeEquipamentoResponse.from(unidade);
    }

    @Transactional
    public void deletar(Long unidadeId) {
        UnidadeEquipamento unidade = findOrThrow(unidadeId);
        unidadeRepository.delete(unidade);
    }

    // depositoId == null -> não mexe no vínculo (form não enviou o campo).
    // depositoId == 0 -> desvincula explicitamente ("Sem depósito" no select).
    // depositoId > 0 -> resolve e vincula ao depósito informado.
    private Deposito resolverDeposito(Long depositoId) {
        if (depositoId == null || depositoId == 0L) {
            return null;
        }
        return depositoRepository.findById(depositoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado: " + depositoId));
    }

    private UnidadeEquipamento findOrThrow(Long unidadeId) {
        return unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada com ID: " + unidadeId));
    }
}
