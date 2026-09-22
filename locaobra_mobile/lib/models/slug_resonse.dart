class SlugResonse {
  final String categoriaSlug;
  final String nomeCategorias;
  final List<dynamic> produtos;

  SlugResonse({
    required this.categoriaSlug,
    required this.nomeCategorias,
    required this.produtos,
  });

  factory SlugResonse.fromJson(Map<String, dynamic> json) {
    return SlugResonse(
      categoriaSlug: json['categoriaSlug'] ?? '',
      nomeCategorias: json['nomeCategoria'] ?? '',
      produtos: json['produtos'] ?? [],
    );
  }
}
