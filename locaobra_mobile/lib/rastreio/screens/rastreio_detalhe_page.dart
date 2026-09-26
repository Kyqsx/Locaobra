import 'package:flutter/material.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart'
    show StatusExpedicao, statusExpedicaoLabel, tipoExpedicaoLabel;
import 'package:locaobra_mobile/entregador/widgets/status_expedicao_badge.dart';
import 'package:locaobra_mobile/rastreio/models/rastreio_expedicao.dart';
import 'package:locaobra_mobile/rastreio/services/rastreio_service.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Rastreio de UMA expedição (entrega ou coleta) pro cliente: linha do
/// tempo com as etapas já percorridas, endereço, itens e — quando já
/// confirmada no local — quem recebeu e a foto tirada pelo entregador.
///
/// Recebe ou o [id] da expedição (rota "/minhas/{id}", usada quando se vem
/// da lista geral de entregas) ou uma [expedicaoInicial] já carregada
/// (usada quando se vem de "Meus Pedidos", que já buscou por pedidoId) —
/// nesse segundo caso ainda assim faz um refresh puxando pra baixo.
class RastreioDetalhePage extends StatefulWidget {
  final int? id;
  final RastreioExpedicao? expedicaoInicial;

  const RastreioDetalhePage({super.key, this.id, this.expedicaoInicial})
      : assert(id != null || expedicaoInicial != null,
            'Informe o id da expedição ou uma expedição já carregada.');

  @override
  State<RastreioDetalhePage> createState() => _RastreioDetalhePageState();
}

class _RastreioDetalhePageState extends State<RastreioDetalhePage> {
  final RastreioService _service = RastreioService();

