import 'dart:convert';
import 'package:http/http.dart' as http;
import 'token_storage.dart';

/// Base da API, igual ao service/api.js do locaobra_web.
/// Os paths de cada chamada já incluem o prefixo '/api'.
const String kApiBaseUrl = 'https://locaobra-7c7d.vercel.app';

class ApiClient {
  static Uri _uri(String path) => Uri.parse('$kApiBaseUrl$path');

  static Future<http.Response> get(String path, {String? token}) async {
    final headers = await _headers(token);
    return http
        .get(_uri(path), headers: headers)
        .timeout(const Duration(seconds: 10));
  }

  static Future<http.Response> post(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) async {
    final headers = await _headers(token);
    return http
        .post(_uri(path), headers: headers, body: jsonEncode(body))
        .timeout(const Duration(seconds: 10));
  }

  /// Se [tokenExplicito] não for passado, busca o token salvo pelo login
  /// automaticamente (TokenStorage) — não precisa mais repassar em toda
  /// chamada, só quando quiser forçar um token diferente do salvo.
  ///
  /// Se a leitura do storage falhar por qualquer motivo (ex.: peculiaridades
  /// do flutter_secure_storage no Flutter Web), segue sem token em vez de
  /// derrubar a chamada — assim uma falha de storage não vira, pro usuário,
  /// um falso "não foi possível conectar ao servidor".
  static Future<Map<String, String>> _headers(String? tokenExplicito) async {
    String? token = tokenExplicito;
    if (token == null) {
      try {
        token = await TokenStorage.obter();
      } catch (e) {
        // ignore: avoid_print
        print('ApiClient: falha ao ler token salvo ($e) — seguindo sem token.');
      }
    }
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }
}
