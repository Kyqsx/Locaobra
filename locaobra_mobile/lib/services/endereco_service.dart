import 'dart:convert';
import 'package:locaobra_mobile/models/endereco.dart';
import 'api_client.dart';

/// CRUD de endereços do cliente logado — equivalente ao que
/// `Perfil/meusEnderecos.jsx` consome no web. Este patch usa só a leitura
/// (checkout); o CRUD completo (adicionar/editar/remover/definir principal)
/// entra na tela "Meus Endereços" do próximo patch.
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
}
