/// Item do carrinho — mesmo formato do item guardado pelo CartContext do web
/// (equipamentoId, nome, imagem, valorDiaria, quantidadeDisponivel,
/// quantidade, observacaoItem), mais o ponto focal da foto.
class CartItem {
  final int equipamentoId;
  final String nome;

  /// Caminho da foto principal como vem da API (relativo ou absoluto).
  /// Use `Equipamento.imagemCompleta` para montar a URL.
  final String? imagem;
  final int focoX; // 0–100, ponto focal da foto (object-position do web)
  final int focoY;
  final double valorDiaria;
  final int quantidadeDisponivel;
  final int quantidade;
  final String observacaoItem;

  const CartItem({
    required this.equipamentoId,
    required this.nome,
    this.imagem,
    this.focoX = 50,
    this.focoY = 50,
    required this.valorDiaria,
    required this.quantidadeDisponivel,
    required this.quantidade,
    this.observacaoItem = '',
  });

  /// Valor de UMA diária deste item considerando a quantidade escolhida.
  double get subtotalDiaria => valorDiaria * quantidade;

  CartItem copyWith({
    int? quantidadeDisponivel,
    int? quantidade,
    String? observacaoItem,
  }) {
    return CartItem(
      equipamentoId: equipamentoId,
      nome: nome,
      imagem: imagem,
      focoX: focoX,
      focoY: focoY,
      valorDiaria: valorDiaria,
      quantidadeDisponivel: quantidadeDisponivel ?? this.quantidadeDisponivel,
      quantidade: quantidade ?? this.quantidade,
      observacaoItem: observacaoItem ?? this.observacaoItem,
    );
  }

  Map<String, dynamic> toJson() => {
        'equipamentoId': equipamentoId,
        'nome': nome,
        'imagem': imagem,
        'focoX': focoX,
        'focoY': focoY,
        'valorDiaria': valorDiaria,
        'quantidadeDisponivel': quantidadeDisponivel,
        'quantidade': quantidade,
        'observacaoItem': observacaoItem,
      };

  factory CartItem.fromJson(Map<String, dynamic> json) {
    return CartItem(
      equipamentoId: (json['equipamentoId'] as num?)?.toInt() ?? 0,
      nome: json['nome']?.toString() ?? '',
      imagem: json['imagem']?.toString(),
      focoX: (json['focoX'] as num?)?.toInt() ?? 50,
      focoY: (json['focoY'] as num?)?.toInt() ?? 50,
      valorDiaria: (json['valorDiaria'] as num?)?.toDouble() ?? 0,
      quantidadeDisponivel: (json['quantidadeDisponivel'] as num?)?.toInt() ?? 99,
      quantidade: (json['quantidade'] as num?)?.toInt() ?? 1,
      observacaoItem: json['observacaoItem']?.toString() ?? '',
    );
  }
}
