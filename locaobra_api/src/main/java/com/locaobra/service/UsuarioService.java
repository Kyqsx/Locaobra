package com.locaobra.service;

import com.locaobra.dto.request.UsuarioRequest;
import com.locaobra.dto.response.UsuarioResponse;
import com.locaobra.entity.Usuario;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + request.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setTipo(request.getTipo());
        usuario.setAtivo(true);

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.from(findOrThrow(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = findOrThrow(id);

        if (!usuario.getEmail().equals(request.getEmail()) &&
                usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + request.getEmail());
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setTipo(request.getTipo());

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desativar(Long id) {
        Usuario usuario = findOrThrow(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private Usuario findOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
