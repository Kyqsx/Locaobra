package com.locaobra.service;

import com.locaobra.dto.response.DashboardResponse;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DashboardService {

    private final ClienteRepository clienteRepository;

    private final UsuarioRepository usuarioRepository;

    public DashboardService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDados() {
        DashboardResponse dash = new DashboardResponse();

        // Clientes
        dash.setTotalClientes(clienteRepository.count());
        // Usuários
        dash.setTotalUsuarios(usuarioRepository.count());

        return dash;
    }
}
