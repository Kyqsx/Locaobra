package com.locaobra.service;

import com.locaobra.dto.request.PecaEstoqueRequest;
import com.locaobra.dto.response.PecaEstoqueResponse;
import com.locaobra.entity.PecaEstoque;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.PecaEstoqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PecaEstoqueService {

    private final PecaEstoqueRepository pecaRepository;

    public PecaEstoqueService(PecaEstoqueRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public PecaEstoqueResponse criar(PecaEstoqueRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new BusinessException("Nome da peça é obrigatório");
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()
                && pecaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Código de peça já cadastrado: " + request.getCodigo());
        }

        PecaEstoque peca = new PecaEstoque();
        peca.setCodigo(request.getCodigo());
        peca.setNome(request.getNome());
        peca.setQuantidadeEmEstoque(request.getQuantidadeEmEstoque() != null ? request.getQuantidadeEmEstoque() : 0);
        peca.setUnidadeMedida(request.getUnidadeMedida());
        peca.setEstoqueMinimo(request.getEstoqueMinimo());

        peca = pecaRepository.save(peca);
        return PecaEstoqueResponse.from(peca);
    }

    @Transactional(readOnly = true)
    public List<PecaEstoqueResponse> listar() {
        return pecaRepository.findAll()
                .stream()
                .map(PecaEstoqueResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PecaEstoqueResponse buscarPorId(Long id) {
        return PecaEstoqueResponse.from(findOrThrow(id));
    }

    @Transactional
    public PecaEstoqueResponse atualizar(Long id, PecaEstoqueRequest request) {
        PecaEstoque peca = findOrThrow(id);

        if (request.getCodigo() != null && !request.getCodigo().isBlank()
                && !request.getCodigo().equals(peca.getCodigo())
                && pecaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Código de peça já cadastrado: " + request.getCodigo());
        }

        if (request.getNome() != null && !request.getNome().isBlank()) {
            peca.setNome(request.getNome());
        }
        peca.setCodigo(request.getCodigo());
        peca.setUnidadeMedida(request.getUnidadeMedida());
        peca.setEstoqueMinimo(request.getEstoqueMinimo());
        if (request.getQuantidadeEmEstoque() != null) {
            peca.setQuantidadeEmEstoque(request.getQuantidadeEmEstoque());
        }

        peca = pecaRepository.save(peca);
        return PecaEstoqueResponse.from(peca);
    }

    @Transactional
    public PecaEstoqueResponse entradaEstoque(Long id, Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new BusinessException("Quantidade de entrada deve ser maior que zero");
        }
        PecaEstoque peca = findOrThrow(id);
        peca.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque() + quantidade);
        peca = pecaRepository.save(peca);
        return PecaEstoqueResponse.from(peca);
    }

    @Transactional
    public void deletar(Long id) {
        PecaEstoque peca = findOrThrow(id);
        pecaRepository.delete(peca);
    }

    private PecaEstoque findOrThrow(Long id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + id));
    }
}
