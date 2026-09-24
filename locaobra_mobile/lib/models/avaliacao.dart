/// Uma avaliação pública de equipamento — espelha AvaliacaoResponse da API.
/// O campo [autor] já vem reduzido pela API (primeiro nome + inicial).
class Avaliacao {
  final int id;
  final int nota; // 1..5
  final String? comentario;
  final String autor;
  final DateTime? criadoEm;

  const Avaliacao({
    required this.id,
    required this.nota,
    this.comentario,
    required this.autor,
    this.criadoEm,
  });

  factory Avaliacao.fromJson(Map<String, dynamic> json) {
    return Avaliacao(
      id: (json['id'] as num?)?.toInt() ?? 0,
      nota: (json['nota'] as num?)?.toInt() ?? 0,
      comentario: json['comentario']?.toString(),
      autor: json['autor']?.toString() ?? 'Cliente',
      criadoEm: DateTime.tryParse(json['criadoEm']?.toString() ?? ''),
    );
  }
}

/// Resposta de GET /api/avaliacoes/equipamento/{id}: média, total,
/// distribuição por nota (1..5) e a lista de avaliações.
class ResumoAvaliacoes {
  final double media; // 0 quando ainda não há avaliações (a API manda null)
  final int total;
  final Map<int, int> distribuicao;
  final List<Avaliacao> avaliacoes;

  const ResumoAvaliacoes({
    required this.media,
    required this.total,
    required this.distribuicao,
    required this.avaliacoes,
  });

  factory ResumoAvaliacoes.fromJson(Map<String, dynamic> json) {
    final distribuicao = <int, int>{};
    final bruta = json['distribuicao'];
    if (bruta is Map) {
      bruta.forEach((chave, valor) {
        final nota = int.tryParse('$chave');
        if (nota != null) {
          distribuicao[nota] = (valor as num?)?.toInt() ?? 0;
        }
      });
    }

    final lista = json['avaliacoes'] as List<dynamic>? ?? const [];

    return ResumoAvaliacoes(
      media: (json['media'] as num?)?.toDouble() ?? 0,
      total: (json['total'] as num?)?.toInt() ?? 0,
      distribuicao: distribuicao,
      avaliacoes: lista
          .whereType<Map<String, dynamic>>()
          .map(Avaliacao.fromJson)
          .toList(),
    );
  }
}
