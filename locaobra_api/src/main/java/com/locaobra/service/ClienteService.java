package com.locaobra.service;

import com.locaobra.dto.request.ClienteCreditoRequest;
import com.locaobra.dto.request.ClienteRequest;
import com.locaobra.dto.request.EnderecoRequest;
import com.locaobra.dto.response.ClienteResponse;
import com.locaobra.dto.response.PerfilClienteResponse;
import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;
import com.locaobra.entity.Usuario;
import com.locaobra.enums.SituacaoCredito;
import com.locaobra.enums.StatusPedido;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ClienteRepository;
import com.locaobra.repository.EnderecoRepository;
import com.locaobra.repository.PedidoRepository;
import com.locaobra.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    // Status que ainda "pesam" no limite de crédito do cliente — um pedido
    // recusado/reprovado/cancelado não consome mais nada.
    private static final List<StatusPedido> STATUS_QUE_CONSOMEM_CREDITO =
            List.of(StatusPedido.SOLICITADO, StatusPedido.CONFIRMADO, StatusPedido.APROVADO);

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final EnderecoService enderecoService;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            EnderecoRepository enderecoRepository,
            EnderecoService enderecoService,
            PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.enderecoService = enderecoService;
        this.pedidoRepository = pedidoRepository;
    }

    // Perfil do cliente logado (identificado pelo e-mail do token JWT), já
    // com a lista de endereços salvos — usado pra pré-preencher o checkout
    // de aluguel no catálogo (o cliente escolhe qual usar).
    @Transactional(readOnly = true)
    public PerfilClienteResponse perfilLogado(String email) {
        Long clienteId = resolverClienteIdLogado(email);
        Cliente cliente = findOrThrow(clienteId);
        List<Endereco> enderecos = enderecoRepository.findByClienteId(clienteId);
        return PerfilClienteResponse.from(cliente, enderecos, calcularCreditoUtilizado(clienteId));
    }

    // Soma o valorTotalEstimado de todo pedido do cliente que ainda está em
    // aberto (ver STATUS_QUE_CONSOMEM_CREDITO) — usado pra exibir "crédito
    // utilizado" e pra travar um novo pedido que estouraria o limite
    // (PedidoService.criar()). Calculado na hora, nunca armazenado, pra não
    // ter que sincronizar toda vez que um pedido muda de status.
    @Transactional(readOnly = true)
    public BigDecimal calcularCreditoUtilizado(Long clienteId) {
        BigDecimal soma = pedidoRepository.somarValorEstimadoPorClienteEStatus(clienteId, STATUS_QUE_CONSOMEM_CREDITO);
        return soma != null ? soma : BigDecimal.ZERO;
    }

    // Usado pelo PedidoService antes de criar um pedido novo — barra a
    // criação se o cliente estiver BLOQUEADO ou se o valor do pedido
    // estourar o limite (crédito utilizado + valor do pedido novo > limite).
    // Cliente EM_ANALISE (sem limite definido) também é barrado: não dá pra
    // liberar valor pra quem ainda não foi avaliado.
    @Transactional(readOnly = true)
    public void validarCreditoParaNovoPedido(Cliente cliente, BigDecimal valorPedido) {
        if (cliente.getSituacaoCredito() != SituacaoCredito.LIBERADO) {
            throw new BusinessException(
                    "Seu cadastro ainda não está liberado para pedidos (situação de crédito: "
                            + cliente.getSituacaoCredito() + "). Fale com nossa equipe.");
        }
        if (cliente.getLimiteCredito() == null) {
            throw new BusinessException("Seu limite de crédito ainda não foi definido. Fale com nossa equipe.");
        }
        BigDecimal utilizado = calcularCreditoUtilizado(cliente.getId());
        if (utilizado.add(valorPedido).compareTo(cliente.getLimiteCredito()) > 0) {
            BigDecimal disponivel = cliente.getLimiteCredito().subtract(utilizado);
            throw new BusinessException(
                    "Esse pedido excede seu limite de crédito disponível (R$ " + disponivel + ").");
        }
    }

    // Edição do crédito do cliente (limite, situação, observações) — feita
    // por um analista de credenciamento/financeiro, não pelo mesmo endpoint
    // de cadastro (ver ClienteCreditoRequest e SecurityConfig).
    @Transactional
    public ClienteResponse atualizarCredito(Long id, ClienteCreditoRequest request) {
        Cliente cliente = findOrThrow(id);
        cliente.setSituacaoCredito(request.getSituacaoCredito());
        cliente.setLimiteCredito(request.getLimiteCredito());
        cliente.setObservacoesCredito(request.getObservacoesCredito());
        clienteRepository.save(cliente);
        return buscarPorId(id);
    }

    // Descobre o id do Cliente por trás do usuário logado — usado por outros
    // services (ex.: EnderecoController) pra resolver "meu cliente" a partir
    // do token, sem duplicar a leitura de Usuario em cada lugar.
    @Transactional(readOnly = true)
    public Long resolverClienteIdLogado(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (usuario.getIdCliente() == null) {
            throw new BusinessException("Esse usuário não está vinculado a um cadastro de cliente.");
        }
        return usuario.getIdCliente();
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
        cliente = clienteRepository.save(cliente);

        // Endereço já vem junto no cadastro, feito pelo funcionário/admin.
        List<EnderecoRequest> enderecos = request.getEnderecos();
        if (enderecos != null) {
            for (int i = 0; i < enderecos.size(); i++) {
                enderecoService.adicionarSemValidarCliente(cliente, enderecos.get(i), i == 0);
            }
        }

        return buscarPorId(cliente.getId());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(c -> ClienteResponse.from(c, enderecoRepository.findByClienteId(c.getId()), calcularCreditoUtilizado(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarAtivos() {
        return clienteRepository.findByAtivoTrue()
                .stream()
                .map(c -> ClienteResponse.from(c, enderecoRepository.findByClienteId(c.getId()), calcularCreditoUtilizado(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        Cliente cliente = findOrThrow(id);
        return ClienteResponse.from(cliente, enderecoRepository.findByClienteId(id), calcularCreditoUtilizado(id));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(c -> ClienteResponse.from(c, enderecoRepository.findByClienteId(c.getId()), calcularCreditoUtilizado(c.getId())))
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
        clienteRepository.save(cliente);
        return buscarPorId(id);
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
