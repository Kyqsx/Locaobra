import 'package:locaobra_mobile/entregador/models/expedicao.dart'
    show StatusExpedicao, TipoExpedicao, EnderecoExpedicao, statusExpedicaoFromJson, tipoExpedicaoFromJson;
import 'package:locaobra_mobile/services/api_client.dart';

/// Item de uma expedição, na visão enxuta do rastreio do cliente — só o
/// nome do equipamento e a quantidade (sem patrimônio/observação interna).
class ItemRastreio {
  final String? equipamentoNome;
  final int quantidade;

  const ItemRastreio({this.equipamentoNome, required this.quantidade});

  factory ItemRastreio.fromJson(Map<String, dynamic> json) {
    return ItemRastreio(
      equipamentoNome: json['equipamentoNome']?.toString(),
      quantidade: (json['quantidade'] as num?)?.toInt() ?? 1,
    );
  }
}

/// Espelha com.locaobra.dto.response.ExpedicaoRastreioResponse — a versão
/// enxuta da expedição usada pelo cliente pra acompanhar a própria
/// entrega/coleta (reaproveita os enums/rótulos já usados na tela do
/// entregador, pra manter os mesmos textos e cores em toda a app).
class RastreioExpedicao {
  final int id;
  final String codigo;
  final TipoExpedicao tipo;
  final StatusExpedicao status;
  final String? pedidoCodigo;
  final String? dataProgramada; // yyyy-MM-dd
  final String? horarioProgramado;
  final EnderecoExpedicao enderecoEntrega;
  final String? motoristaNome;
  final String? observacoes;
  final String? criadoEm;
  final String? checkoutEm;
  final String? entregaConfirmadaEm;
  final String? assinaturaEntrega;
  final String? fotoEntrega;
  final String? motivoCancelamento;
  final List<ItemRastreio> itens;

  const RastreioExpedicao({
    required this.id,
    required this.codigo,
    required this.tipo,
    required this.status,
    this.pedidoCodigo,
    this.dataProgramada,
    this.horarioProgramado,
    this.enderecoEntrega = const EnderecoExpedicao(),
    this.motoristaNome,
    this.observacoes,
    this.criadoEm,
    this.checkoutEm,
    this.entregaConfirmadaEm,
    this.assinaturaEntrega,
    this.fotoEntrega,
    this.motivoCancelamento,
    this.itens = const [],
  });

  factory RastreioExpedicao.fromJson(Map<String, dynamic> json) {
    final itensJson = json['itens'] as List<dynamic>? ?? [];
    return RastreioExpedicao(
      id: (json['id'] as num?)?.toInt() ?? 0,
      codigo: json['codigo']?.toString() ?? '',
      tipo: tipoExpedicaoFromJson(json['tipo']?.toString()),
      status: statusExpedicaoFromJson(json['status']?.toString()),
      pedidoCodigo: json['pedidoCodigo']?.toString(),
      dataProgramada: json['dataProgramada']?.toString(),
      horarioProgramado: json['horarioProgramado']?.toString(),
      enderecoEntrega:
          EnderecoExpedicao.fromJson(json['enderecoEntrega'] as Map<String, dynamic>?),
      motoristaNome: json['motoristaNome']?.toString(),
      observacoes: json['observacoes']?.toString(),
      criadoEm: json['criadoEm']?.toString(),
      checkoutEm: json['checkoutEm']?.toString(),
      entregaConfirmadaEm: json['entregaConfirmadaEm']?.toString(),
      assinaturaEntrega: json['assinaturaEntrega']?.toString(),
      fotoEntrega: json['fotoEntrega']?.toString(),
      motivoCancelamento: json['motivoCancelamento']?.toString(),
      itens: itensJson
          .map((i) => ItemRastreio.fromJson(i as Map<String, dynamic>))
          .toList(),
    );
  }

  bool get ehColeta => tipo == TipoExpedicao.coleta;
  bool get cancelada => status == StatusExpedicao.cancelado;

  String get fotoEntregaUrl => ApiClient.resolveUrl(fotoEntrega);
}
