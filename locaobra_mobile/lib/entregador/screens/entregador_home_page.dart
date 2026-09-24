import 'package:flutter/material.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/auth/login_page.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart';
import 'package:locaobra_mobile/entregador/screens/entregador_detalhe_page.dart';
import 'package:locaobra_mobile/entregador/services/expedicao_service.dart';
import 'package:locaobra_mobile/entregador/widgets/status_expedicao_badge.dart';
import 'package:locaobra_mobile/services/auth_service.dart';

const _corPrimaria = Color.fromARGB(255, 255, 128, 0);

/// Home do entregador — equivalente, no mobile, à tela de Expedição do
/// painel web, mas só com o que o ENTREGADOR usa: a lista das expedições em
/// que ele é o motorista designado (a API já filtra isso pelo token) e o
/// atalho pra abrir cada uma e registrar a entrega/coleta.
class EntregadorHomePage extends StatefulWidget {
  const EntregadorHomePage({super.key});

  @override
  State<EntregadorHomePage> createState() => _EntregadorHomePageState();
}

class _EntregadorHomePageState extends State<EntregadorHomePage> {
  final _service = ExpedicaoApiService();
  late Future<List<Expedicao>> _futureExpedicoes;

  // AGENDADO fica fora por padrão: enquanto o conferente não faz o
  // check-out (EM_TRANSITO), não há nada pro entregador fazer ainda.
  StatusExpedicao? _filtro = StatusExpedicao.emTransito;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  void _carregar() {
    setState(() {
      _futureExpedicoes = _service.listar();
    });
  }

  Future<void> _sair() async {
    await AuthService().logout();
    AuthState.logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginPage()),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        foregroundColor: Colors.black87,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Minhas entregas',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
            ValueListenableBuilder<String?>(
              valueListenable: AuthState.usuarioLogado,
              builder: (context, nome, _) => Text(
                nome ?? '',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Sair',
            onPressed: _sair,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: Column(
        children: [
          _buildFiltros(),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () async => _carregar(),
              child: FutureBuilder<List<Expedicao>>(
                future: _futureExpedicoes,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErro(snapshot.error.toString());
                  }

                  final todas = snapshot.data ?? [];
                  final filtradas = _filtro == null
                      ? todas
                      : todas.where((e) => e.status == _filtro).toList();

                  if (filtradas.isEmpty) {
                    return _buildVazio();
                  }

                  return ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(16),
                    itemCount: filtradas.length,
                    separatorBuilder: (context, index) => const SizedBox(height: 10),
                    itemBuilder: (context, index) =>
                        _ExpedicaoCard(expedicao: filtradas[index], onTap: () => _abrirDetalhe(filtradas[index])),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _abrirDetalhe(Expedicao expedicao) async {
    final houveMudanca = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => EntregadorDetalhePage(expedicaoId: expedicao.id),
      ),
    );
    if (houveMudanca == true) _carregar();
  }

  Widget _buildFiltros() {
    final opcoes = <MapEntry<String, StatusExpedicao?>>[
      const MapEntry('Em trânsito', StatusExpedicao.emTransito),
      const MapEntry('Agendadas', StatusExpedicao.agendado),
      const MapEntry('Entregues', StatusExpedicao.entregue),
      const MapEntry('Concluídas', StatusExpedicao.concluido),
      const MapEntry('Todas', null),
    ];
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        child: Row(
          children: opcoes.map((opcao) {
            final selecionado = _filtro == opcao.value;
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: ChoiceChip(
                label: Text(opcao.key),
                selected: selecionado,
                onSelected: (_) => setState(() => _filtro = opcao.value),
                selectedColor: _corPrimaria.withValues(alpha: 0.18),
                labelStyle: TextStyle(
                  color: selecionado ? _corPrimaria : Colors.black87,
                  fontWeight: selecionado ? FontWeight.w600 : FontWeight.normal,
                ),
                side: BorderSide(color: selecionado ? _corPrimaria : Colors.grey.shade300),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget _buildVazio() {
    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight),
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.local_shipping_outlined, size: 56, color: Colors.grey.shade400),
                  const SizedBox(height: 12),
                  Text(
                    'Nenhuma expedição por aqui.',
                    style: TextStyle(color: Colors.grey.shade600, fontSize: 15),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErro(String mensagem) {
    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight),
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.error_outline, size: 48, color: Colors.red.shade300),
                  const SizedBox(height: 12),
                  Text(mensagem, textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: _carregar, child: const Text('Tentar de novo')),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ExpedicaoCard extends StatelessWidget {
  final Expedicao expedicao;
  final VoidCallback onTap;

  const _ExpedicaoCard({required this.expedicao, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final endereco = expedicao.enderecoEntrega.formatado;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: Colors.grey.shade200),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Row(
                      children: [
                        Icon(
                          expedicao.ehColeta ? Icons.inventory_2_outlined : Icons.local_shipping_outlined,
                          size: 18,
                          color: _corPrimaria,
                        ),
                        const SizedBox(width: 6),
                        Flexible(
                          child: Text(
                            '${tipoExpedicaoLabel(expedicao.tipo)} · ${expedicao.codigo}',
                            style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  StatusExpedicaoBadge(status: expedicao.status),
                ],
              ),
              const SizedBox(height: 8),
              if (expedicao.clienteNome != null && expedicao.clienteNome!.isNotEmpty)
                Text(
                  expedicao.clienteNome!,
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                ),
              if (endereco != null && endereco.isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.place_outlined, size: 16, color: Colors.grey.shade500),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        endereco,
                        style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],
              if (expedicao.dataProgramada != null) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.event_outlined, size: 16, color: Colors.grey.shade500),
                    const SizedBox(width: 4),
                    Text(
                      _formatarData(expedicao.dataProgramada!) +
                          (expedicao.horarioProgramado != null && expedicao.horarioProgramado!.isNotEmpty
                              ? ' · ${expedicao.horarioProgramado}'
                              : ''),
                      style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  /// yyyy-MM-dd (como a API manda) → dd/MM/yyyy, igual o web (formatDateOnly).
  String _formatarData(String data) {
    final partes = data.split('-');
    if (partes.length != 3) return data;
    return '${partes[2]}/${partes[1]}/${partes[0]}';
  }
}
