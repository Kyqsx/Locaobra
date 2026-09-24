import 'package:flutter/material.dart';
import 'package:locaobra_mobile/screens/carrinho_page.dart';

/// Ponto único de navegação para a tela do carrinho — quem chama
/// (ProductView, Home...) não precisa saber como a tela é montada.
void abrirCarrinho(BuildContext context) {
  Navigator.of(context).push(
    MaterialPageRoute(builder: (_) => const CarrinhoPage()),
  );
}
