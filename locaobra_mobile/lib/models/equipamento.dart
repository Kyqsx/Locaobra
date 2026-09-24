import 'package:locaobra_mobile/services/api_client.dart';

/// Uma foto do equipamento: URL (como vem da API) + ponto focal 0–100 que o
/// Admin salva para o enquadramento quadrado (o `object-position` do web).
class ImagemEquipamento {
  final String url;
  final int focoX;
  final int focoY;

  const ImagemEquipamento({required this.url, this.focoX = 50, this.focoY = 50});

  factory ImagemEquipamento.fromJson(Map<String, dynamic> json) {
    return ImagemEquipamento(
      url: json['url']?.toString() ?? '',
      focoX: ((json['focoX'] as num?)?.toInt() ?? 50).clamp(0, 100).toInt(),
      focoY: ((json['focoY'] as num?)?.toInt() ?? 50).clamp(0, 100).toInt(),
    );
  }

  /// URL pronta pra Image.network (prefixa a baseURL quando for relativa).
  String get urlCompleta => Equipamento.imagemCompleta(url);
}

/// Espelha o objeto "equipamento" devolvido por GET /api/equipamentos
/// (mesmo formato consumido pelo locaobra_web).
class Equipamento {
  final int id;
  final String nome;
  final String? descricao;
  final String categoria;
  final List<String> imagens; // caminhos como vêm da API (ver [imagemCompleta])
  final List<ImagemEquipamento> fotos; // mesmas fotos, com o ponto focal
  final int quantidadeDisponivel;
  final int quantidadeTotal;
  final double valorDiaria;
  final double mediaAvaliacoes;
  final int totalAvaliacoes;
  final String? status;
  final Map<String, String> especificacoes;
  final DateTime? criadoEm;

  const Equipamento({
    required this.id,
    required this.nome,
    this.descricao,
    required this.categoria,
    required this.imagens,
    this.fotos = const [],
    required this.quantidadeDisponivel,
    required this.quantidadeTotal,
    required this.valorDiaria,
    this.mediaAvaliacoes = 0,
    this.totalAvaliacoes = 0,
    this.status,
    this.especificacoes = const {},
    this.criadoEm,
  });

  factory Equipamento.fromJson(Map<String, dynamic> json) {
    final imagensJson = json['imagens'] as List<dynamic>? ?? [];
    return Equipamento(
      id: json['id'] is int
          ? json['id'] as int
          : int.tryParse('${json['id']}') ?? 0,
      nome: json['nome']?.toString() ?? '',
      descricao: json['descricao']?.toString(),
      categoria: json['categoria']?.toString() ?? '',
      imagens: imagensJson
          .map((img) => img is Map ? (img['url']?.toString() ?? '') : '')
          .where((url) => url.isNotEmpty)
          .toList(),
      fotos: imagensJson
          .whereType<Map>()
          .map((img) => ImagemEquipamento.fromJson(Map<String, dynamic>.from(img)))
          .where((foto) => foto.url.isNotEmpty)
          .toList(),
      quantidadeDisponivel:
          (json['quantidadeDisponivel'] as num?)?.toInt() ?? 0,
      quantidadeTotal: (json['quantidadeTotal'] as num?)?.toInt() ?? 0,
      valorDiaria: (json['valorDiaria'] as num?)?.toDouble() ?? 0,
      mediaAvaliacoes: (json['mediaAvaliacoes'] as num?)?.toDouble() ?? 0,
      totalAvaliacoes: (json['totalAvaliacoes'] as num?)?.toInt() ?? 0,
      status: json['status']?.toString(),
      especificacoes: (json['especificacoes'] as Map?)?.map(
            (k, v) => MapEntry(k.toString(), v.toString()),
          ) ??
          const {},
      criadoEm: DateTime.tryParse(json['criadoEm']?.toString() ?? ''),
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
