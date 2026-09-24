import 'package:flutter/material.dart';

/// Estrelas de avaliação (0–5), com meia estrela — equivalente ao
/// componente Estrelas do locaobra_web.
class Estrelas extends StatelessWidget {
  final double valor;
  final double tamanho;

  const Estrelas({super.key, required this.valor, this.tamanho = 14});

  @override
  Widget build(BuildContext context) {
    // Arredonda para o meio ponto mais próximo (4.3 -> 4.5, 4.2 -> 4.0).
    final arredondado = (valor * 2).round() / 2;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (indice) {
        final posicao = indice + 1;
        final IconData icone;
        if (arredondado >= posicao) {
          icone = Icons.star;
        } else if (arredondado >= posicao - 0.5) {
          icone = Icons.star_half;
        } else {
          icone = Icons.star_border;
        }
        return Icon(icone, size: tamanho, color: Colors.orange);
      }),
    );
  }
}
