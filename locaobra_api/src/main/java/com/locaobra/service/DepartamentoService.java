package com.locaobra.service;

import com.locaobra.dto.request.DepartamentoRequest;
import com.locaobra.dto.response.DepartamentoResponse;
import com.locaobra.entity.DepartamentoEntity;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.DepartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;

    public DepartamentoService(DepartamentoRepository departamentoRepository) {
        this.departamentoRepository = departamentoRepository;
    }

    @Transactional
    public DepartamentoResponse criar(DepartamentoRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new BusinessException("Nome do departamento é obrigatório");
        }
        if (departamentoRepository.existsByNome(request.getNome().trim().toUpperCase())) {
            throw new BusinessException("Já existe um departamento com o nome: " + request.getNome());
        }

        DepartamentoEntity dept = new DepartamentoEntity();
        dept.setNome(request.getNome().trim().toUpperCase());
        dept.setDescricao(request.getDescricao());
        dept.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);

        return DepartamentoResponse.from(departamentoRepository.save(dept));
    }

    @Transactional(readOnly = true)
    public List<DepartamentoResponse> listarTodos() {
        return departamentoRepository.findAll().stream()
                .map(DepartamentoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepartamentoResponse> listarAtivos() {
        return departamentoRepository.findByAtivoTrue().stream()
                .map(DepartamentoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartamentoResponse buscarPorId(Long id) {
        return DepartamentoResponse.from(findOrThrow(id));
    }

    @Transactional
    public DepartamentoResponse atualizar(Long id, DepartamentoRequest request) {
        DepartamentoEntity dept = findOrThrow(id);

        if (request.getNome() != null && !request.getNome().isBlank()) {
            String nomeNormalizado = request.getNome().trim().toUpperCase();
            if (!dept.getNome().equals(nomeNormalizado) && departamentoRepository.existsByNome(nomeNormalizado)) {
                throw new BusinessException("Já existe um departamento com o nome: " + request.getNome());
            }
            dept.setNome(nomeNormalizado);
        }

        if (request.getDescricao() != null) dept.setDescricao(request.getDescricao());
        if (request.getAtivo() != null) dept.setAtivo(request.getAtivo());

        return DepartamentoResponse.from(departamentoRepository.save(dept));
    }

    public DepartamentoEntity findOrThrow(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado: " + id));
    }
}