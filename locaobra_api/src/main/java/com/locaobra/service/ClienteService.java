package com.locaobra.service;

import com.locaobra.dto.request.ClienteRequest;
import com.locaobra.dto.response.ClienteResponse;
import com.locaobra.entity.Cliente;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        if (clienteRepository.existsByCpfCnpj(request.getCpfCnpj())) {
            throw new BusinessException("CPF/CNPJ já cadastrado: " + request.getCpfCnpj());
        }
        if (clienteRepository.existsByTelefone(request.getTelefone())) {
            throw new BusinessException("Telefone já cadastrado: " + request.getTelefone());
        }

        Cliente cliente = new Cliente();
        preencherCliente(cliente, request);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarAtivos() {
        return clienteRepository.findByAtivoTrue()
                .stream()
                .map(ClienteResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(ClienteResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = findOrThrow(id);

        if (!cliente.getCpfCnpj().equals(request.getCpfCnpj()) &&
                clienteRepository.existsByCpfCnpj(request.getCpfCnpj())) {
            throw new BusinessException("CPF/CNPJ já cadastrado: " + request.getCpfCnpj());
        }
        if (!cliente.getTelefone().equals(request.getTelefone()) &&
                clienteRepository.existsByTelefone(request.getTelefone())) {
            throw new BusinessException("Telefone já cadastrado: " + request.getTelefone());
        }

        preencherCliente(cliente, request);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void desativar(Long id) {
        Cliente cliente = findOrThrow(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional
    public void deletar(Long id) {
        findOrThrow(id);
        clienteRepository.deleteById(id);
    }

    private void preencherCliente(Cliente cliente, ClienteRequest request) {
        cliente.setNome(request.getNome());
        cliente.setCpfCnpj(request.getCpfCnpj());
        cliente.setTelefone(request.getTelefone());
    }

    public Cliente findOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }
}
