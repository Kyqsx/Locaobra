package com.locaobra.service;

import com.locaobra.dto.request.CargoRequest;
import com.locaobra.dto.response.CargoResponse;
import com.locaobra.entity.Cargo;
import com.locaobra.entity.DepartamentoEntity;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.CargoRepository;
import com.locaobra.repository.DepartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;
    private final DepartamentoRepository departamentoRepository;

    public CargoService(CargoRepository cargoRepository, DepartamentoRepository departamentoRepository) {
        this.cargoRepository = cargoRepository;
        this.departamentoRepository = departamentoRepository;
    }

    @Transactional
    public CargoResponse criar(CargoRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new BusinessException("Nome do cargo é obrigatório");
        }
        if (cargoRepository.existsByNome(request.getNome().trim().toUpperCase())) {
            throw new BusinessException("Já existe um cargo com o nome: " + request.getNome());
        }

        Cargo cargo = new Cargo();
        cargo.setNome(request.getNome().trim().toUpperCase());
        cargo.setDescricao(request.getDescricao());
        cargo.setSalarioPadrao(request.getSalarioPadrao());
        cargo.setRequisitos(request.getRequisitos());
        cargo.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);

        if (request.getDepartamentoId() != null) {
            DepartamentoEntity dept = departamentoRepository.findById(request.getDepartamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado: " + request.getDepartamentoId()));
            cargo.setDepartamento(dept);
        }

        return CargoResponse.from(cargoRepository.save(cargo));
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarTodos() {
        return cargoRepository.findAll().stream()
                .map(CargoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarAtivos() {
        return cargoRepository.findByAtivoTrue().stream()
                .map(CargoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CargoResponse buscarPorId(Long id) {
        return CargoResponse.from(findOrThrow(id));
    }

    @Transactional
    public CargoResponse atualizar(Long id, CargoRequest request) {
        Cargo cargo = findOrThrow(id);

        if (request.getNome() != null && !request.getNome().isBlank()) {
            String nomeNormalizado = request.getNome().trim().toUpperCase();
            if (!cargo.getNome().equals(nomeNormalizado) && cargoRepository.existsByNome(nomeNormalizado)) {
                throw new BusinessException("Já existe um cargo com o nome: " + request.getNome());
            }
            cargo.setNome(nomeNormalizado);
        }

        if (request.getDescricao() != null) cargo.setDescricao(request.getDescricao());
        if (request.getSalarioPadrao() != null) cargo.setSalarioPadrao(request.getSalarioPadrao());
        if (request.getRequisitos() != null) cargo.setRequisitos(request.getRequisitos());
        if (request.getAtivo() != null) cargo.setAtivo(request.getAtivo());

        if (request.getDepartamentoId() != null) {
            DepartamentoEntity dept = departamentoRepository.findById(request.getDepartamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado: " + request.getDepartamentoId()));
            cargo.setDepartamento(dept);
        } else if (request.getDepartamentoId() == null && request.getNome() != null) {
            // Se enviou dados mas departamentoId veio null explicitamente, remove o vínculo
            // (não podemos diferenciar null de "não enviado" sem um marcador,
            // entã vamos apenas ignorar quando for atualizaçã parcial.
        }

        return CargoResponse.from(cargoRepository.save(cargo));
    }

    public Cargo findOrThrow(Long id) {
        return cargoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo não encontrado: " + id));
    }
}