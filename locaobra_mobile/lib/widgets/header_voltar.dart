import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Header padrão do app para telas empilhadas: só a seta de voltar
/// dentro de um círculo, sem título da página atual.

/// Diâmetro único dos botões circulares do header (voltar, carrinho etc.).
/// Reduzido pra 34 pra casar com o tamanho efetivo que o botão do carrinho
/// renderiza no AppBar (o slot de actions limita o widget).
const double kHeaderCircleSize = 34;

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
      // Garante que a área da status bar sempre fique transparente (com
      // ícones escuros, já que o fundo do app é claro), em vez de herdar
      // a cor/estilo que uma tela anterior possa ter deixado configurado.
      systemOverlayStyle: const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        statusBarBrightness: Brightness.light,
      ),
      elevation: 0,
      scrolledUnderElevation: 0,
      automaticallyImplyLeading: false,
      // Sem isso o Material 3 centraliza o título e reserva no lado direito
      // (trailing) a mesma largura do leading — espremendo as actions e
      // causando overflow horizontal.
      centerTitle: false,
      titleSpacing: 0,
      title: const SizedBox.shrink(),
      actions: actions,
      // Mesma margem de 6px do leading e da página.
      actionsPadding: const EdgeInsets.only(right: 6),
      // 6 de margem + 34 do círculo, espelhando o lado direito.
      leadingWidth: kHeaderCircleSize + 12,
      leading: Padding(
        padding: const EdgeInsets.only(left: 6),
        child: Center(
          child: SizedBox.square(
            dimension: kHeaderCircleSize,
            child: HeaderCircleButton(
              icon: Icons.arrow_back_ios_new,
              iconSize: 17,
              onTap: onPop ?? () => Navigator.of(context).maybePop(),
            ),
          ),
        ),
      ),
    );
  }
}

/// Botão circular padrão do header (mesmo visual do botão de voltar), para
/// ser usado também nas ações do lado direito (ex.: carrinho), garantindo
/// que os dois lados fiquem com a mesma estética.
class HeaderCircleButton extends StatelessWidget {
  final IconData icon;
  final double iconSize;
  final VoidCallback onTap;
  final Widget? badge;

  const HeaderCircleButton({
    super.key,
    required this.icon,
    required this.onTap,
    this.iconSize = 20,
    this.badge,
  });

  @override
  Widget build(BuildContext context) {
    // O botão preenche o espaço que o container (SizedBox/leading) der —
    // o tamanho é definido por quem usa, não aqui.
    final botao = Material(
      color: AppColors.white,
      shape: CircleBorder(
        side: BorderSide(color: AppColors.gray300, width: 1),
      ),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: Center(
          child: Icon(icon, size: iconSize, color: AppColors.textPrimary),
        ),
      ),
    );

    if (badge == null) return botao;
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Positioned.fill(child: botao),
        badge!,
      ],
    );
  }
}