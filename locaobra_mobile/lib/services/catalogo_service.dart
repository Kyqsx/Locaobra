import 'dart:convert';
import 'package:locaobra_mobile/models/equipamento.dart';
import 'api_client.dart';

/// A API respondeu 404 para o equipamento pedido (removido ou inativo).
class EquipamentoNaoEncontradoException implements Exception {
  const EquipamentoNaoEncontradoException();

  @override
  String toString() => 'Equipamento não encontrado.';
}

/// Busca o catálogo de equipamentos. A API não tem um endpoint por slug de
/// categoria — ela devolve tudo em GET /api/equipamentos e o filtro por
/// categoria é feito no cliente, igual ao locaobra_web faz.
class CatalogoService {
  Future<List<Equipamento>> buscarTodos() async {
    final response = await ApiClient.get('/api/equipamentos');

    if (response.statusCode != 200) {
      throw Exception('Falha ao carregar os equipamentos (status ${response.statusCode}).');
    }

    // bodyBytes + utf8: o Spring manda "application/json" sem charset e o
    // response.body do pacote http assumiria latin1 (acentos quebrados).
    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) {
      throw Exception('Formato inesperado na resposta da API.');
    }

    return data
        .whereType<Map<String, dynamic>>()
        .map(Equipamento.fromJson)
        .toList();
  }

  /// Detalhe de um equipamento — GET /api/equipamentos/{id}, a mesma chamada
  /// que a página de produto do web faz.
  Future<Equipamento> buscarPorId(int id) async {
    final response = await ApiClient.get('/api/equipamentos/$id');

    if (response.statusCode == 404) {
      throw const EquipamentoNaoEncontradoException();
    }
    if (response.statusCode != 200) {
      throw Exception('Falha ao carregar o equipamento (status ${response.statusCode}).');
    }

    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! Map<String, dynamic>) {
      throw Exception('Formato inesperado na resposta da API.');
    }
    return Equipamento.fromJson(data);
  }

  /// [categoriaSlug] é comparado sem diferenciar maiúsculas/minúsculas,
  /// igual à filtragem feita em locaobra_web (Catalogo/catalogo.jsx).
  Future<List<Equipamento>> buscarPorCategoria(String categoriaSlug) async {
    final todos = await buscarTodos();
    return todos
        .where((eq) => eq.categoria.toLowerCase() == categoriaSlug.toLowerCase())
        .toList();
  }
}
