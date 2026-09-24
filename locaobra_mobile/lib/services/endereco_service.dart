import 'dart:convert';
import 'package:locaobra_mobile/models/endereco.dart';
import 'api_client.dart';

/// CRUD de endereços do cliente logado — equivalente ao que
/// `Perfil/meusEnderecos.jsx` consome no web.
class EnderecoService {
  /// GET /api/clientes/meus-enderecos.
  Future<List<Endereco>> listarMeus() async {
    final response = await ApiClient.get('/api/clientes/meus-enderecos');

    if (response.statusCode == 401 || response.statusCode == 403) {
      // Sessão expirada/carrinho aberto sem login — quem chama decide o que
      // fazer (ex.: tratar como "sem endereços salvos" e deixar digitar).
      return const [];
    }
    if (response.statusCode != 200) {
      throw Exception('Falha ao carregar seus endereços (status ${response.statusCode}).');
    }

    final data = jsonDecode(utf8.decode(response.bodyBytes));
    if (data is! List) return const [];

    return data
        .whereType<Map<String, dynamic>>()
        .map(Endereco.fromJson)
        .toList();
  }

  /// POST /api/clientes/meus-enderecos.
  Future<Endereco> adicionar(Endereco endereco) async {
    final response = await ApiClient.post('/api/clientes/meus-enderecos', endereco.toRequestJson());
    if (response.statusCode != 201) {
      throw Exception(_extrairMensagem(response.body) ?? 'Não foi possível salvar o endereço.');
    }
    return Endereco.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
  }

  /// PUT /api/clientes/meus-enderecos/{id}.
  Future<Endereco> atualizar(int id, Endereco endereco) async {
    final response = await ApiClient.put('/api/clientes/meus-enderecos/$id', endereco.toRequestJson());
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ?? 'Não foi possível salvar o endereço.');
    }
    return Endereco.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
  }

  /// DELETE /api/clientes/meus-enderecos/{id}.
  Future<void> remover(int id) async {
    final response = await ApiClient.delete('/api/clientes/meus-enderecos/$id');
    if (response.statusCode != 204) {
      throw Exception(_extrairMensagem(response.body) ?? 'Não foi possível remover o endereço.');
    }
  }

  /// PATCH /api/clientes/meus-enderecos/{id}/principal.
  Future<Endereco> definirPrincipal(int id) async {
    final response = await ApiClient.patch('/api/clientes/meus-enderecos/$id/principal', const {});
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ?? 'Não foi possível definir como principal.');
    }
    return Endereco.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
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
