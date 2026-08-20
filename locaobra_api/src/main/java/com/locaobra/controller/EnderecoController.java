package com.locaobra.controller;

import com.locaobra.entity.Endereco;
import com.locaobra.entity.Usuario;
import com.locaobra.dto.EnderecoDTO;
import com.locaobra.config.JwtService;
import com.locaobra.repository.UsuarioRepository;
import com.locaobra.service.EnderecoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/v1") // Raiz da API
public class EnderecoController {

    @Autowired
    private EnderecoService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // --- Endpoints Gerais para Endereços (se ainda forem necessários) ---

    @GetMapping("/enderecos")
    public ResponseEntity<List<EnderecoDTO>> listarEnderecos() {
        return ResponseEntity.ok(service.listarEnderecos());
    }

    @GetMapping("/enderecos/{id}")
    public ResponseEntity<EnderecoDTO> get(@PathVariable("id") Long id) {
        return service.getEnderecoById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Este endpoint agora é mais específico para criação geral, se necessário.
    // O endpoint principal para criação associada ao usuário é /endereco
    @PostMapping("/enderecos")
    public ResponseEntity<Endereco> incluir(@RequestBody Endereco endereco) {
        Endereco novo = service.incluir(endereco);
        return ResponseEntity.status(201).body(novo);
    }

    @PutMapping("/enderecos/{id}")
    public ResponseEntity<Endereco> atualizar(@PathVariable Long id, @RequestBody Endereco endereco) {
        Endereco atualizado = service.atualizar(id, endereco);
        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/enderecos/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        boolean deletado = service.deletar(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Novo Endpoint: Cadastrar Endereço e Associar ao Usuário Logado ---

    /**
     * Endpoint para cadastrar um endereço e associá-lo ao usuário logado (Cliente ou Funcionario).
     * O usuário é identificado pelo token JWT.
     */
    @PostMapping("/endereco") // Endpoint específico para o usuário logado
    public ResponseEntity<?> cadastrarEnderecoUsuarioLogado(
            @RequestBody Endereco endereco,
            HttpServletRequest request) {

        try {
            // 1. Extrair o token do cabeçalho Authorization
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(401).body("Token não fornecido.");
            }

            // 2. Validar o token e extrair o email
            String email;
            try {
                if (!jwtService.validateToken(token, jwtService.extractEmail(token))) {
                    return ResponseEntity.status(401).body("Token inválido ou expirado.");
                }
                email = jwtService  .extractEmail(token);
            } catch (Exception e) {
                return ResponseEntity.status(401).body("Token inválido: " + e.getMessage());
            }

            // 3. Buscar o usuário pelo email
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Usuário não encontrado com o email: " + email);
            }
            Usuario usuario = usuarioOpt.get();

            // 4. Salvar o novo endereço
            Endereco novoEndereco = service.incluir(endereco);
            Long idNovoEndereco = novoEndereco.getId_endereco(); // getId() retorna id_endereco

            // 5. Associar o endereço diretamente ao usuário logado
            usuario.setIdEndereco(idNovoEndereco);
            usuarioRepository.save(usuario);

            // 6. Retornar sucesso com o DTO do endereço criado
            // Certifique-se de que o construtor do EnderecoDTO corresponde aos campos
            EnderecoDTO enderecoDTO = new EnderecoDTO(
                    novoEndereco.getId_endereco(), // id_endereco
                    novoEndereco.getCep(),
                    novoEndereco.getRua(),
                    novoEndereco.getBairro(),
                    novoEndereco.getCidade(),
                    novoEndereco.getEstado(),
                    novoEndereco.getComplemento(),
                    novoEndereco.getNumero()
            );

            return ResponseEntity.ok(enderecoDTO);

        } catch (Exception e) {
            // Log do erro para depuração (opcional, use logger em produção)
            // e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao cadastrar endereço: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para extrair o token Bearer do cabeçalho Authorization.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer "
        }
        return null;
    }

}