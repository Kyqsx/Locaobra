package com.locaobra.repository;

import com.locaobra.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByCpfCnpj(String cpfCnpj);

    boolean existsByTelefone(String telefone);

    List<Cliente> findByAtivoTrue();

    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
