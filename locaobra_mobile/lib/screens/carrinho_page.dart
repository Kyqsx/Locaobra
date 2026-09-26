import 'dart:async';

import 'package:flutter/material.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/auth/login_page.dart';
import 'package:locaobra_mobile/cart/cart_item.dart';
import 'package:locaobra_mobile/cart/cart_state.dart';
import 'package:locaobra_mobile/cart/dados_checkout.dart';
import 'package:locaobra_mobile/models/endereco.dart';
import 'package:locaobra_mobile/models/frete_estimativa.dart';
import 'package:locaobra_mobile/models/ponto_retirada.dart';
import 'package:locaobra_mobile/screens/pagamento_page.dart';
import 'package:locaobra_mobile/services/api_client.dart';
import 'package:locaobra_mobile/services/endereco_service.dart';
import 'package:locaobra_mobile/services/pedido_service.dart';
import 'package:locaobra_mobile/utils/formatters.dart';

/// Carrinho + Checkout num patch só (como no web): revisar os itens e
/// enviar um único pedido de orçamento — equivalente a
/// `Cart/carrinho.jsx` + `CartContext.jsx`.
///
/// Pedido é coisa de cliente logado: a tela exige login ao abrir (mesma
/// regra do `ClienteRoute` do web) e devolve pra tela anterior se a pessoa
/// não completar o login.
class CarrinhoPage extends StatefulWidget {
  const CarrinhoPage({super.key});

  @override
  State<CarrinhoPage> createState() => _CarrinhoPageState();
}

class _CarrinhoPageState extends State<CarrinhoPage> {
  final EnderecoService _enderecoService = EnderecoService();
  final PedidoService _pedidoService = PedidoService();

  final TextEditingController _cepCtrl = TextEditingController();
  final TextEditingController _ruaCtrl = TextEditingController();
  final TextEditingController _numeroCtrl = TextEditingController();
  final TextEditingController _complementoCtrl = TextEditingController();
  final TextEditingController _bairroCtrl = TextEditingController();
  final TextEditingController _cidadeCtrl = TextEditingController();
  final TextEditingController _estadoCtrl = TextEditingController();
  final TextEditingController _observacoesCtrl = TextEditingController();

  late DateTime _dataInicio;
  late DateTime _dataFim;

  TipoEntrega _tipoEntrega = TipoEntrega.entrega;

  bool _carregandoEnderecos = true;
  List<Endereco> _enderecosSalvos = const [];
  int? _enderecoSelecionadoId; // null = usando o formulário de endereço novo

  bool _carregandoPontos = false;
  List<PontoRetirada> _pontosRetirada = const [];
  int? _pontoRetiradaId;

  FreteEstimativa? _frete;
  bool _carregandoFrete = false;
  String? _erroFrete;
  Timer? _debounceFrete;
  int _estimativaId = 0;

  bool _verificandoLogin = true;
  String? _erro;
  PedidoCriado? _pedidoCriado;

  @override
  void initState() {
    super.initState();
    final hoje = DateTime.now();
    _dataInicio = DateTime(hoje.year, hoje.month, hoje.day);
    _dataFim = _dataInicio.add(const Duration(days: 1));

    for (final ctrl in [_cepCtrl, _ruaCtrl, _numeroCtrl, _complementoCtrl, _bairroCtrl, _cidadeCtrl, _estadoCtrl]) {
      ctrl.addListener(_onEnderecoNovoMudou);
    }

    WidgetsBinding.instance.addPostFrameCallback((_) => _garantirLogin());
  }

  @override
  void dispose() {
    _debounceFrete?.cancel();
    for (final ctrl in [
      _cepCtrl,
      _ruaCtrl,
      _numeroCtrl,
      _complementoCtrl,
      _bairroCtrl,
      _cidadeCtrl,
      _estadoCtrl,
      _observacoesCtrl,
    ]) {
      ctrl.dispose();
    }
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Carregamento
  // ---------------------------------------------------------------------------

  /// Pedido é coisa de cliente logado — igual ao ClienteRoute do web.
  /// Se a pessoa não estava logada e não completa o login, volta pra tela
  /// de onde veio.
  Future<void> _garantirLogin() async {
    if (AuthState.estaLogado) {
      setState(() => _verificandoLogin = false);
      _carregarEnderecos();
      return;
    }

    final logou = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => const LoginPage(voltarAoConcluir: true)),
    );
    if (!mounted) return;

