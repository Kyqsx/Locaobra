import 'package:flutter/material.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Header padrão do app para telas empilhadas: só a seta de voltar
/// dentro de um círculo, sem título da página atual.
class HeaderVoltar extends StatelessWidget implements PreferredSizeWidget {
  /// Opcional: comportamento customizado ao tocar em voltar
  /// (ex.: `Navigator.pop(context, valor)`).
  final VoidCallback? onPop;

  /// Ações opcionais no lado direito (ex.: ícone de carrinho).
  final List<Widget>? actions;

  const HeaderVoltar({super.key, this.onPop, this.actions});

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      automaticallyImplyLeading: false,
      title: const SizedBox.shrink(),
      actions: actions,
      leadingWidth: 64,
      leading: Padding(
        padding: const EdgeInsets.only(left: 16, top: 6, bottom: 6),
        child: Material(
          color: AppColors.bgPrimary,
          shape: CircleBorder(
            side: BorderSide(color: AppColors.gray300, width: 1),
          ),
          child: InkWell(
            customBorder: const CircleBorder(),
            onTap: onPop ?? () => Navigator.of(context).maybePop(),
            child: const Center(
              child: Icon(
                Icons.arrow_back_ios_new,
                size: 17,
                color: AppColors.textPrimary,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
