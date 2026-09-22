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
  static Future<Map<String, String>> _headers(String? tokenExplicito) async {
    final token = tokenExplicito ?? await TokenStorage.obter();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }
}