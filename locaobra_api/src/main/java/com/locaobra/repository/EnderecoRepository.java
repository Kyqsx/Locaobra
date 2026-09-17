package com.locaobra.repository;

import com.locaobra.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByClienteId(Long clienteId);

    Optional<Endereco> findByClienteIdAndPrincipalTrue(Long clienteId);

    long countByClienteId(Long clienteId);
}
