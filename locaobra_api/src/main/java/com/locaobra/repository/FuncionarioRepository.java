package com.locaobra.repository;

import com.locaobra.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByCpf(String cpf);

    Optional<Funcionario> findByMatricula(String matricula);

    boolean existsByCpf(String cpf);

    boolean existsByMatricula(String matricula);

    List<Funcionario> findByStatusTrue();

    List<Funcionario> findByNomeContainingIgnoreCase(String nome);
}
