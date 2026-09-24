import 'dart:convert';
import 'package:locaobra_mobile/cart/cart_item.dart';
import 'package:locaobra_mobile/models/endereco.dart';
import 'package:locaobra_mobile/models/frete_estimativa.dart';
import 'package:locaobra_mobile/models/pedido.dart';
import 'package:locaobra_mobile/models/ponto_retirada.dart';
import 'api_client.dart';

/// Como o cliente quer receber o equipamento — mesmos valores do enum
/// TipoEntrega da API.
enum TipoEntrega { entrega, retirada }

extension TipoEntregaApi on TipoEntrega {
  String get valorApi => this == TipoEntrega.entrega ? 'ENTREGA' : 'RETIRADA';
}

/// Pedido recém-criado — só o que a tela de sucesso do checkout precisa.
class PedidoCriado {
  final int id;
  final String codigo;

  const PedidoCriado({required this.id, required this.codigo});

  factory PedidoCriado.fromJson(Map<String, dynamic> json) {
    return PedidoCriado(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}') ?? 0,
      codigo: json['codigo']?.toString() ?? '',
    );
  }
}

/// A API recusou o pedido (ex.: validação de datas/itens). Mensagem já vem
/// pronta pra mostrar na tela.
class PedidoInvalidoException implements Exception {
  final String mensagem;
  const PedidoInvalidoException(this.mensagem);

  @override
  String toString() => mensagem;
}

/// Carrinho + Checkout — equivalente ao que `Cart/carrinho.jsx` faz contra
/// POST /api/pedidos, POST /api/pedidos/estimar-frete e
/// GET /api/pedidos/pontos-retirada.
class PedidoService {
  String _dataIso(DateTime data) =>
      '${data.year.toString().padLeft(4, '0')}-${data.month.toString().padLeft(2, '0')}-${data.day.toString().padLeft(2, '0')}';

  List<Map<String, dynamic>> _itensJson(List<CartItem> itens) {
    return itens
        .map((i) => {
              'equipamentoId': i.equipamentoId,
              'quantidade': i.quantidade,
              if (i.observacaoItem.trim().isNotEmpty)
                'observacaoItem': i.observacaoItem.trim(),
            })
        .toList();
  }

  /// GET /api/pedidos/pontos-retirada — só carregado quando o cliente marca
  /// RETIRADA, igual ao web.
  Future<List<PontoRetirada>> listarPontosRetirada() async {
    final response = await ApiClient.get('/api/pedidos/pontos-retirada');
    if (response.statusCode != 200) return const [];

    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) return const [];
    return data.whereType<Map<String, dynamic>>().map(PontoRetirada.fromJson).toList();
  }

  /// POST /api/pedidos/estimar-frete. Devolve null em qualquer falha — quem
  /// chama mostra "a calcular" e deixa o cliente enviar o pedido do mesmo
  /// jeito (o valor final sai com o consultor).
  Future<FreteEstimativa?> estimarFrete({
    required DateTime dataInicio,
    required DateTime dataFim,
    int? enderecoId,
    Endereco? enderecoNovo,
    required List<CartItem> itens,
  }) async {
    try {
      final payload = {
        'dataInicio': _dataIso(dataInicio),
        'dataFim': _dataIso(dataFim),
        if (enderecoId != null) 'enderecoId': enderecoId,
        if (enderecoId == null && enderecoNovo != null)
          'enderecoEntrega': enderecoNovo.toRequestJson(),
        'itens': _itensJson(itens),
      };
      final response = await ApiClient.post('/api/pedidos/estimar-frete', payload);
      if (response.statusCode != 200) return null;

      final data = jsonDecode(utf8.decode(response.bodyBytes));
      if (data is! Map<String, dynamic>) return null;
      return FreteEstimativa.fromJson(data);
    } catch (_) {
      return null;
    }
  }

  /// POST /api/pedidos — envia o orçamento. Uma das três formas de destino
  /// deve ser preenchida: [enderecoId] (endereço salvo), [enderecoNovo]
  /// (digitado na hora) ou nenhum dos dois quando [tipoEntrega] é retirada.
  Future<PedidoCriado> criar({
    required DateTime dataInicio,
    required DateTime dataFim,
    required TipoEntrega tipoEntrega,
    int? enderecoId,
    Endereco? enderecoNovo,
    String? observacoesCliente,
    required List<CartItem> itens,
  }) async {
    final payload = <String, dynamic>{
      'dataInicio': _dataIso(dataInicio),
      'dataFim': _dataIso(dataFim),
      'tipoEntrega': tipoEntrega.valorApi,
      'itens': _itensJson(itens),
    };

    if (tipoEntrega == TipoEntrega.entrega) {
      if (enderecoId != null) {
        payload['enderecoId'] = enderecoId;
      } else if (enderecoNovo != null) {
        payload['enderecoEntrega'] = enderecoNovo.toRequestJson();
      }
    }
    if (observacoesCliente != null && observacoesCliente.trim().isNotEmpty) {
      payload['observacoesCliente'] = observacoesCliente.trim();
    }

    final response = await ApiClient.post('/api/pedidos', payload);

    if (response.statusCode == 201) {
      final data = jsonDecode(utf8.decode(response.bodyBytes));
      if (data is! Map<String, dynamic>) {
        throw Exception('Resposta inesperada da API ao criar o pedido.');
      }
      return PedidoCriado.fromJson(data);
    }

    if (response.statusCode == 400 || response.statusCode == 422) {
      throw PedidoInvalidoException(_extrairMensagem(response.body) ??
          'Não foi possível enviar o pedido. Confira os dados e tente de novo.');
    }

    throw Exception(_extrairMensagem(response.body) ??
        'Não foi possível enviar o pedido (status ${response.statusCode}).');
  }

  /// GET /api/pedidos/meus — pedidos do cliente logado, equivalente a
  /// `Pedidos/meusPedidos.jsx`.
  Future<List<Pedido>> listarMeus() async {
    final response = await ApiClient.get('/api/pedidos/meus');
    if (response.statusCode != 200) {
      throw Exception('Não foi possível carregar seus pedidos (status ${response.statusCode}).');
    }

    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) return const [];
    return data.whereType<Map<String, dynamic>>().map(Pedido.fromJson).toList();
  }

  /// POST /api/pedidos/{id}/cancelar — só permitido enquanto o pedido
  /// estiver SOLICITADO (mesma regra do botão "Cancelar pedido" no web).
  Future<void> cancelar(int id) async {
    final response = await ApiClient.post('/api/pedidos/$id/cancelar', const {});
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível cancelar o pedido.');
    }
  }

  String? _extrairMensagem(String body) {
    try {
      final data = jsonDecode(body);
      if (data is Map<String, dynamic> && data['message'] != null) {
        return data['message'].toString();
      }
    } catch (_) {
      // corpo não é JSON, ignora
    }
    return null;
  }
}
