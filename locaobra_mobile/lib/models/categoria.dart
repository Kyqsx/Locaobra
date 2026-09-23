import 'package:flutter/material.dart';

/// Categorias do catálogo. O [slug] é o que vai na URL/rota e é comparado
/// (sem diferenciar maiúsculas/minúsculas) com o campo `categoria` que
/// vem de cada equipamento na API — exatamente como o locaobra_web faz em
/// `Home/index.jsx` (lista fixa de categorias) e `Catalogo/catalogo.jsx`
/// (filtro `eq.categoria.toLowerCase() === slug.toLowerCase()`).
///
/// Importante: como a API não tem uma entidade "Categoria" própria (o
/// campo `categoria` do equipamento é só um texto livre digitado no
/// Admin), o valor cadastrado ali precisa ser igual a um desses slugs
/// para o item aparecer na categoria certa.
class Categoria {
  final String nome;
  final String slug;
  final IconData icon;
  final String? imagePath; // asset .svg usado no card grande da Home (opcional)

  const Categoria({
    required this.nome,
    required this.slug,
    required this.icon,
    this.imagePath,
  });
}

const List<Categoria> kCategorias = [
  Categoria(
    nome: 'Ferramentas Elétricas',
    slug: 'ferramentas-eletricas',
    icon: Icons.electrical_services,
    imagePath: 'assets/imagens/ferramentas.svg',
  ),
  Categoria(
    nome: 'Andaimes e Escadas',
    slug: 'andaimes',
    icon: Icons.stairs,
    imagePath: 'assets/imagens/andaimes.svg',
  ),
  Categoria(
    nome: 'Acesso e Elevação',
    slug: 'acesso-elevacao',
    icon: Icons.elevator,
    imagePath: 'assets/imagens/elevacao.svg',
  ),
  Categoria(
    nome: 'Equipamentos Pesados',
    slug: 'equipamentos-pesados',
    icon: Icons.construction,
    imagePath: 'assets/imagens/pesado.svg',
  ),
  // Existia como página própria (Concretagem.dart) mas não tem
  // equivalente no locaobra_web nem asset de imagem ainda — mantida na
  // lista (abas) com um ícone, sem card grande na Home.
  Categoria(
    nome: 'Concretagem',
    slug: 'concretagem',
    icon: Icons.foundation,
  ),
];
