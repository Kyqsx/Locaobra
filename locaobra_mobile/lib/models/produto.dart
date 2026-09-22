// Modelo de produto compartilhado por todas as páginas de categoria
// (Ferramentas Elétricas, Concretagem, Acesso e Elevação, etc).
//
// Ficando em um arquivo único, evita erro de "ambiguous import" quando
// duas páginas de categoria (que já se importam entre si por causa das
// abas de navegação) tentassem definir sua própria classe Produto.
class Produto {
  // Fotos do produto (a primeira é a foto principal/capa).
  final List<String> imagePaths;
  final String nome;
  final String descricao;
  final String categoria;
  final int quantidadeDisponivel;
  final double precoPorDia;
  final double avaliacao; // de 0 a 5, usado para desenhar as estrelinhas
  // Especificações técnicas, em pares de rótulo/valor
  // (ex: {'Tambor': 'Aço 2.66mm', 'Voltagem': 'Bivolt (127V / 220V)'})
  final Map<String, String> especificacoes;

  const Produto({
    required this.imagePaths,
    required this.nome,
    required this.descricao,
    required this.categoria,
    required this.quantidadeDisponivel,
    required this.precoPorDia,
    this.avaliacao = 5.0,
    this.especificacoes = const {},
  });

  // Atalho para pegar a foto principal (usada nos cards da listagem)
  String get imagemPrincipal => imagePaths.first;
}