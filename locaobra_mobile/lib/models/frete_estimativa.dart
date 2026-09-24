/// Estimativa de frete — espelha FreteEstimativaResponse. É estimativa: o
/// valor final é recalculado pelo consultor na confirmação do pedido.
class FreteEstimativa {
  final double valorFrete;
  final int? distanciaKm;
  final int? prazoEstimadoDias;

  const FreteEstimativa({
    required this.valorFrete,
    this.distanciaKm,
    this.prazoEstimadoDias,
  });

  factory FreteEstimativa.fromJson(Map<String, dynamic> json) {
    return FreteEstimativa(
      valorFrete: (json['valorFrete'] as num?)?.toDouble() ?? 0,
      distanciaKm: (json['distanciaKm'] as num?)?.toInt(),
      prazoEstimadoDias: (json['prazoEstimadoDias'] as num?)?.toInt(),
    );
  }
}
