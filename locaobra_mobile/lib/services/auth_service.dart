import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

/// Resultado de uma tentativa de login.
class LoginResult {
  final bool sucesso;
  final String? token;
  final String? mensagemErro;

  LoginResult.sucesso(this.token) : sucesso = true, mensagemErro = null;
  LoginResult.erro(this.mensagemErro) : sucesso = false, token = null;
}

class AuthService {
  // Mesma base usada pelo SlugService. Ajuste o path '/auth/login' se o
  // endpoint real do backend for outro (ex: '/usuarios/login', '/login').
  final String baseUrl = 'https://locaobra-7c7d.vercel.app/api';

  Future<LoginResult> login(String email, String senha) async {
    final url = Uri.parse('$baseUrl/auth/login');

    debugPrint('[AuthService] --> POST $url');
    debugPrint('[AuthService] --> body: {"email": "$email", "senha": "***"}');

    try {
      final response = await http
          .post(
            url,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email, 'senha': senha}),
          )
          .timeout(const Duration(seconds: 10));

      debugPrint('[AuthService] <-- status: ${response.statusCode}');
      debugPrint('[AuthService] <-- body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        // Ajuste a chave abaixo conforme o campo real que a API devolver
        // (pode ser "token", "accessToken", "jwt", etc).
        final token = data is Map<String, dynamic>
            ? (data['token'] ?? data['accessToken'] ?? data['jwt'])
            : null;
        return LoginResult.sucesso(token?.toString());
      }

      if (response.statusCode == 401 || response.statusCode == 403) {
        return LoginResult.erro('E-mail ou senha inválidos.');
      }

      return LoginResult.erro(
        'Falha no login (status ${response.statusCode}).',
      );
    } catch (e) {
      debugPrint('[AuthService] !!! erro: $e');
      return LoginResult.erro('Não foi possível conectar ao servidor.');
    }
  }
}
