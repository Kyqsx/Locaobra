package com.locaobra.service;

import com.locaobra.config.JwtService;
import com.locaobra.dto.request.LoginRequest;
import com.locaobra.dto.response.LoginResponse;
import com.locaobra.entity.Cliente;
import com.locaobra.entity.Usuario;
import com.locaobra.exception.BusinessException;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.FuncionarioRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.locaobra.dto.request.SignupRequest;
import com.locaobra.enums.TipoUsuario;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void signup(SignupRequest request) {
        // 1. Verificar se o email já existe para evitar duplicidade
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Erro: Este email já está em uso!");
        }

        // SEGURANÇA: o cadastro público SEMPRE cria um CLIENTE. O campo "tipo" do
        // corpo da requisição é ignorado — aceitá-lo permitia que qualquer pessoa
        // criasse uma conta ADMIN/FUNCIONARIO. Contas internas são criadas apenas
        // por um ADMIN autenticado (UsuarioController / FuncionarioController).
        TipoUsuario tipoUsuario = TipoUsuario.CLIENTE;

        Cliente cliente = null;
        if (tipoUsuario == TipoUsuario.CLIENTE) {
            cliente = new Cliente();
            cliente.setNome(request.getNome());
            cliente.setCpfCnpj("00000000000"); // Placeholder, ajuste conforme necessário
            cliente.setTelefone("0000000000"); // Placeholder, ajuste conforme necessário
            cliente.setAtivo(true);
            cliente = clienteRepository.save(cliente);
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(request.getNome());
        novoUsuario.setEmail(request.getEmail());
        novoUsuario.setTipo(tipoUsuario);
        if (cliente != null) {
            novoUsuario.setIdCliente(cliente.getId());
        }

        // 3. CRIPTOGRAFAR a senha antes de salvar
        String senhaCriptografada = passwordEncoder.encode(request.getSenha());
        novoUsuario.setSenha(senhaCriptografada);

        // 4. Salvar no banco de dados
        usuarioRepository.save(novoUsuario);
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email ou senha inválidos"));

        if (!usuario.getAtivo()) {
            throw new BusinessException("Usuário inativo. Contate o administrador.");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new BusinessException("Email ou senha inválidos");
        }

        String cargo = null;
        if (usuario.getTipo() == TipoUsuario.FUNCIONARIO && usuario.getIdFuncionario() != null) {
            cargo = funcionarioRepository.findById(usuario.getIdFuncionario())
                    .map(f -> f.getCargo() != null ? f.getCargo().getNome() : null)
                    .orElse(null);
        }

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getTipo().name(), cargo);

        return new LoginResponse(token, usuario.getEmail(), usuario.getNome(), usuario.getTipo());
    }
}