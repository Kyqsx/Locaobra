import 'endereco.dart';

/// Depósito disponível pro cliente retirar o equipamento — espelha
/// PontoRetiradaResponse (GET /api/pedidos/pontos-retirada).
class PontoRetirada {
  final int id;
  final String nome;
  final Endereco? endereco;

  const PontoRetirada({required this.id, required this.nome, this.endereco});

  factory PontoRetirada.fromJson(Map<String, dynamic> json) {
    final enderecoJson = json['endereco'];
    return PontoRetirada(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}') ?? 0,
      nome: json['nome']?.toString() ?? '',
      endereco: enderecoJson is Map<String, dynamic>
          ? Endereco.fromJson(enderecoJson)
          : null,
    );
  }
}
