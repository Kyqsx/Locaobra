import 'package:flutter/material.dart';

/// Ponto único de navegação para a tela do carrinho.
///
/// A tela de Carrinho + Checkout entra no próximo patch; até lá esta função
/// só avisa o usuário. Quando a tela existir, basta trocar o corpo daqui por
/// um Navigator.push — quem chama (ProductView, Home...) não precisa mudar.
void abrirCarrinho(BuildContext context) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      const SnackBar(content: Text('A tela do carrinho chega no próximo patch.')),
    );
}
