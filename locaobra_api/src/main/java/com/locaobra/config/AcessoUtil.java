package com.locaobra.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Consulta rápida aos papéis do usuário logado, para rotas públicas que precisam
 * devolver mais (ou menos) dados conforme quem chama — ex.: catálogo e blog.
 * O JwtAuthFilter roda antes e preenche o contexto mesmo em rotas permitAll,
 * desde que o token seja válido.
 */
public final class AcessoUtil {

    private AcessoUtil() {
    }

    /** true se o usuário autenticado tiver QUALQUER um dos papéis (sem o prefixo ROLE_). */
    public static boolean temPapel(String... papeis) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority autoridade : auth.getAuthorities()) {
            for (String papel : papeis) {
                if (("ROLE_" + papel).equals(autoridade.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