    if (logou != true || !AuthState.estaLogado) {
      Navigator.of(context).pop();
      return;
    }
    setState(() => _verificandoLogin = false);
    _carregarEnderecos();
  }

  Future<void> _carregarEnderecos() async {
    setState(() => _carregandoEnderecos = true);
    try {
      final enderecos = await _enderecoService.listarMeus();
      if (!mounted) return;
      final principal = enderecos.where((e) => e.principal).toList();
      setState(() {
        _enderecosSalvos = enderecos;
        _enderecoSelecionadoId = principal.isNotEmpty
            ? principal.first.id
            : (enderecos.isNotEmpty ? enderecos.first.id : null);
        _carregandoEnderecos = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _enderecosSalvos = const [];
        _enderecoSelecionadoId = null;
        _carregandoEnderecos = false;
      });
    }
    _agendarRecalculoFrete(imediato: true);
  }

  Future<void> _carregarPontosRetirada() async {
    if (_pontosRetirada.isNotEmpty || _carregandoPontos) return;
    setState(() => _carregandoPontos = true);
    final pontos = await _pedidoService.listarPontosRetirada();
    if (!mounted) return;
    setState(() {
      _pontosRetirada = pontos;
      _carregandoPontos = false;
    });
  }

  // ---------------------------------------------------------------------------
  // Cálculos
  // ---------------------------------------------------------------------------

  int get _dias => _dataFim.difference(_dataInicio).inDays.clamp(1, 3650);

  double _valorItens(List<CartItem> itens) =>
      itens.fold<double>(0, (soma, i) => soma + i.valorDiaria * i.quantidade * _dias);

  double get _valorFrete =>
      _tipoEntrega == TipoEntrega.entrega ? (_frete?.valorFrete ?? 0) : 0;

  Endereco get _enderecoNovo => Endereco(
        cep: _cepCtrl.text,
        rua: _ruaCtrl.text,
        numero: _numeroCtrl.text,
        complemento: _complementoCtrl.text,
        bairro: _bairroCtrl.text,
        cidade: _cidadeCtrl.text,
        estado: _estadoCtrl.text,
      );

  bool get _usandoEnderecoSalvo =>
      _enderecoSelecionadoId != null &&
      _enderecosSalvos.any((e) => e.id == _enderecoSelecionadoId);

  void _onEnderecoNovoMudou() {
    if (_usandoEnderecoSalvo) return;
    _agendarRecalculoFrete();
  }

  void _agendarRecalculoFrete({bool imediato = false}) {
    _debounceFrete?.cancel();

    final itens = CartState.itens.value;
    if (_tipoEntrega != TipoEntrega.entrega || itens.isEmpty) {
      setState(() {
        _frete = null;
        _erroFrete = null;
        _carregandoFrete = false;
      });
      return;
    }

    Endereco? enderecoDestino;
    int? enderecoId;
    if (_usandoEnderecoSalvo) {
      enderecoId = _enderecoSelecionadoId;
      enderecoDestino = _enderecosSalvos.firstWhere((e) => e.id == enderecoId);
    } else {
      enderecoDestino = _enderecoNovo;
    }

    if (!enderecoDestino.temMinimoParaFrete) {
      setState(() {
        _frete = null;
        _erroFrete = null;
        _carregandoFrete = false;
      });
      return;
    }

    final id = ++_estimativaId;
    setState(() {
      _carregandoFrete = true;
      _erroFrete = null;
    });

    _debounceFrete = Timer(Duration(milliseconds: imediato ? 0 : 500), () async {
      final resultado = await _pedidoService.estimarFrete(
        dataInicio: _dataInicio,
        dataFim: _dataFim,
        enderecoId: enderecoId,
        enderecoNovo: enderecoId == null ? enderecoDestino : null,
        itens: itens,
      );
      if (!mounted || _estimativaId != id) return;
      setState(() {
        _carregandoFrete = false;
        if (resultado != null) {
          _frete = resultado;
          _erroFrete = null;
        } else {
          _frete = null;
          _erroFrete = 'Não foi possível calcular o frete agora. Você ainda pode enviar '
              'o pedido — o valor final sai com o consultor.';
        }
      });
    });
  }

  // ---------------------------------------------------------------------------
  // Datas
  // ---------------------------------------------------------------------------

  Future<void> _escolherDataInicio() async {
    final hoje = DateTime.now();
    final selecionada = await showDatePicker(
      context: context,
      initialDate: _dataInicio,
      firstDate: DateTime(hoje.year, hoje.month, hoje.day),
      lastDate: DateTime(hoje.year + 2),
    );
    if (selecionada == null) return;
    setState(() {
      _dataInicio = selecionada;
      if (!_dataFim.isAfter(_dataInicio)) {
        _dataFim = _dataInicio.add(const Duration(days: 1));
      }
    });
    _agendarRecalculoFrete(imediato: true);
  }

  Future<void> _escolherDataFim() async {
    final selecionada = await showDatePicker(
      context: context,
      initialDate: _dataFim.isAfter(_dataInicio) ? _dataFim : _dataInicio.add(const Duration(days: 1)),
      firstDate: _dataInicio,
      lastDate: DateTime(_dataInicio.year + 2),
    );
    if (selecionada == null) return;
    setState(() => _dataFim = selecionada);
    _agendarRecalculoFrete(imediato: true);
  }

  // ---------------------------------------------------------------------------
  // Envio — valida e manda pra tela de pagamento simulado, que é quem de
  // fato chama PedidoService.criar (com a forma de pagamento escolhida) e
  // limpa o carrinho no sucesso.
  // ---------------------------------------------------------------------------

  Future<void> _irParaPagamento() async {
    final itens = CartState.itens.value;
    setState(() => _erro = null);

    if (itens.isEmpty) {
      setState(() => _erro = 'Seu carrinho está vazio. Adicione ao menos um equipamento.');
      return;
    }
    if (!_dataFim.isAfter(_dataInicio)) {
      setState(() => _erro = 'A data de fim não pode ser anterior ou igual à data de início.');
      return;
    }
    if (_tipoEntrega == TipoEntrega.entrega &&
        !_usandoEnderecoSalvo &&
        (_ruaCtrl.text.trim().isEmpty || _cidadeCtrl.text.trim().isEmpty || _estadoCtrl.text.trim().isEmpty)) {
      setState(() => _erro = 'Preencha ao menos rua, cidade e UF do endereço de entrega.');
      return;
    }

    String? observacoes = _observacoesCtrl.text.trim();
    if (_tipoEntrega == TipoEntrega.retirada && _pontoRetiradaId != null) {
      final ponto = _pontosRetirada.where((p) => p.id == _pontoRetiradaId).toList();
      if (ponto.isNotEmpty) {
        observacoes = [observacoes, 'Retirada no depósito ${ponto.first.nome}.']
            .where((s) => s.isNotEmpty)
            .join(' ');
      }
    }

    final valorTotal = _valorItens(itens) + _valorFrete;

    final pedido = await Navigator.of(context).push<PedidoCriado>(
      MaterialPageRoute(
        builder: (_) => PagamentoPage(
          dados: DadosCheckout(
            dataInicio: _dataInicio,
            dataFim: _dataFim,
            tipoEntrega: _tipoEntrega,
            enderecoId: _tipoEntrega == TipoEntrega.entrega && _usandoEnderecoSalvo
                ? _enderecoSelecionadoId
                : null,
            enderecoNovo: _tipoEntrega == TipoEntrega.entrega && !_usandoEnderecoSalvo
                ? _enderecoNovo
                : null,
            observacoesCliente: observacoes,
            itens: itens,
            valorTotal: valorTotal,
          ),
        ),
      ),
    );
    if (!mounted || pedido == null) return;
    setState(() => _pedidoCriado = pedido);
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    if (_verificandoLogin) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('Seu carrinho'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SafeArea(
        child: _pedidoCriado != null
            ? _buildSucesso(_pedidoCriado!)
            : ValueListenableBuilder<List<CartItem>>(
                valueListenable: CartState.itens,
                builder: (context, itens, _) {
                  if (itens.isEmpty) return _buildVazio();
                  return _buildConteudo(itens);
                },
              ),
      ),
    );
  }

  Widget _buildVazio() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.shopping_cart_outlined, size: 56, color: Colors.grey),
            const SizedBox(height: 12),
            const Text('Seu carrinho está vazio no momento.', textAlign: TextAlign.center),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                foregroundColor: Colors.white,
              ),
              child: const Text('Ver catálogo'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSucesso(PedidoCriado pedido) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('✅', style: TextStyle(fontSize: 48)),
            const SizedBox(height: 12),
            const Text('Pedido enviado!', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text(
              'Seu orçamento ${pedido.codigo} foi enviado e está aguardando revisão da '
              'nossa equipe. Você pode acompanhar o status a qualquer momento em '
              '"Meus Pedidos".',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade700),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                // A tela "Meus Pedidos" chega num próximo patch — por
                // enquanto o botão só volta pra Home.
                child: const Text('Continuar comprando'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConteudo(List<CartItem> itens) {
    final totalQuantidade = itens.fold<int>(0, (n, i) => n + i.quantidade);
    final valorItens = _valorItens(itens);
    final valorTotal = valorItens + _valorFrete;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ...itens.map((item) => _buildItemCarrinho(item)),
        const SizedBox(height: 8),
        _Cartao(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Dados do pedido', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              if (_erro != null) ...[
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.red.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.red.shade200),
                  ),
                  child: Text(_erro!, style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
                ),
                const SizedBox(height: 12),
              ],
              Row(
                children: [
                  Expanded(child: _buildCampoData('Data de início', _dataInicio, _escolherDataInicio)),
                  const SizedBox(width: 12),
                  Expanded(child: _buildCampoData('Data de fim', _dataFim, _escolherDataFim)),
                ],
              ),
              const SizedBox(height: 16),
              const Text('Como você quer receber?', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              _buildOpcaoEntrega(
                selecionado: _tipoEntrega == TipoEntrega.entrega,
                titulo: '🚚 Entregar no meu endereço',
                detalhe: 'Levamos até a obra (frete calculado abaixo)',
                onTap: () {
                  setState(() => _tipoEntrega = TipoEntrega.entrega);
                  _agendarRecalculoFrete(imediato: true);
                },
              ),
              const SizedBox(height: 8),
              _buildOpcaoEntrega(
                selecionado: _tipoEntrega == TipoEntrega.retirada,
                titulo: '🏬 Retirar no depósito',
                detalhe: 'Você busca e devolve no ponto escolhido — sem frete',
                onTap: () {
                  setState(() => _tipoEntrega = TipoEntrega.retirada);
                  _agendarRecalculoFrete(imediato: true);
                  _carregarPontosRetirada();
                },
              ),
              const SizedBox(height: 16),
              if (_tipoEntrega == TipoEntrega.retirada)
                _buildSecaoRetirada()
              else
                _buildSecaoEntrega(),
              const SizedBox(height: 16),
              const Text('Observações gerais (opcional)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
              const SizedBox(height: 6),
              TextField(
                controller: _observacoesCtrl,
                maxLines: 2,
                decoration: const InputDecoration(
                  hintText: 'Alguma informação adicional para o consultor?',
                  border: OutlineInputBorder(),
                  isDense: true,
                ),
              ),
              const Divider(height: 32),
              _buildLinhaResumo(
                '$totalQuantidade ite${totalQuantidade > 1 ? 'ns' : 'm'} × $_dias dia${_dias > 1 ? 's' : ''}',
                formatarMoeda(valorItens),
              ),
              const SizedBox(height: 4),
              if (_tipoEntrega == TipoEntrega.entrega)
                _buildLinhaResumo(
                  'Frete (estimado)'
                  '${_carregandoFrete ? ' · calculando...' : ''}'
                  '${_frete?.distanciaKm != null && !_carregandoFrete ? ' · ~${_frete!.distanciaKm} km · até ${_frete!.prazoEstimadoDias} dia${(_frete!.prazoEstimadoDias ?? 0) > 1 ? 's' : ''} úteis' : ''}',
                  _carregandoFrete ? '...' : (_frete != null ? formatarMoeda(_valorFrete) : 'a calcular'),
                )
              else
                _buildLinhaResumo('Retirada no depósito', 'Grátis'),
              if (_erroFrete != null) ...[
                const SizedBox(height: 6),
                Text(_erroFrete!, style: TextStyle(color: Colors.orange.shade800, fontSize: 12)),
              ],
              const Divider(height: 24),
              _buildLinhaResumo('Total estimado', formatarMoeda(valorTotal), destaque: true),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _irParaPagamento,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  child: const Text('Ir para pagamento'),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildItemCarrinho(CartItem item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: _Cartao(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: SizedBox(
                width: 72,
                height: 72,
                child: item.imagem != null && item.imagem!.isNotEmpty
                    ? Image.network(
                        ApiClient.resolveUrl(item.imagem),
                        fit: BoxFit.cover,
                        alignment: Alignment(item.focoX / 50 - 1, item.focoY / 50 - 1),
                        errorBuilder: (_, __, ___) => Container(
                          color: Colors.grey.shade100,
                          alignment: Alignment.center,
                          child: const Text('📐', style: TextStyle(fontSize: 24)),
                        ),
                      )
                    : Container(
                        color: Colors.grey.shade100,
                        alignment: Alignment.center,
                        child: const Text('📐', style: TextStyle(fontSize: 24)),
                      ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.nome, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  Text('${formatarMoeda(item.valorDiaria)} / diária',
                      style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _SeletorQuantidade(
                        valor: item.quantidade,
                        maximo: item.quantidadeDisponivel < 1 ? 1 : item.quantidadeDisponivel,
                        onChanged: (nova) {
                          CartState.atualizarQuantidade(item.equipamentoId, nova);
                          _agendarRecalculoFrete(imediato: true);
                        },
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          '${item.quantidadeDisponivel} disponíve${item.quantidadeDisponivel == 1 ? 'l' : 'is'}',
                          style: TextStyle(color: Colors.grey.shade500, fontSize: 11),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  TextFormField(
                    initialValue: item.observacaoItem,
                    onChanged: (v) => CartState.atualizarObservacao(item.equipamentoId, v),
                    style: const TextStyle(fontSize: 12),
                    decoration: const InputDecoration(
                      isDense: true,
                      hintText: 'Observação para esse item (opcional)',
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  formatarMoeda(item.valorDiaria * item.quantidade * _dias),
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
                const SizedBox(height: 8),
                InkWell(
                  onTap: () {
                    CartState.removerItem(item.equipamentoId);
                    _agendarRecalculoFrete(imediato: true);
                  },
                  child: Text('Remover', style: TextStyle(color: Colors.red.shade400, fontSize: 12)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCampoData(String rotulo, DateTime valor, VoidCallback onTap) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(rotulo, style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
        const SizedBox(height: 4),
        InkWell(
          onTap: onTap,
          child: InputDecorator(
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              isDense: true,
              contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            ),
            child: Text(formatarData(valor), style: const TextStyle(fontSize: 13)),
          ),
        ),
      ],
    );
  }

  Widget _buildOpcaoEntrega({
    required bool selecionado,
    required String titulo,
    required String detalhe,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: selecionado ? Colors.orange.shade50 : Colors.white,
          border: Border.all(color: selecionado ? Colors.orange : Colors.grey.shade300),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          children: [
            Icon(
              selecionado ? Icons.radio_button_checked : Icons.radio_button_off,
              color: selecionado ? Colors.orange : Colors.grey,
              size: 20,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titulo, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  Text(detalhe, style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSecaoRetirada() {
    if (_carregandoPontos) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_pontosRetirada.isEmpty) {
      return Text(
        'Nenhum ponto de retirada disponível no momento — o consultor vai combinar o local com você.',
        style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Onde você vai retirar?', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        for (final ponto in _pontosRetirada)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _buildOpcaoEntrega(
              selecionado: _pontoRetiradaId == ponto.id,
              titulo: ponto.nome,
              detalhe: ponto.endereco?.resumo ?? '---',
              onTap: () => setState(() => _pontoRetiradaId = ponto.id),
            ),
          ),
      ],
    );
  }

  Widget _buildSecaoEntrega() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Endereço de entrega', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        if (_carregandoEnderecos)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: Center(child: CircularProgressIndicator()),
          )
        else ...[
          for (final endereco in _enderecosSalvos)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: _buildOpcaoEntrega(
                selecionado: _enderecoSelecionadoId == endereco.id,
                titulo: (endereco.apelido.isEmpty ? 'Endereço' : endereco.apelido) +
                    (endereco.principal ? ' · Principal' : ''),
                detalhe: endereco.resumo,
                onTap: () {
                  setState(() => _enderecoSelecionadoId = endereco.id);
                  _agendarRecalculoFrete(imediato: true);
                },
              ),
            ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _buildOpcaoEntrega(
              selecionado: !_usandoEnderecoSalvo,
              titulo: 'Usar outro endereço',
              detalhe: 'Digite um endereço só para esta locação',
              onTap: () {
                setState(() => _enderecoSelecionadoId = null);
                _agendarRecalculoFrete();
              },
            ),
          ),
          if (!_usandoEnderecoSalvo) ...[
            const SizedBox(height: 4),
            _buildCampoTexto('CEP', _cepCtrl),
            _buildCampoTexto('Rua *', _ruaCtrl),
            Row(
              children: [
                Expanded(child: _buildCampoTexto('Número', _numeroCtrl)),
                const SizedBox(width: 8),
                Expanded(child: _buildCampoTexto('Complemento', _complementoCtrl)),
              ],
            ),
            _buildCampoTexto('Bairro', _bairroCtrl),
            Row(
              children: [
                Expanded(flex: 3, child: _buildCampoTexto('Cidade *', _cidadeCtrl)),
                const SizedBox(width: 8),
                Expanded(
                  flex: 1,
                  child: _buildCampoTexto('UF *', _estadoCtrl, maxLength: 2, maiusculo: true),
                ),
              ],
            ),
          ],
        ],
      ],
    );
  }

  Widget _buildCampoTexto(
    String rotulo,
    TextEditingController controller, {
    int? maxLength,
    bool maiusculo = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextField(
        controller: controller,
        maxLength: maxLength,
        textCapitalization: maiusculo ? TextCapitalization.characters : TextCapitalization.words,
        decoration: InputDecoration(
          labelText: rotulo,
          border: const OutlineInputBorder(),
          isDense: true,
          counterText: '',
          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        ),
      ),
    );
  }

  Widget _buildLinhaResumo(String rotulo, String valor, {bool destaque = false}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Text(
            rotulo,
            style: TextStyle(
              fontSize: destaque ? 15 : 13,
              fontWeight: destaque ? FontWeight.bold : FontWeight.normal,
              color: Colors.grey.shade700,
            ),
          ),
        ),
        Text(
          valor,
          style: TextStyle(
            fontSize: destaque ? 17 : 13,
            fontWeight: FontWeight.bold,
            color: destaque ? Colors.orange : Colors.black87,
          ),
        ),
      ],
    );
  }
}

class _Cartao extends StatelessWidget {
  final Widget child;
  const _Cartao({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: child,
    );
  }
}

/// Seletor − / número / + com limites de 1 até [maximo] — mesmo padrão do
/// ProductView.
class _SeletorQuantidade extends StatelessWidget {
  final int valor;
  final int maximo;
  final ValueChanged<int> onChanged;

  const _SeletorQuantidade({required this.valor, required this.maximo, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            onPressed: valor > 1 ? () => onChanged(valor - 1) : null,
            icon: const Icon(Icons.remove, size: 16),
            visualDensity: VisualDensity.compact,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
            padding: EdgeInsets.zero,
          ),
          SizedBox(
            width: 28,
            child: Text('$valor', textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
          IconButton(
            onPressed: valor < maximo ? () => onChanged(valor + 1) : null,
            icon: const Icon(Icons.add, size: 16),
            visualDensity: VisualDensity.compact,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
            padding: EdgeInsets.zero,
          ),
        ],
      ),
    );
  }
}
