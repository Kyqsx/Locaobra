import 'dart:convert';
import 'package:http/http.dart' as http;

/// Endereço devolvido pelo ViaCEP pra preencher o formulário automaticamente.
class EnderecoViaCep {
  final String rua;
  final String bairro;
  final String cidade;
  final String estado;

  const EnderecoViaCep({
    required this.rua,
    required this.bairro,
    required this.cidade,
    required this.estado,
  });

  factory EnderecoViaCep.fromJson(Map<String, dynamic> json) => EnderecoViaCep(
        rua: json['logradouro']?.toString() ?? '',
        bairro: json['bairro']?.toString() ?? '',
        cidade: json['localidade']?.toString() ?? '',
        estado: json['uf']?.toString() ?? '',
      );
}

/// Consulta o ViaCEP (https://viacep.com.br) — API pública, sem autenticação
/// — pra autocompletar rua/bairro/cidade/UF a partir do CEP digitado.
/// Chamado direto do app, sem passar pelo nosso backend.
class ViaCepService {
  /// Só os dígitos do CEP (remove pontuação e espaços).
  static String apenasDigitos(String cep) => cep.replaceAll(RegExp(r'\D'), '');

  /// Formata os dígitos como "00000-000" enquanto o usuário digita.
  static String formatar(String valor) {
    final digitos = apenasDigitos(valor);
    final limitado = digitos.length > 8 ? digitos.substring(0, 8) : digitos;
    if (limitado.length <= 5) return limitado;
    return '${limitado.substring(0, 5)}-${limitado.substring(5)}';
  }

  /// Busca o endereço pelo CEP. Devolve null se o CEP não tiver 8 dígitos ou
  /// se o ViaCEP não encontrar nada. Lança exceção só em falha de rede/HTTP,
  /// pra quem chama poder distinguir "não encontrado" de "sem internet".
  static Future<EnderecoViaCep?> buscar(String cep) async {
    final digitos = apenasDigitos(cep);
    if (digitos.length != 8) return null;

    final resposta = await http
        .get(Uri.parse('https://viacep.com.br/ws/$digitos/json/'))
        .timeout(const Duration(seconds: 10));

    if (resposta.statusCode != 200) {
      throw Exception('ViaCEP respondeu status ${resposta.statusCode}');
    }

    final dados = jsonDecode(resposta.body) as Map<String, dynamic>;
    if (dados['erro'] == true) return null;

    return EnderecoViaCep.fromJson(dados);
  }
}
