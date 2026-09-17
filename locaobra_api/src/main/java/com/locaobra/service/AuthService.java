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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public void signup(SignupRequest request) {
        // 1. Verificar se o email já existe para evitar duplicidade
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Erro: Este email já está em uso!");
        }

        TipoUsuario tipoUsuario;
        try {
            tipoUsuario = TipoUsuario.valueOf(request.getTipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Erro: Tipo de usuário inválido!");
        }

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
        novoUsuario.setEmailVerificado(false);
        usuarioRepository.save(novoUsuario);

        // 5. Gerar token de verificação de email e enviar
        gerarTokenVerificacao(novoUsuario);
    }

    /**
     * Gera um novo token de verificação (válido por 24h), persiste e envia o email.
     */
    private void gerarTokenVerificacao(Usuario usuario) {
        String token = UUID.randomUUID().toString().replace("-", "");
        usuario.setTokenVerificacao(token);
        usuario.setTokenVerificacaoExpiraEm(LocalDateTime.now().plusHours(24));
        usuarioRepository.save(usuario);
        emailService.enviarEmailVerificacao(usuario, token);
    }

    /**
     * Confirma o email do usuário a partir do token recebido no link.
     */
    public void verificarEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Token de verificação inválido.");
        }

        Usuario usuario = usuarioRepository.findByTokenVerificacao(token)
                .orElseThrow(() -> new BusinessException("Token de verificação inválido ou já utilizado."));

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            return; // já verificado — nada a fazer
        }

        if (usuario.getTokenVerificacaoExpiraEm() == null
                || usuario.getTokenVerificacaoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Token de verificação expirado. Solicite um novo email.");
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacao(null);
        usuario.setTokenVerificacaoExpiraEm(null);
        usuarioRepository.save(usuario);
    }

    /**
     * Reenvia o email de verificação para contas ainda não confirmadas.
     */
    public void reenviarVerificacao(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Não existe conta com este email."));

        if (!Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            throw new BusinessException("Este email já está verificado. Faça login normalmente.");
        }

        gerarTokenVerificacao(usuario);
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email ou senha inválidos"));

        if (!usuario.getAtivo()) {
            throw new BusinessException("Usuário inativo. Contate o administrador.");
        }

        // null = contas antigas criadas antes da verificação de email existir
        if (Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            throw new BusinessException("Email não verificado. Confira sua caixa de entrada e clique no link de confirmação.");
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