import 'dart:convert';
import 'package:locaobra_mobile/models/avaliacao.dart';
import 'api_client.dart';

/// Leitura das avaliações de um equipamento (endpoint público da API).
class AvaliacaoService {
  Future<ResumoAvaliacoes> listarPorEquipamento(int equipamentoId) async {
    final response = await ApiClient.get(
      '/api/avaliacoes/equipamento/$equipamentoId',
    );

    if (response.statusCode != 200) {
      throw Exception(
        'Falha ao carregar as avaliações (status ${response.statusCode}).',
      );
    }

    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! Map<String, dynamic>) {
      throw Exception('Formato inesperado na resposta da API.');
    }
    return ResumoAvaliacoes.fromJson(data);
  }
}
