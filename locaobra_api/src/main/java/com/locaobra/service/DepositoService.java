package com.locaobra.service;

import com.locaobra.dto.request.DepositoRequest;
import com.locaobra.dto.response.DepositoResponse;
import com.locaobra.entity.Deposito;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.DepositoRepository;
import com.locaobra.repository.FuncionarioRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepositoService {

    private final DepositoRepository depositoRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final FuncionarioRepository funcionarioRepository;

    public DepositoService(
            DepositoRepository depositoRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            FuncionarioRepository funcionarioRepository) {
        this.depositoRepository = depositoRepository;
        this.unidadeRepository = unidadeRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional
    public DepositoResponse criar(DepositoRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new BusinessException("Nome do depósito é obrigatório");
        }
        String nome = request.getNome().trim();
        if (depositoRepository.existsByNome(nome)) {
            throw new BusinessException("Já existe um depósito com o nome: " + nome);
        }

        Deposito deposito = new Deposito();
        deposito.setNome(nome);
        deposito.setEndereco(request.getEndereco());
        deposito.setDescricao(request.getDescricao());
        deposito.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);

        return comContagens(depositoRepository.save(deposito));
    }

    @Transactional(readOnly = true)
    public List<DepositoResponse> listarTodos() {
        return depositoRepository.findAll().stream()
                .map(this::comContagens)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepositoResponse> listarAtivos() {
        return depositoRepository.findByAtivoTrue().stream()
                .map(this::comContagens)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepositoResponse buscarPorId(Long id) {
        return comContagens(findOrThrow(id));
    }

    @Transactional
    public DepositoResponse atualizar(Long id, DepositoRequest request) {
        Deposito deposito = findOrThrow(id);

        if (request.getNome() != null && !request.getNome().isBlank()) {
            String nome = request.getNome().trim();
            if (!deposito.getNome().equals(nome) && depositoRepository.existsByNome(nome)) {
                throw new BusinessException("Já existe um depósito com o nome: " + nome);
            }
            deposito.setNome(nome);
        }
        if (request.getEndereco() != null) deposito.setEndereco(request.getEndereco());
        if (request.getDescricao() != null) deposito.setDescricao(request.getDescricao());
        if (request.getAtivo() != null) deposito.setAtivo(request.getAtivo());

        return comContagens(depositoRepository.save(deposito));
    }

    @Transactional
    public void deletar(Long id) {
        Deposito deposito = findOrThrow(id);
        // Excluir um depósito que ainda tem unidades ou funcionários vinculados
        // deixaria essas referências "soltas" (o vínculo é opcional, então o
        // banco não bloqueia por FK) — melhor pedir pra desvincular antes,
        // assim ninguém perde de vista onde o patrimônio realmente está.
        if (unidadeRepository.existsByDepositoId(id)) {
            throw new BusinessException("Este depósito ainda tem unidades de equipamento vinculadas. Mova-as para outro depósito antes de excluir.");
        }
        if (funcionarioRepository.existsByDepositoId(id)) {
            throw new BusinessException("Este depósito ainda tem funcionários vinculados. Realoque-os antes de excluir.");
        }
        depositoRepository.delete(deposito);
    }

    private DepositoResponse comContagens(Deposito deposito) {
        DepositoResponse r = DepositoResponse.from(deposito);
        r.setQuantidadeUnidades(unidadeRepository.countByDepositoId(deposito.getId()));
        r.setQuantidadeFuncionarios(funcionarioRepository.countByDepositoId(deposito.getId()));
        return r;
    }

    private Deposito findOrThrow(Long id) {
        return depositoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado: " + id));
    }
}
