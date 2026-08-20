package com.locaobra.config;

import com.locaobra.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);
        String cargoDoToken = jwtService.extractCargo(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                if (usuario.getAtivo()) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    // Autoridade de tipo (ADMIN / FUNCIONARIO / CLIENTE)
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getTipo().name()));
                    // Autoridade de cargo (ENTREGADOR / CONFERENTE / TECNICO_MANUTENCAO / etc.),
                    // extraída do token — permite travar rotas por função específica.
                    if (cargoDoToken != null && !cargoDoToken.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + cargoDoToken));
                    }

                    var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}