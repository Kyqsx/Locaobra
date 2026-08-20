package com.locaobra.controller;

import com.locaobra.dto.request.LoginRequest;
import com.locaobra.dto.request.SignupRequest; // Você precisará criar este DTO
import com.locaobra.dto.response.LoginResponse;
import com.locaobra.service.AuthService;
import com.locaobra.config.JwtService;
import com.locaobra.entity.Funcionario;
import com.locaobra.enums.TipoUsuario;
import com.locaobra.repository.FuncionarioRepository;
import com.locaobra.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public AuthController(AuthService authService, JwtService jwtService, UsuarioRepository usuarioRepository,
                          FuncionarioRepository funcionarioRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuário cadastrado com sucesso!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = (String) auth.getPrincipal();

            return usuarioRepository.findByEmail(email)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("logado", true);
                    response.put("email", user.getEmail());
                    response.put("nome", user.getNome());
                    response.put("tipo", user.getTipo());
                    response.put("id", user.getId());
                    response.put("idFuncionario", user.getIdFuncionario());

                    if (user.getTipo() == TipoUsuario.FUNCIONARIO && user.getIdFuncionario() != null) {
                        funcionarioRepository.findById(user.getIdFuncionario()).ifPresent(funcionario -> {
                            response.put("cargoFuncionario", funcionario.getCargo() != null ? funcionario.getCargo().getNome() : null);
                            response.put("funcionarioAtivo", funcionario.getStatus());
                        });
                    }

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}