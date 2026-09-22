import 'dart:convert';
import 'api_client.dart';
import 'token_storage.dart';

/// Resultado de uma tentativa de login, espelhando a resposta real da API:
/// { token, tipo, id, nome }.
class LoginResult {
  final bool sucesso;
  final String? token;
  final String? tipo;
  final int? id;
  final String? nome;
  final String? mensagemErro;

  LoginResult.sucesso({this.token, this.tipo, this.id, this.nome})
    : sucesso = true,
      mensagemErro = null;

  LoginResult.erro(this.mensagemErro)
    : sucesso = false,
      token = null,
      tipo = null,
      id = null,
      nome = null;
}

/// Resultado de um cadastro. A API responde 201 quando a conta é criada mas
/// ainda exige verificação de e-mail antes do primeiro login (igual ao web).
class CadastroResult {
  final bool sucesso;
  final bool precisaVerificarEmail;
  final String? mensagemErro;

  CadastroResult.sucesso({this.precisaVerificarEmail = true})
    : sucesso = true,
      mensagemErro = null;

  CadastroResult.erro(this.mensagemErro)
    : sucesso = false,
      precisaVerificarEmail = false;
}

class AuthService {
  Future<LoginResult> login(String email, String senha) async {
    try {
      final response = await ApiClient.post('/api/auth/login', {
        'email': email,
        'senha': senha,
      });

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data is! Map<String, dynamic> || data['token'] == null) {
          return LoginResult.erro('Resposta inválida do servidor.');
        }

        final token = data['token'].toString();
        try {
          await TokenStorage.salvar(token);
        } catch (e) {
          // ignore: avoid_print
          print('AuthService: falha ao salvar token ($e) — login prossegue mesmo assim.');
        }

        return LoginResult.sucesso(
          token: token,
          tipo: data['tipo']?.toString(),
          id: data['id'] is int ? data['id'] as int : int.tryParse('${data['id']}'),
          nome: data['nome']?.toString(),
        );
      }

      if (response.statusCode == 401 || response.statusCode == 403) {
        return LoginResult.erro('E-mail ou senha inválidos.');
      }

      return LoginResult.erro(_extrairMensagem(response.body) ??
          'Falha no login (status ${response.statusCode}).');
    } catch (_) {
      return LoginResult.erro('Não foi possível conectar ao servidor.');
    }
  }

  Future<CadastroResult> cadastrar({
    required String nome,
    required String email,
    required String senha,
  }) async {
    try {
      final response = await ApiClient.post('/api/auth/signup', {
        'nome': nome,
        'email': email,
        'senha': senha,
        'tipo': 'CLIENTE',
      });

      if (response.statusCode == 201) {
        return CadastroResult.sucesso();
      }

      return CadastroResult.erro(
        _extrairMensagem(response.body) ??
            'Erro ao cadastrar. Verifique os dados e tente novamente.',
      );
    } catch (_) {
      return CadastroResult.erro('Não foi possível conectar ao servidor.');
    }
  }

  /// Remove o token salvo — chamar ao deslogar o usuário.
  Future<void> logout() => TokenStorage.limpar();

  /// A API costuma responder erros como {status, message, timestamp}.
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
