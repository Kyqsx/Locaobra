import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/slug_resonse.dart';

class SlugService {
  final String baseUrl = 'https://locaobra-7c7d.vercel.app';

  Future<SlugResonse> buscarPorSlug(String categoriaSlug) async {
    final response = await http.get(Uri.parse('$baseUrl/catalogo/$categoriaSlug'));

    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return SlugResonse.fromJson(data);
    } else {
      throw Exception('Falha ao carregar os dados do catálogo');
    }
  }
}