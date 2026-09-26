import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/models/pedido.dart';
import 'package:locaobra_mobile/rastreio/models/rastreio_expedicao.dart';
import 'package:locaobra_mobile/rastreio/screens/rastreio_detalhe_page.dart';
import 'package:locaobra_mobile/rastreio/services/rastreio_service.dart';
import 'package:locaobra_mobile/services/pedido_service.dart';
import 'package:locaobra_mobile/utils/formatters.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// "Meus Pedidos" — equivalente a `Pedidos/meusPedidos.jsx`: lista os
/// orçamentos do cliente logado, com status e opção de cancelar enquanto
/// ainda estiver SOLICITADO.
class MeusPedidosPage extends StatefulWidget {
  const MeusPedidosPage({super.key});

  @override
  State<MeusPedidosPage> createState() => _MeusPedidosPageState();
}

class _MeusPedidosPageState extends State<MeusPedidosPage> {
  final PedidoService _service = PedidoService();
  final RastreioService _rastreioService = RastreioService();

  bool _carregando = true;
  String? _erro;
  List<Pedido> _pedidos = const [];
  int? _cancelandoId;
  int? _rastreandoId;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final pedidos = await _service.listarMeus();
      if (!mounted) return;
      setState(() {
        _pedidos = pedidos;
        _carregando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _erro = 'Não foi possível carregar seus pedidos.';
        _carregando = false;
      });
    }
  }

  Future<void> _cancelar(Pedido pedido) async {
    final confirmou = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancelar pedido'),
        content: const Text('Tem certeza que deseja cancelar esse pedido?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Voltar')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Cancelar pedido'),
          ),
        ],
      ),
    );
    if (confirmou != true) return;

    setState(() => _cancelandoId = pedido.id);
    try {
      await _service.cancelar(pedido.id);
      if (!mounted) return;
      await _carregar();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
      setState(() => _cancelandoId = null);
    }
  }

  // Busca a(s) expedição(ões) geradas por esse pedido e abre o rastreio.
  // Normalmente é uma só; quando os itens saíram de depósitos diferentes,
  // o pedido pode ter mais de uma — nesse caso deixa o cliente escolher.
  Future<void> _rastrear(Pedido pedido) async {
    setState(() => _rastreandoId = pedido.id);
    try {
      final expedicoes = await _rastreioService.listarPorPedido(pedido.id);
      if (!mounted) return;
      setState(() => _rastreandoId = null);

      if (expedicoes.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Esse pedido ainda não tem uma entrega agendada.')),
        );
        return;
      }
      if (expedicoes.length == 1) {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => RastreioDetalhePage(expedicaoInicial: expedicoes.first)),
        );
        return;
      }
      _escolherExpedicao(expedicoes);
    } catch (e) {
      if (!mounted) return;
      setState(() => _rastreandoId = null);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  // Pedido desmembrado em mais de uma expedição (um depósito não tinha tudo):
  // deixa o cliente escolher qual acompanhar.
  Future<void> _escolherExpedicao(List<RastreioExpedicao> expedicoes) async {
    final escolhida = await showModalBottomSheet<RastreioExpedicao>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Esse pedido tem mais de uma entrega. Qual você quer acompanhar?',
                  style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            for (final e in expedicoes)
              ListTile(
                title: Text(e.codigo),
                subtitle: Text(e.enderecoEntrega.formatado ?? ''),
                onTap: () => Navigator.pop(ctx, e),
              ),
          ],
        ),
      ),
    );
    if (escolhida != null && mounted) {
      Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => RastreioDetalhePage(expedicaoInicial: escolhida)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_carregando) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_erro != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(_erro!, textAlign: TextAlign.center, style: TextStyle(color: AppColors.error)),
        ),
      );
    }
    if (_pedidos.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.receipt_long_outlined, size: 56, color: AppColors.gray500),
              const SizedBox(height: 12),
              const Text('Você ainda não fez nenhum pedido.', textAlign: TextAlign.center),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                  foregroundColor: AppColors.white,
                ),
                child: const Text('Explorar catálogo'),
              ),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _carregar,
      child: ListView.builder(
        // Status bar + toolbar no padding do scroll (sem SafeArea), pra
        // conteúdo rolar por baixo do header transparente. Lateral 6px
        // igual ao catálogo/product view/carrinho.
        padding: EdgeInsets.fromLTRB(
          6,
          MediaQuery.of(context).padding.top + kToolbarHeight + 8,
          6,
          8,
        ),
        itemCount: _pedidos.length,
        itemBuilder: (context, i) => Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _buildPedidoCard(_pedidos[i]),
        ),
      ),
    );
  }

  Widget _buildPedidoCard(Pedido pedido) {
    final corStatus = _corStatus(pedido.status);
    final valorTotal = pedido.valorTotalEstimado + (pedido.valorFrete ?? 0);

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.gray50,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(AppRadius.xl),
                topRight: Radius.circular(AppRadius.xl),
              ),
              border: Border(bottom: BorderSide(color: AppColors.gray200)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Text(pedido.codigo, style: const TextStyle(fontWeight: FontWeight.bold, fontFamily: 'monospace')),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: corStatus.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(AppRadius.full),
                        ),
                        child: Text(
                          pedido.status.label,
                          style: TextStyle(color: corStatus, fontSize: 11, fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  formatarMoeda(valorTotal),
                  style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primary, fontSize: 15),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _infoRow(
                  'Período',
                  '${pedido.dataInicio != null ? formatarData(pedido.dataInicio!) : '—'} a '
                      '${pedido.dataFim != null ? formatarData(pedido.dataFim!) : '—'}'
                      ' (${pedido.diasLocacao} dia${pedido.diasLocacao > 1 ? 's' : ''})',
                ),
                _infoRow(
                  'Entrega',
                  pedido.ehRetirada
                      ? '🏬 Retirada no depósito (sem frete)'
                      : '🚚 Entrega'
                          '${pedido.enderecoEntrega?.formatado != null ? ' — ${pedido.enderecoEntrega!.formatado}' : ''}',
                ),
                if (!pedido.ehRetirada && pedido.valorFrete != null)
                  _infoRow(
                    'Frete',
                    '${formatarMoeda(pedido.valorFrete!)}'
                  ),
                _infoRow('Total', formatarMoeda(valorTotal)),
                const Padding(
                  padding: EdgeInsets.only(top: 8, bottom: 4),
                  child: Divider(height: 1),
                ),
                ...pedido.itens.map(
                  (item) => Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            '${item.equipamentoNome} × ${item.quantidade}',
                            style: TextStyle(color: AppColors.gray700, fontSize: 13),
                          ),
                        ),
                        Text(
                          '${formatarMoeda(item.valorDiariaSnapshot)}/dia',
                          style: TextStyle(color: AppColors.gray600, fontSize: 13),
                        ),
                      ],
                    ),
                  ),
                ),
                if (pedido.observacoesCliente != null && pedido.observacoesCliente!.trim().isNotEmpty) ...[
                  const SizedBox(height: 8),
                  _infoRow('Suas observações', pedido.observacoesCliente!),
                ],
                if (pedido.motivoRecusa != null && pedido.motivoRecusa!.trim().isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.errorBg,
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                    ),
                    child: Text.rich(
                      TextSpan(
                        children: [
                          const TextSpan(text: 'Motivo: ', style: TextStyle(fontWeight: FontWeight.bold)),
                          TextSpan(text: pedido.motivoRecusa!),
                        ],
                      ),
                      style: TextStyle(color: AppColors.error, fontSize: 13),
                    ),
                  ),
                ],
              ],
            ),
          ),
          if (pedido.status == StatusPedido.aprovado)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Align(
                alignment: Alignment.centerRight,
                child: OutlinedButton.icon(
                  onPressed: _rastreandoId == pedido.id ? null : () => _rastrear(pedido),
                  icon: const Icon(Icons.local_shipping_outlined, size: 18),
                  label: Text(_rastreandoId == pedido.id ? 'Buscando...' : 'Rastrear entrega'),
                ),
              ),
            ),
          if (pedido.podeCancelar)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Align(
                alignment: Alignment.centerRight,
                child: OutlinedButton(
                  onPressed: _cancelandoId == pedido.id ? null : () => _cancelar(pedido),
                  style: OutlinedButton.styleFrom(foregroundColor: AppColors.error),
                  child: Text(_cancelandoId == pedido.id ? 'Cancelando...' : 'Cancelar pedido'),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _infoRow(String rotulo, String valor) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: RichText(
        text: TextSpan(
          style: TextStyle(color: AppColors.gray800, fontSize: 13),
          children: [
            TextSpan(text: '$rotulo: ', style: const TextStyle(fontWeight: FontWeight.bold)),
            TextSpan(text: valor),
          ],
        ),
      ),
    );
  }

  Color _corStatus(StatusPedido status) {
    switch (status) {
      case StatusPedido.aprovado:
        return AppColors.success;
      case StatusPedido.recusado:
        return AppColors.error;
      case StatusPedido.cancelado:
        return AppColors.gray600;
      case StatusPedido.solicitado:
        return AppColors.info;
    }
  }
}
