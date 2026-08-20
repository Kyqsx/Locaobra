package com.locaobra.repository;

import com.locaobra.dto.EnderecoDTO;
import com.locaobra.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    @Query("SELECT new com.locaobra.dto.EnderecoDTO(e.id_endereco, e.cep, e.rua, e.bairro, e.cidade, e.estado, e.complemento, e.numero) FROM Endereco e")
    List<EnderecoDTO> findAllBasic();

    @Query("SELECT new com.locaobra.dto.EnderecoDTO(e.id_endereco, e.cep, e.rua, e.bairro, e.cidade, e.estado, e.complemento, e.numero) FROM Endereco e WHERE e.id_endereco = :id")
    Optional<EnderecoDTO> findBasicById(Long id);

    @Query("SELECT new com.locaobra.dto.EnderecoDTO(e.id_endereco, e.cep, e.rua, e.bairro, e.cidade, e.estado, e.complemento, e.numero) FROM Endereco e WHERE e.id_endereco = :id")
    Optional<EnderecoDTO> findByIdPaciente(@Param("id") Long id);
}