  RastreioExpedicao? _expedicao;
  bool _carregando = true;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _expedicao = widget.expedicaoInicial;
    _carregar();
  }

  Future<void> _carregar() async {
    final id = widget.id ?? widget.expedicaoInicial!.id;
    setState(() {
      _carregando = _expedicao == null;
      _erro = null;
    });
    try {
      final expedicao = await _service.buscarPorId(id);
      if (!mounted) return;
      setState(() {
        _expedicao = expedicao;
        _carregando = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _erro = _expedicao == null
            ? e.toString().replaceFirst('Exception: ', '')
            : null; // já tem algo na tela (o inicial); só ignora o refresh
        _carregando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(
        title: Text(
          _expedicao != null ? 'Rastreio — ${tipoExpedicaoLabel(_expedicao!.tipo)}' : 'Rastreio da entrega',
        ),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_carregando && _expedicao == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_erro != null && _expedicao == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_erro!, textAlign: TextAlign.center, style: TextStyle(color: AppColors.error)),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: _carregar, child: const Text('Tentar de novo')),
            ],
          ),
        ),
      );
    }

    final expedicao = _expedicao!;
    return RefreshIndicator(
      onRefresh: _carregar,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildCabecalho(expedicao),
          const SizedBox(height: 16),
          if (expedicao.cancelada)
            _buildCanceladaBanner(expedicao)
          else
            _buildLinhaDoTempo(expedicao),
          const SizedBox(height: 16),
          _buildCard(
            titulo: 'Endereço',
            child: Text(
              expedicao.enderecoEntrega.formatado?.isNotEmpty == true
                  ? expedicao.enderecoEntrega.formatado!
                  : 'Não informado',
              style: const TextStyle(fontSize: 14),
            ),
          ),
          const SizedBox(height: 12),
          _buildCard(
            titulo: 'Itens',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: expedicao.itens.isEmpty
                  ? const [Text('Nenhum item informado.')]
                  : expedicao.itens
                      .map((i) => Padding(
                            padding: const EdgeInsets.only(bottom: 4),
                            child: Text('• ${i.equipamentoNome ?? 'Equipamento'} × ${i.quantidade}'),
                          ))
                      .toList(),
            ),
          ),
          if (expedicao.observacoes != null && expedicao.observacoes!.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            _buildCard(titulo: 'Observações', child: Text(expedicao.observacoes!)),
          ],
          if (expedicao.entregaConfirmadaEm != null) ...[
            const SizedBox(height: 12),
            _buildConfirmacaoCard(expedicao),
          ],
        ],
      ),
    );
  }

  Widget _buildCabecalho(RastreioExpedicao expedicao) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(expedicao.codigo,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontFamily: 'monospace', fontSize: 15)),
              StatusExpedicaoBadge(status: expedicao.status),
            ],
          ),
          const SizedBox(height: 8),
          if (expedicao.pedidoCodigo != null) _linha('Pedido', expedicao.pedidoCodigo!),
          _linha(
            'Data prevista',
            '${_formatarDataSimples(expedicao.dataProgramada)}'
                '${expedicao.horarioProgramado != null && expedicao.horarioProgramado!.isNotEmpty ? ' às ${expedicao.horarioProgramado}' : ''}',
          ),
          if (expedicao.motoristaNome != null) _linha('Entregador', expedicao.motoristaNome!),
        ],
      ),
    );
  }

  Widget _linha(String rotulo, String valor) {
    return Padding(
      padding: const EdgeInsets.only(top: 4),
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

  Widget _buildCanceladaBanner(RastreioExpedicao expedicao) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.errorBg,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.errorLight),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.cancel_outlined, color: AppColors.error),
          const SizedBox(width: 10),
          Expanded(
            child: Text.rich(
              TextSpan(
                children: [
                  const TextSpan(text: 'Cancelada. ', style: TextStyle(fontWeight: FontWeight.bold)),
                  TextSpan(text: expedicao.motivoCancelamento ?? 'Nenhum motivo informado.'),
                ],
              ),
              style: TextStyle(color: AppColors.error, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  // Etapas da entrega: Agendado -> Em trânsito -> Entregue.
  // Etapas da coleta: as mesmas, mais Concluído (equipamento de volta ao depósito).
  Widget _buildLinhaDoTempo(RastreioExpedicao expedicao) {
    final etapas = expedicao.ehColeta
        ? const [
            StatusExpedicao.agendado,
            StatusExpedicao.emTransito,
            StatusExpedicao.entregue,
            StatusExpedicao.concluido,
          ]
        : const [
            StatusExpedicao.agendado,
            StatusExpedicao.emTransito,
            StatusExpedicao.entregue,
          ];
    final indiceAtual = etapas.indexOf(expedicao.status);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var i = 0; i < etapas.length; i++)
            _buildEtapa(
              rotulo: statusExpedicaoLabel(etapas[i]),
              concluida: indiceAtual >= 0 && i <= indiceAtual,
              atual: i == indiceAtual,
              ultima: i == etapas.length - 1,
              dataHora: _dataHoraDaEtapa(expedicao, etapas[i]),
            ),
        ],
      ),
    );
  }

  String? _dataHoraDaEtapa(RastreioExpedicao expedicao, StatusExpedicao etapa) {
    switch (etapa) {
      case StatusExpedicao.agendado:
        return _formatarDataHora(expedicao.criadoEm);
      case StatusExpedicao.emTransito:
        return _formatarDataHora(expedicao.checkoutEm);
      case StatusExpedicao.entregue:
        return _formatarDataHora(expedicao.entregaConfirmadaEm);
      default:
        return null; // CONCLUIDO: a API de rastreio não expõe checkinEm (não é relevante pro cliente)
    }
  }

  Widget _buildEtapa({
    required String rotulo,
    required bool concluida,
    required bool atual,
    required bool ultima,
    String? dataHora,
  }) {
    final cor = concluida ? AppColors.success : AppColors.gray400;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Column(
              children: [
                Icon(
                  concluida ? Icons.check_circle : Icons.radio_button_unchecked,
                  color: cor,
                  size: 20,
                ),
                if (!ultima)
                  Expanded(
                    child: Container(
                      width: 2,
                      margin: const EdgeInsets.symmetric(vertical: 2),
                      color: concluida ? AppColors.successBg : AppColors.gray300,
                    ),
                  ),
              ],
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 1),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      rotulo,
                      style: TextStyle(
                        fontWeight: atual || concluida ? FontWeight.bold : FontWeight.normal,
                        color: concluida ? AppColors.textPrimary : AppColors.gray600,
                        fontSize: 14,
                      ),
                    ),
                    if (dataHora != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(dataHora, style: TextStyle(color: AppColors.gray600, fontSize: 12)),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConfirmacaoCard(RastreioExpedicao expedicao) {
    return _buildCard(
      titulo: expedicao.ehColeta ? 'Confirmação da coleta' : 'Confirmação da entrega',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (expedicao.assinaturaEntrega != null) _linha('Recebido por', expedicao.assinaturaEntrega!),
          _linha('Confirmado em', _formatarDataHora(expedicao.entregaConfirmadaEm) ?? '—'),
          if (expedicao.fotoEntrega != null && expedicao.fotoEntrega!.isNotEmpty) ...[
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(AppRadius.lg),
              child: Image.network(
                expedicao.fotoEntregaUrl,
                height: 180,
                width: double.infinity,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) => const SizedBox.shrink(),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildCard({required String titulo, required Widget child}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          child,
        ],
      ),
    );
  }

  // 'yyyy-MM-dd' -> 'dd/MM/yyyy' (mesmo formato de formatarData, mas a
  // partir da string ISO que vem pronta da API, sem precisar de DateTime).
  String _formatarDataSimples(String? iso) {
    if (iso == null || iso.length < 10) return '—';
    final partes = iso.substring(0, 10).split('-');
    if (partes.length != 3) return iso;
    return '${partes[2]}/${partes[1]}/${partes[0]}';
  }

  // 'yyyy-MM-ddTHH:mm:ss...' -> 'dd/MM/yyyy às HH:mm'.
  String? _formatarDataHora(String? iso) {
    if (iso == null || iso.length < 16) return null;
    final data = _formatarDataSimples(iso);
    final hora = iso.substring(11, 16);
    return '$data às $hora';
  }
}
