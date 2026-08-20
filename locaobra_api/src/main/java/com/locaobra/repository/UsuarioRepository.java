package com.locaobra.repository;

import com.locaobra.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.tipo = :tipo")
    Optional<Usuario> findByEmailAndTipo(@Param("email") String email, @Param("tipo") String tipo);

    @Query("SELECT u FROM Usuario u WHERE " +
            "(u.idCliente = :idCliente AND :idCliente IS NOT NULL) OR " +
            "(u.idFuncionario = :idFuncionario AND :idFuncionario IS NOT NULL)")
    List<Usuario> findByClienteIdOrFuncionarioId(
            @Param("idCliente") Long idCliente,
            @Param("idFuncionario") Long idFuncionario);
}
