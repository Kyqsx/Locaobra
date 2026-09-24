import 'package:flutter/material.dart';
import '../models/equipamento.dart';
import '../utils/formatters.dart';

/// Grade (grid) de cards de equipamento — mesmo layout visual que estava
/// hardcoded em cada página de categoria (2 colunas, foto, nome, preço
/// por dia), agora alimentada com dados reais de [Equipamento] vindos da
/// API em vez da lista fixa `_produtos`.
class CategoryListagem extends StatelessWidget {
  final List<Equipamento> equipamentos;
  final ValueChanged<Equipamento>? onTapEquipamento;

  const CategoryListagem({
    super.key,
    required this.equipamentos,
    this.onTapEquipamento,
  });

  @override
  Widget build(BuildContext context) {
    if (equipamentos.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(
          child: Text(
            'Nenhum equipamento disponível nesta categoria.',
            style: TextStyle(color: Colors.black54),
          ),
        ),
      );
    }

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: equipamentos.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 16,
        mainAxisSpacing: 16,
        childAspectRatio: 0.6,
      ),
      itemBuilder: (context, index) => _ProductCard(
        equipamento: equipamentos[index],
        onTap: onTapEquipamento == null
            ? null
            : () => onTapEquipamento!(equipamentos[index]),
      ),
    );
  }
}

class _ProductCard extends StatelessWidget {
  final Equipamento equipamento;
  final VoidCallback? onTap;

  const _ProductCard({required this.equipamento, this.onTap});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Material(
        color: Colors.white,
        child: InkWell(
          onTap: onTap,
          child: Container(
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300, width: 1),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AspectRatio(
                  aspectRatio: 1,
                  child: equipamento.imagemPrincipal.isNotEmpty
                      ? Image.network(
                          equipamento.imagemPrincipal,
                          fit: BoxFit.cover,
                          width: double.infinity,
                          errorBuilder: (_, __, ___) => const _Placeholder(),
                        )
                      : const _Placeholder(),
                ),
                Padding(
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        equipamento.nome,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                      if (equipamento.mediaAvaliacoes > 0) ...[
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            const Icon(Icons.star, size: 12, color: Colors.orange),
                            const SizedBox(width: 4),
                            Text(
                              equipamento.mediaAvaliacoes.toStringAsFixed(1),
                              style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
                            ),
                          ],
                        ),
                      ],
                      const SizedBox(height: 8),
                      const Divider(height: 1, color: Colors.grey),
                      const SizedBox(height: 8),
                      RichText(
                        text: TextSpan(
                          children: [
                            TextSpan(
                              text: formatarMoeda(equipamento.valorDiaria),
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: Colors.orange,
                              ),
                            ),
                            TextSpan(
                              text: ' / dia',
                              style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Placeholder extends StatelessWidget {
  const _Placeholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.grey.shade100,
      alignment: Alignment.center,
      child: const Text('🏗️', style: TextStyle(fontSize: 32)),
    );
  }
}