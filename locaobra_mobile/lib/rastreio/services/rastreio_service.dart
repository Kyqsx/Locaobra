import 'dart:convert';
import 'package:locaobra_mobile/rastreio/models/rastreio_expedicao.dart';
import 'package:locaobra_mobile/services/api_client.dart';

/// Rastreio de entrega/coleta pro cliente logado — espelha a fatia de
/// /api/expedicoes consumida pelo app do cliente (só leitura; quem altera o
/// status é sempre o conferente/entregador).
class RastreioService {
  /// GET /api/expedicoes/minhas — todas as expedições (entrega e coleta) em
  /// que o cliente logado é o destinatário, mais recentes primeiro.
  Future<List<RastreioExpedicao>> listarMinhas() async {
    final response = await ApiClient.get('/api/expedicoes/minhas');
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível carregar suas entregas (status ${response.statusCode}).');
    }
    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) return const [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(RastreioExpedicao.fromJson)
        .toList();
  }

  /// GET /api/expedicoes/minhas/{id} — detalhe de uma expedição específica
  /// (a API garante que só devolve se for do cliente logado).
  Future<RastreioExpedicao> buscarPorId(int id) async {
    final response = await ApiClient.get('/api/expedicoes/minhas/$id');
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível carregar essa entrega (status ${response.statusCode}).');
    }
    return RastreioExpedicao.fromJson(
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>);
  }

  /// GET /api/expedicoes/pedido/{pedidoId} — expedição(ões) geradas por um
  /// pedido específico. Normalmente uma só, mas pode ser mais de uma quando
  /// os itens saíram de depósitos diferentes.
  Future<List<RastreioExpedicao>> listarPorPedido(int pedidoId) async {
    final response = await ApiClient.get('/api/expedicoes/pedido/$pedidoId');
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível carregar o rastreio desse pedido (status ${response.statusCode}).');
    }
    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) return const [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(RastreioExpedicao.fromJson)
        .toList();
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
