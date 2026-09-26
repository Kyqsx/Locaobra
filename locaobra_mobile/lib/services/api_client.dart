import 'dart:convert';
import 'package:http/http.dart' as http;
import 'token_storage.dart';

/// Base da API, igual ao service/api.js do locaobra_web.
/// Os paths de cada chamada já incluem o prefixo '/api'.
const String kApiBaseUrl = 'https://locaobra-7c7d.vercel.app';
// const String kApiBaseUrl = 'http://localhost:8080'; // para dev local, com o web rodando em localhost:3000

class ApiClient {
  static Uri _uri(String path) => Uri.parse('$kApiBaseUrl$path');

  /// A API pode devolver caminhos relativos de imagem (ex: '/uploads/x.jpg').
  /// Se já vier absoluto (http/https), usa direto; senão, prefixa com a
  /// baseURL — igual o web faz em `${api.defaults.baseURL}${path}`
  /// (utils/imagem.js: imageUrl).
  static String resolveUrl(String? path) {
    if (path == null || path.isEmpty) return '';
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return '$kApiBaseUrl$path';
  }

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

  static Future<http.Response> patch(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) async {
    final headers = await _headers(token);
    return http
        .patch(_uri(path), headers: headers, body: jsonEncode(body))
        .timeout(const Duration(seconds: 10));
  }

  static Future<http.Response> put(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) async {
    final headers = await _headers(token);
    return http
        .put(_uri(path), headers: headers, body: jsonEncode(body))
        .timeout(const Duration(seconds: 10));
  }

  static Future<http.Response> delete(String path, {String? token}) async {
    final headers = await _headers(token);
    return http.delete(_uri(path), headers: headers).timeout(const Duration(seconds: 10));
  }

  /// POST multipart/form-data — usado por endpoints que recebem arquivos
  /// (ex.: /api/expedicoes/{id}/confirmar-entrega, que exige a foto e a
  /// assinatura desenhada como imagens, igual o web faz com FormData).
  ///
  /// [campos] vira RequestPart de texto; [arquivos] vira RequestPart de
  /// arquivo, cada um com bytes + nome já prontos (ver MultipartFile.fromBytes
  /// nas chamadas). Timeout maior que os demais métodos porque upload de
  /// foto pela rede do cliente pode demorar mais que os 10s padrão.
  static Future<http.StreamedResponse> postMultipart(
    String path, {
    required Map<String, String> campos,
    required Map<String, http.MultipartFile> arquivos,
    String? token,
  }) async {
    final headers = await _headers(token);
    headers.remove('Content-Type'); // o http seta o boundary sozinho

    final request = http.MultipartRequest('POST', _uri(path))
      ..headers.addAll(headers)
      ..fields.addAll(campos);
    arquivos.forEach((nomeCampo, arquivo) {
      request.files.add(arquivo);
    });

    return request.send().timeout(const Duration(seconds: 30));
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
