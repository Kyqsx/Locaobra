package com.locaobra.service;

import com.locaobra.dto.request.FuncionarioRequest;
import com.locaobra.dto.response.FuncionarioResponse;
import com.locaobra.entity.Cargo;
import com.locaobra.entity.DepartamentoEntity;
import com.locaobra.entity.Funcionario;
import com.locaobra.entity.Usuario;
import com.locaobra.enums.TipoUsuario;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.CargoRepository;
import com.locaobra.repository.DepartamentoRepository;
import com.locaobra.repository.FuncionarioRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CargoRepository cargoRepository;
    private final DepartamentoRepository departamentoRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              CargoRepository cargoRepository,
                              DepartamentoRepository departamentoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cargoRepository = cargoRepository;
        this.departamentoRepository = departamentoRepository;
    }

    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest request) {
        validarDuplicidade(request.getCpf(), request.getMatricula());
        validarCamposObrigatoriosParaCriacao(request);

        Funcionario funcionario = new Funcionario();
        preencherFuncionario(funcionario, request, false);
        funcionario.setStatus(request.getStatus() != null ? request.getStatus() : true);

        Funcionario salvo = funcionarioRepository.save(funcionario);
        criarUsuarioAutomatico(salvo, request);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarTodos() {
        return funcionarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarAtivos() {
        return funcionarioRepository.findByStatusTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> buscarPorNome(String nome) {
        return funcionarioRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        Funcionario funcionario = findOrThrow(id);

        if (request.getCpf() != null && !funcionario.getCpf().equals(request.getCpf()) &&
                funcionarioRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF ja cadastrado: " + request.getCpf());
        }
        if (request.getMatricula() != null && !funcionario.getMatricula().equals(request.getMatricula()) &&
                funcionarioRepository.existsByMatricula(request.getMatricula())) {
            throw new BusinessException("Matricula ja cadastrada: " + request.getMatricula());
        }

        preencherFuncionario(funcionario, request, true);
        funcionario.setStatus(request.getStatus() != null ? request.getStatus() : funcionario.getStatus());

        Funcionario salvo = funcionarioRepository.save(funcionario);
        atualizarUsuarioAutomatico(salvo, request);
        return toResponse(salvo);
    }

    @Transactional
    public void desativar(Long id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setStatus(false);
        funcionarioRepository.save(funcionario);

        findUsuariosPorFuncionarioId(id).forEach(usuario -> {
            usuario.setAtivo(false);
            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public void deletar(Long id) {
        findOrThrow(id);
        findUsuariosPorFuncionarioId(id).forEach(usuario -> {
            usuario.setAtivo(false);
            usuarioRepository.save(usuario);
        });
        funcionarioRepository.deleteById(id);
    }

    private void validarDuplicidade(String cpf, String matricula) {
        if (funcionarioRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF ja cadastrado: " + cpf);
        }
        if (funcionarioRepository.existsByMatricula(matricula)) {
            throw new BusinessException("Matricula ja cadastrada: " + matricula);
        }
    }

    private void validarCamposObrigatoriosParaCriacao(FuncionarioRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new BusinessException("Nome e obrigatorio");
        }
        if (request.getCpf() == null || request.getCpf().isBlank()) {
            throw new BusinessException("CPF e obrigatorio");
        }
        if (request.getMatricula() == null || request.getMatricula().isBlank()) {
            throw new BusinessException("Matricula e obrigatoria");
        }
        if (request.getCargoId() == null) {
            throw new BusinessException("Cargo e obrigatorio");
        }
        if (request.getDepartamentoId() == null) {
            throw new BusinessException("Departamento e obrigatorio");
        }
        if (request.getSalario() == null || request.getSalario().isBlank()) {
            throw new BusinessException("Salario e obrigatorio");
        }
        if (request.getDataNascimento() == null || request.getDataNascimento().isBlank()) {
            throw new BusinessException("Data de nascimento e obrigatoria");
        }
        if (request.getDataAdmissao() == null || request.getDataAdmissao().isBlank()) {
            throw new BusinessException("Data de admissao e obrigatoria");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("E-mail e obrigatorio");
        }
    }
    private void preencherFuncionario(Funcionario funcionario, FuncionarioRequest request, boolean isUpdate) {
        if (!isUpdate || request.getNome() != null && !request.getNome().isBlank()) {
            funcionario.setNome(request.getNome());
        }
        if (!isUpdate || request.getCpf() != null && !request.getCpf().isBlank()) {
            funcionario.setCpf(request.getCpf());
        }
        if (request.getTelefone() != null) {
            funcionario.setTelefone(request.getTelefone());
        }
        if (!isUpdate || request.getMatricula() != null && !request.getMatricula().isBlank()) {
            funcionario.setMatricula(request.getMatricula());
        }
        if (request.getCargoId() != null) {
            Cargo cargo = cargoRepository.findById(request.getCargoId())
                    .orElseThrow(() -> new BusinessException("Cargo nao encontrado: " + request.getCargoId()));
            funcionario.setCargo(cargo);
        } else if (!isUpdate) {
            throw new BusinessException("Cargo e obrigatorio");
        }
        if (request.getDepartamentoId() != null) {
            DepartamentoEntity dept = departamentoRepository.findById(request.getDepartamentoId())
                    .orElseThrow(() -> new BusinessException("Departamento nao encontrado: " + request.getDepartamentoId()));
            funcionario.setDepartamento(dept);
        } else if (!isUpdate) {
            throw new BusinessException("Departamento e obrigatorio");
        }
        if (!isUpdate || request.getSalario() != null && !request.getSalario().isBlank()) {
            funcionario.setSalario(parseSalario(request.getSalario(), !isUpdate));
        }
        if (!isUpdate || request.getDataNascimento() != null && !request.getDataNascimento().isBlank()) {
            funcionario.setDataNascimento(parseData(request.getDataNascimento(), !isUpdate));
        }
        if (!isUpdate || request.getDataAdmissao() != null && !request.getDataAdmissao().isBlank()) {
            funcionario.setDataAdmissao(parseData(request.getDataAdmissao(), !isUpdate));
        }
        if (request.getDataDemissao() != null && !request.getDataDemissao().isBlank()) {
            funcionario.setDataDemissao(parseData(request.getDataDemissao(), false));
        }
    }
    private FuncionarioResponse toResponse(Funcionario funcionario) {
        FuncionarioResponse response = FuncionarioResponse.from(funcionario);
        findUsuarioPorFuncionarioId(funcionario.getId())
                .ifPresent(usuario -> response.setEmail(usuario.getEmail()));
        return response;
    }

    private void criarUsuarioAutomatico(Funcionario funcionario, FuncionarioRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("E-mail e obrigatorio para criar o usuario do funcionario");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail ja cadastrado: " + request.getEmail());
        }

        String senha = request.getSenha() != null && !request.getSenha().isBlank()
                ? request.getSenha()
                : "123456";

        Usuario usuario = new Usuario();
        usuario.setNome(funcionario.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setTipo(TipoUsuario.FUNCIONARIO);
        usuario.setAtivo(funcionario.getStatus());
        usuario.setIdFuncionario(funcionario.getId());
        usuarioRepository.save(usuario);
    }

    private void atualizarUsuarioAutomatico(Funcionario funcionario, FuncionarioRequest request) {
        List<Usuario> usuarios = findUsuariosPorFuncionarioId(funcionario.getId());
        if (usuarios.isEmpty()) {
            criarUsuarioAutomatico(funcionario, request);
            return;
        }

        Usuario usuarioPrincipal = usuarios.get(0);
        for (int i = 1; i < usuarios.size(); i++) {
            Usuario usuarioDuplicado = usuarios.get(i);
            usuarioDuplicado.setAtivo(false);
            usuarioRepository.save(usuarioDuplicado);
        }

        usuarioPrincipal.setNome(funcionario.getNome());
        usuarioPrincipal.setAtivo(funcionario.getStatus());
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(usuarioPrincipal.getEmail())) {
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("E-mail ja cadastrado: " + request.getEmail());
            }
            usuarioPrincipal.setEmail(request.getEmail());
        }
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuarioPrincipal.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        usuarioRepository.save(usuarioPrincipal);
    }

    private Double parseSalario(String salario, boolean required) {
        if (salario == null || salario.isBlank()) {
            if (required) {
                throw new BusinessException("Salario e obrigatorio");
            }
            return null;
        }
        try {
            return Double.parseDouble(salario.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BusinessException("Salario invalido: " + salario);
        }
    }

    private LocalDateTime parseData(String valor, boolean required) {
        if (valor == null || valor.isBlank()) {
            if (required) {
                throw new BusinessException("Data e obrigatoria");
            }
            return null;
        }
        try {
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(valor).atStartOfDay();
            } catch (DateTimeParseException ex) {
                throw new BusinessException("Data invalida: " + valor);
            }
        }
    }

    private Optional<Usuario> findUsuarioPorFuncionarioId(Long funcionarioId) {
        return findUsuariosPorFuncionarioId(funcionarioId).stream().findFirst();
    }

    private List<Usuario> findUsuariosPorFuncionarioId(Long funcionarioId) {
        return usuarioRepository.findByClienteIdOrFuncionarioId(null, funcionarioId);
    }

    public Funcionario findOrThrow(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionario nao encontrado: " + id));
    }
}
