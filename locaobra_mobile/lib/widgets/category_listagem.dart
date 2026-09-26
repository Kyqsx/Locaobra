import 'package:flutter/material.dart';
import '../models/equipamento.dart';
import '../utils/formatters.dart';
import 'estrelas.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

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
            style: TextStyle(color: AppColors.textSecondary),
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
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
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

class _ProductCard extends StatefulWidget {
  final Equipamento equipamento;
  final VoidCallback? onTap;

  const _ProductCard({required this.equipamento, this.onTap});

  @override
  State<_ProductCard> createState() => _ProductCardState();
}

class _ProductCardState extends State<_ProductCard> {
  // Hover manual: os efeitos do InkWell (highlight/splash) são pintados
  // ATRÁS dos filhos da Material, então ficam invisíveis sobre a foto
  // opaca do equipamento. Com um overlay próprio o hover cobre o card
  // inteiro, incluindo a imagem.
  bool _hover = false;

  @override
  Widget build(BuildContext context) {
    final equipamento = widget.equipamento;

    return MouseRegion(
      cursor: widget.onTap != null
          ? SystemMouseCursors.click
          : MouseCursor.defer,
      onEnter: (_) => setState(() => _hover = true),
      onExit: (_) => setState(() => _hover = false),
      child: GestureDetector(
        onTap: widget.onTap,
        child: Stack(
          children: [
            Material(
              color: AppColors.white,
              // Borda desenhada na própria Material (com o shape arredondado),
              // e não num Container interno — assim ela acompanha os cantos
              // arredondados em vez de ser cortada pelo ClipRRect.
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppRadius.xl),
                side: const BorderSide(color: AppColors.gray300, width: 1),
              ),
              clipBehavior: Clip.antiAlias,
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
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(10),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              equipamento.nome,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                Estrelas(valor: equipamento.mediaAvaliacoes, tamanho: 12),
                                const SizedBox(width: 4),
                                Text(
                                  equipamento.totalAvaliacoes > 0
                                      ? equipamento.mediaAvaliacoes.toStringAsFixed(1)
                                      : '0.0',
                                  style: TextStyle(fontSize: 11, color: AppColors.gray600),
                                ),
                              ],
                            ),
                            // Empurra o preço pro fim do card, alinhando os
                            // preços da grade na mesma altura.
                            const Spacer(),
                            RichText(
                              text: TextSpan(
                                children: [
                                  TextSpan(
                                    text: formatarMoeda(equipamento.valorDiaria),
                                    style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.primary,
                                    ),
                                  ),
                                  TextSpan(
                                    text: ' / dia',
                                    style: TextStyle(fontSize: 11, color: AppColors.gray600),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              // Overlay do hover — fica acima da imagem também.
              if (_hover && widget.onTap != null)
                Positioned.fill(
                  child: IgnorePointer(
                    child: Container(
                      color: Colors.black.withValues(alpha: 0.06),
                    ),
                  ),
                ),
          ],
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
      color: AppColors.gray100,
      alignment: Alignment.center,
      child: const Text('🏗️', style: TextStyle(fontSize: 32)),
    );
  }
}