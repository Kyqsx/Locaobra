import 'package:locaobra_mobile/models/endereco.dart';

/// Espelha o enum StatusPedido da API — mesmos rótulos usados em
/// `Pedidos/meusPedidos.jsx` (STATUS_INFO).
enum StatusPedido { solicitado, aprovado, recusado, cancelado }

extension StatusPedidoParser on StatusPedido {
  static StatusPedido fromApi(String? valor) {
    switch (valor) {
      case 'APROVADO':
        return StatusPedido.aprovado;
      case 'RECUSADO':
        return StatusPedido.recusado;
      case 'CANCELADO':
        return StatusPedido.cancelado;
      case 'SOLICITADO':
      default:
        return StatusPedido.solicitado;
    }
  }

  String get label {
    switch (this) {
      case StatusPedido.aprovado:
        return 'Aprovado';
      case StatusPedido.recusado:
        return 'Recusado';
      case StatusPedido.cancelado:
        return 'Cancelado';
      case StatusPedido.solicitado:
        return 'Aguardando revisão';
    }
  }
}

/// Um item do pedido — equivalente a ItemPedidoResponse.
class ItemPedido {
  final int id;
  final String equipamentoNome;
  final int quantidade;
  final double valorDiariaSnapshot;

  const ItemPedido({
    required this.id,
    required this.equipamentoNome,
    required this.quantidade,
    required this.valorDiariaSnapshot,
  });

  factory ItemPedido.fromJson(Map<String, dynamic> json) {
    return ItemPedido(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}') ?? 0,
      equipamentoNome: json['equipamentoNome']?.toString() ?? '',
      quantidade: json['quantidade'] is int
          ? json['quantidade'] as int
          : int.tryParse('${json['quantidade']}') ?? 0,
      valorDiariaSnapshot: double.tryParse('${json['valorDiariaSnapshot']}') ?? 0,
    );
  }
}

/// Um pedido/orçamento do cliente — equivalente a PedidoResponse, só com o
/// que a tela "Meus Pedidos" precisa (mesmos campos que
/// `Pedidos/meusPedidos.jsx` usa).
class Pedido {
  final int id;
  final String codigo;
  final StatusPedido status;
  final DateTime? dataInicio;
  final DateTime? dataFim;
  final int diasLocacao;
  final String tipoEntrega; // 'ENTREGA' ou 'RETIRADA'
  final Endereco? enderecoEntrega;
  final double valorTotalEstimado;
  final double? valorFrete;
  final String? observacoesCliente;
  final String? motivoRecusa;
  final List<ItemPedido> itens;

  const Pedido({
    required this.id,
    required this.codigo,
    required this.status,
    this.dataInicio,
    this.dataFim,
    required this.diasLocacao,
    required this.tipoEntrega,
    this.enderecoEntrega,
    required this.valorTotalEstimado,
    this.valorFrete,
    this.observacoesCliente,
    this.motivoRecusa,
    this.itens = const [],
  });

  bool get podeCancelar => status == StatusPedido.solicitado;
  bool get ehRetirada => tipoEntrega == 'RETIRADA';

  static DateTime? _dataIso(dynamic valor) {
    if (valor == null) return null;
    try {
      return DateTime.parse(valor.toString());
    } catch (_) {
      return null;
    }
  }

  factory Pedido.fromJson(Map<String, dynamic> json) {
    return Pedido(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}') ?? 0,
      codigo: json['codigo']?.toString() ?? '',
      status: StatusPedidoParser.fromApi(json['status']?.toString()),
      dataInicio: _dataIso(json['dataInicio']),
      dataFim: _dataIso(json['dataFim']),
      diasLocacao: json['diasLocacao'] is int
          ? json['diasLocacao'] as int
          : int.tryParse('${json['diasLocacao']}') ?? 1,
      tipoEntrega: json['tipoEntrega']?.toString() ?? 'ENTREGA',
      enderecoEntrega: json['enderecoEntrega'] is Map<String, dynamic>
          ? Endereco.fromJson(json['enderecoEntrega'] as Map<String, dynamic>)
          : null,
      valorTotalEstimado: double.tryParse('${json['valorTotalEstimado']}') ?? 0,
      valorFrete: json['valorFrete'] == null ? null : double.tryParse('${json['valorFrete']}'),
      observacoesCliente: json['observacoesCliente']?.toString(),
      motivoRecusa: json['motivoRecusa']?.toString(),
      itens: (json['itens'] as List?)
              ?.whereType<Map<String, dynamic>>()
              .map(ItemPedido.fromJson)
              .toList() ??
          const [],
    );
  }
}
