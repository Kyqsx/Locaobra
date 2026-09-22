import 'dart:convert';
import 'package:locaobra_mobile/models/equipamento.dart';
import 'api_client.dart';

/// Busca o catálogo de equipamentos. A API não tem um endpoint por slug de
/// categoria — ela devolve tudo em GET /api/equipamentos e o filtro por
/// categoria é feito no cliente, igual ao locaobra_web faz.
class CatalogoService {
  Future<List<Equipamento>> buscarTodos() async {
    final response = await ApiClient.get('/api/equipamentos');

    if (response.statusCode != 200) {
      throw Exception('Falha ao carregar os equipamentos (status ${response.statusCode}).');
    }

    final data = jsonDecode(response.body);
    if (data is! List) {
      throw Exception('Formato inesperado na resposta da API.');
    }

    return data
        .whereType<Map<String, dynamic>>()
        .map(Equipamento.fromJson)
        .toList();
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
