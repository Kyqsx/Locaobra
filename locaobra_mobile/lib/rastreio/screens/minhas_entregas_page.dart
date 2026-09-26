import 'package:flutter/material.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart' show tipoExpedicaoLabel;
import 'package:locaobra_mobile/entregador/widgets/status_expedicao_badge.dart';
import 'package:locaobra_mobile/rastreio/models/rastreio_expedicao.dart';
import 'package:locaobra_mobile/rastreio/screens/rastreio_detalhe_page.dart';
import 'package:locaobra_mobile/rastreio/services/rastreio_service.dart';

/// "Minhas Entregas" — lista todas as expedições (entregas e coletas) do
/// cliente logado, mais recentes primeiro. Ponto de entrada geral do
/// rastreio (menu do usuário); o rastreio de um pedido específico também
/// pode ser aberto direto a partir de "Meus Pedidos".
class MinhasEntregasPage extends StatefulWidget {
  const MinhasEntregasPage({super.key});

  @override
  State<MinhasEntregasPage> createState() => _MinhasEntregasPageState();
}

class _MinhasEntregasPageState extends State<MinhasEntregasPage> {
  final RastreioService _service = RastreioService();

  bool _carregando = true;
  String? _erro;
  List<RastreioExpedicao> _entregas = const [];

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
      final entregas = await _service.listarMinhas();
      if (!mounted) return;
      setState(() {
        _entregas = entregas;
        _carregando = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _erro = e.toString().replaceFirst('Exception: ', '');
        _carregando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('Minhas Entregas'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SafeArea(child: _buildBody()),
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
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_erro!, textAlign: TextAlign.center, style: TextStyle(color: Colors.red.shade700)),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: _carregar, child: const Text('Tentar de novo')),
            ],
          ),
        ),
      );
    }
    if (_entregas.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: const [
              Icon(Icons.local_shipping_outlined, size: 56, color: Colors.grey),
              SizedBox(height: 12),
              Text('Você ainda não tem entregas ou coletas agendadas.', textAlign: TextAlign.center),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _carregar,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _entregas.length,
        itemBuilder: (context, i) => Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _buildCard(_entregas[i]),
        ),
      ),
    );
  }

  Widget _buildCard(RastreioExpedicao expedicao) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => RastreioDetalhePage(expedicaoInicial: expedicao)),
        ),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.grey.shade300),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      '${tipoExpedicaoLabel(expedicao.tipo)} • ${expedicao.codigo}',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  StatusExpedicaoBadge(status: expedicao.status),
                ],
              ),
              const SizedBox(height: 6),
              if (expedicao.pedidoCodigo != null)
                Text('Pedido ${expedicao.pedidoCodigo}',
                    style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
              const SizedBox(height: 4),
              Text(
                expedicao.enderecoEntrega.formatado?.isNotEmpty == true
                    ? expedicao.enderecoEntrega.formatado!
                    : 'Endereço não informado',
                style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
