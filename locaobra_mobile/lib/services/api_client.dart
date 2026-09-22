import 'dart:convert';
import 'package:http/http.dart' as http;

/// Base da API, igual ao service/api.js do locaobra_web.
/// Os paths de cada chamada já incluem o prefixo '/api'.
const String kApiBaseUrl = 'https://locaobra-7c7d.vercel.app';

class ApiClient {
  static Uri _uri(String path) => Uri.parse('$kApiBaseUrl$path');

  static Future<http.Response> get(String path, {String? token}) {
    return http
        .get(_uri(path), headers: _headers(token))
        .timeout(const Duration(seconds: 10));
  }

  static Future<http.Response> post(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) {
    return http
        .post(_uri(path), headers: _headers(token), body: jsonEncode(body))
        .timeout(const Duration(seconds: 10));
  }

  static Map<String, String> _headers(String? token) => {
    'Content-Type': 'application/json',
    if (token != null) 'Authorization': 'Bearer $token',
  };
}
