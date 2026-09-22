import 'package:locaobra_mobile/services/api_client.dart';

/// Espelha o objeto "equipamento" devolvido por GET /api/equipamentos
/// (mesmo formato consumido pelo locaobra_web).
class Equipamento {
  final int id;
  final String nome;
  final String categoria;
  final List<String> imagens; // caminhos como vêm da API (ver [imagemCompleta])
  final int quantidadeDisponivel;
  final int quantidadeTotal;
  final double valorDiaria;
  final double mediaAvaliacoes;
  final String? status;
  final Map<String, String> especificacoes;

  const Equipamento({
    required this.id,
    required this.nome,
    required this.categoria,
    required this.imagens,
    required this.quantidadeDisponivel,
    required this.quantidadeTotal,
    required this.valorDiaria,
    this.mediaAvaliacoes = 0,
    this.status,
    this.especificacoes = const {},
  });

  factory Equipamento.fromJson(Map<String, dynamic> json) {
    final imagensJson = json['imagens'] as List<dynamic>? ?? [];
    return Equipamento(
      id: json['id'] is int
          ? json['id'] as int
          : int.tryParse('${json['id']}') ?? 0,
      nome: json['nome']?.toString() ?? '',
      categoria: json['categoria']?.toString() ?? '',
      imagens: imagensJson
          .map((img) => img is Map ? (img['url']?.toString() ?? '') : '')
          .where((url) => url.isNotEmpty)
          .toList(),
      quantidadeDisponivel:
          (json['quantidadeDisponivel'] as num?)?.toInt() ?? 0,
      quantidadeTotal: (json['quantidadeTotal'] as num?)?.toInt() ?? 0,
      valorDiaria: (json['valorDiaria'] as num?)?.toDouble() ?? 0,
      mediaAvaliacoes: (json['mediaAvaliacoes'] as num?)?.toDouble() ?? 0,
      status: json['status']?.toString(),
      especificacoes: (json['especificacoes'] as Map?)?.map(
            (k, v) => MapEntry(k.toString(), v.toString()),
          ) ??
          const {},
    );
  }

  /// Atalho pra foto principal, já com URL completa (usada nos cards).
  String get imagemPrincipal =>
      imagens.isNotEmpty ? imagemCompleta(imagens.first) : '';

  /// A API pode devolver caminhos relativos de imagem (ex: '/uploads/x.jpg').
  /// Se já vier absoluto (http/https), usa direto; senão, prefixa com a
  /// baseURL da API — igual o web faz em `${api.defaults.baseURL}${path}`.
  static String imagemCompleta(String path) {
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return '$kApiBaseUrl$path';
  }
}
