package com.locaobra.service;

import com.locaobra.dto.response.DashboardResponse;
import com.locaobra.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DashboardService {

    private final ClienteRepository clienteRepository;

    public DashboardService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDados() {
        DashboardResponse dash = new DashboardResponse();

        // Clientes
        dash.setTotalClientes(clienteRepository.count());

        return dash;
    }
}
