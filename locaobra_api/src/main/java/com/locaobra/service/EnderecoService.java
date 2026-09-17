package com.locaobra.service;

import com.locaobra.dto.request.EnderecoRequest;
import com.locaobra.dto.response.EnderecoResponse;
import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.EnderecoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// CRUD de endereços de um cliente. Sempre operado no contexto de um
// clienteId — tanto faz se quem chama é o próprio cliente (autoatendimento,
// endereço vem do token) ou um funcionário editando o cadastro de alguém
// (endereço vem da URL) — ver ClienteController/EnderecoController.
@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final ClienteRepository clienteRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, ClienteRepository clienteRepository) {
        this.enderecoRepository = enderecoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listarPorCliente(Long clienteId) {
        return enderecoRepository.findByClienteId(clienteId).stream()
                .map(EnderecoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnderecoResponse adicionar(Long clienteId, EnderecoRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + clienteId));

        validar(request);

        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        preencher(endereco, request);

        // Primeiro endereço do cliente vira principal automaticamente,
        // mesmo que não peçam explicitamente — sempre precisa ter um.
        boolean primeiroEndereco = enderecoRepository.countByClienteId(clienteId) == 0;
        endereco.setPrincipal(primeiroEndereco || Boolean.TRUE.equals(request.getPrincipal()));

        if (endereco.getPrincipal()) {
            desmarcarPrincipalAtual(clienteId);
        }

        return EnderecoResponse.from(enderecoRepository.save(endereco));
    }

    @Transactional
    public EnderecoResponse atualizar(Long clienteId, Long enderecoId, EnderecoRequest request) {
        Endereco endereco = buscarDoCliente(clienteId, enderecoId);
        validar(request);
        preencher(endereco, request);

        if (Boolean.TRUE.equals(request.getPrincipal()) && !Boolean.TRUE.equals(endereco.getPrincipal())) {
            desmarcarPrincipalAtual(clienteId);
            endereco.setPrincipal(true);
        }

        return EnderecoResponse.from(enderecoRepository.save(endereco));
    }

    @Transactional
    public void remover(Long clienteId, Long enderecoId) {
        Endereco endereco = buscarDoCliente(clienteId, enderecoId);
        boolean eraPrincipal = Boolean.TRUE.equals(endereco.getPrincipal());
        enderecoRepository.delete(endereco);

        // Se apagou o principal e ainda sobrou algum, promove outro
        // automaticamente pra sempre ter um endereço padrão definido.
        if (eraPrincipal) {
            List<Endereco> restantes = enderecoRepository.findByClienteId(clienteId);
            if (!restantes.isEmpty()) {
                Endereco novoPrincipal = restantes.get(0);
                novoPrincipal.setPrincipal(true);
                enderecoRepository.save(novoPrincipal);
            }
        }
    }

    @Transactional
    public EnderecoResponse definirPrincipal(Long clienteId, Long enderecoId) {
        Endereco endereco = buscarDoCliente(clienteId, enderecoId);
        desmarcarPrincipalAtual(clienteId);
        endereco.setPrincipal(true);
        return EnderecoResponse.from(enderecoRepository.save(endereco));
    }

    // Usado na criação do cliente (endereço já junto no cadastro).
    @Transactional
    public void adicionarSemValidarCliente(Cliente cliente, EnderecoRequest request, boolean primeiroDoCliente) {
        validar(request);
        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        preencher(endereco, request);
        endereco.setPrincipal(primeiroDoCliente || Boolean.TRUE.equals(request.getPrincipal()));
        enderecoRepository.save(endereco);
    }

    // Cria e PERSISTE um endereço "avulso" — sem vínculo de cliente (linha
    // própria na tabela enderecos) — usado por Depósito, Pedido, Expedição e
    // Funcionário. O dono real referencia essa linha por FK. Retorna null se o
    // request vier vazio, pra permitir "sem endereço".
    @Transactional
    public Endereco persistirAvulso(EnderecoRequest request) {
        if (request == null) return null;
        boolean vazio = (request.getRua() == null || request.getRua().isBlank())
                && (request.getCidade() == null || request.getCidade().isBlank())
                && (request.getEstado() == null || request.getEstado().isBlank());
        if (vazio) return null;

        validar(request);
        Endereco endereco = new Endereco();
        endereco.setCliente(null);
        endereco.setPrincipal(false);
        preencher(endereco, request);
        return enderecoRepository.save(endereco);
    }

    // Salva um endereço avulso ATUALIZANDO EM PLACE a linha já vinculada (quando
    // existir) em vez de criar uma nova a cada edição — senão cada "Editar
    // depósito/funcionário" deixaria uma linha órfã na tabela enderecos.
    //   request vazio  → desvincula (retorna null, o dono fica sem endereço)
    //   request nulo   → sem mudança (mantém o atual)
    @Transactional
    public Endereco salvarAvulso(Endereco atual, EnderecoRequest request) {
        if (request == null) return atual;
        boolean vazio = (request.getRua() == null || request.getRua().isBlank())
                && (request.getCidade() == null || request.getCidade().isBlank())
                && (request.getEstado() == null || request.getEstado().isBlank());
        if (vazio) return null;

        validar(request);
        Endereco endereco = (atual != null) ? atual : new Endereco();
        endereco.setCliente(null);
        endereco.setPrincipal(false);
        preencher(endereco, request);
        return enderecoRepository.save(endereco);
    }

    // Faz uma CÓPIA persistida de um endereço existente (sem vínculo de
    // cliente). É a implementação do "snapshot por linha própria": Pedido e
    // Expedição apontam por FK pra essa cópia, então editar/apagar o endereço
    // salvo do cliente depois não altera o histórico.
    @Transactional
    public Endereco copiarAvulso(Endereco origem) {
        if (origem == null) return null;
        Endereco copia = new Endereco();
        copia.setCliente(null);
        copia.setPrincipal(false);
        copia.setCep(origem.getCep());
        copia.setRua(origem.getRua());
        copia.setNumero(origem.getNumero());
        copia.setComplemento(origem.getComplemento());
        copia.setBairro(origem.getBairro());
        copia.setCidade(origem.getCidade());
        copia.setEstado(origem.getEstado());
        return enderecoRepository.save(copia);
    }

    private void desmarcarPrincipalAtual(Long clienteId) {
        enderecoRepository.findByClienteIdAndPrincipalTrue(clienteId)
                .ifPresent(atual -> {
                    atual.setPrincipal(false);
                    enderecoRepository.save(atual);
                });
    }

    private Endereco buscarDoCliente(Long clienteId, Long enderecoId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + enderecoId));
        if (endereco.getCliente() == null || !endereco.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("Esse endereço não pertence a esse cliente.");
        }
        return endereco;
    }

    private void validar(EnderecoRequest request) {
        if (request.getRua() == null || request.getRua().isBlank()) {
            throw new BusinessException("Rua é obrigatória.");
        }
        if (request.getCidade() == null || request.getCidade().isBlank()) {
            throw new BusinessException("Cidade é obrigatória.");
        }
        if (request.getEstado() == null || request.getEstado().isBlank()) {
            throw new BusinessException("Estado é obrigatório.");
        }
    }

    private void preencher(Endereco endereco, EnderecoRequest request) {
        endereco.setApelido(request.getApelido());
        endereco.setCep(request.getCep());
        endereco.setRua(request.getRua());
        endereco.setNumero(request.getNumero());
        endereco.setComplemento(request.getComplemento());
        endereco.setBairro(request.getBairro());
        endereco.setCidade(request.getCidade());
        endereco.setEstado(request.getEstado());
    }
}
