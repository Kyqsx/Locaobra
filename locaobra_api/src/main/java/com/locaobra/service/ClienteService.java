package com.locaobra.service;

import com.locaobra.dto.EnderecoDTO;
import com.locaobra.dto.request.ClienteRequest;
import com.locaobra.dto.response.ClienteResponse;
import com.locaobra.dto.response.PerfilClienteResponse;
import com.locaobra.entity.Cliente;
import com.locaobra.entity.Usuario;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.EnderecoRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;

    public ClienteService(ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            EnderecoRepository enderecoRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
    }

    // Perfil do cliente logado (identificado pelo e-mail do token JWT),
    // já com o endereço cadastrado formatado — usado pra pré-preencher o
    // checkout de aluguel no catálogo.
    @Transactional(readOnly = true)
    public PerfilClienteResponse perfilLogado(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (usuario.getIdCliente() == null) {
            throw new BusinessException("Esse usuário não está vinculado a um cadastro de cliente.");
        }
        Cliente cliente = findOrThrow(usuario.getIdCliente());

        String enderecoFormatado = null;
        if (usuario.getIdEndereco() != null) {
            enderecoFormatado = enderecoRepository.findBasicById(usuario.getIdEndereco())
                    .map(this::formatarEndereco)
                    .orElse(null);
        }

        return PerfilClienteResponse.from(cliente, enderecoFormatado, usuario.getIdEndereco());
    }

    private String formatarEndereco(EnderecoDTO e) {
        StringBuilder sb = new StringBuilder();
        if (e.getRua() != null && !e.getRua().isBlank()) sb.append(e.getRua());
        if (e.getNumero() != null && !e.getNumero().isBlank()) sb.append(", ").append(e.getNumero());
        if (e.getBairro() != null && !e.getBairro().isBlank()) sb.append(" - ").append(e.getBairro());
        if (e.getCidade() != null && !e.getCidade().isBlank()) sb.append(", ").append(e.getCidade());
        if (e.getEstado() != null && !e.getEstado().isBlank()) sb.append("/").append(e.getEstado());
        if (e.getCep() != null && !e.getCep().isBlank()) sb.append(" - CEP ").append(e.getCep());
        return sb.toString();
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
