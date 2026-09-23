import 'package:flutter/material.dart';
import '../models/categoria.dart';

/// Linha horizontal de abas de categoria — reaproveita o visual que
/// estava duplicado em cada página de categoria (Ferramentas_Eletricas,
/// Concretagem, etc), mas dirigida por slug em vez de nome fixo, então
/// funciona pra qualquer categoria vinda de [kCategorias].
///
/// [selectedSlug] nulo representa o "Catálogo completo".
/// [onSelect] é chamado com o slug tocado (nunca nulo, pois não há aba
/// própria pra "todos" aqui — isso fica a cargo de quem usa o widget).
class CategoryNavTabs extends StatelessWidget {
  final String? selectedSlug;
  final ValueChanged<String> onSelect;

  const CategoryNavTabs({
    super.key,
    required this.selectedSlug,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 12.0),
      child: Row(
        children: [
          for (final cat in kCategorias) ...[
            _NavTab(
              title: cat.nome,
              selecionada: cat.slug == selectedSlug,
              onTap: () => onSelect(cat.slug),
            ),
            const SizedBox(width: 24),
          ],
        ],
      ),
    );
  }
}

class _NavTab extends StatelessWidget {
  final String title;
  final bool selecionada;
  final VoidCallback onTap;

  const _NavTab({
    required this.title,
    required this.selecionada,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Text(
          title,
          style: TextStyle(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: selecionada ? Colors.orange : Colors.black87,
          ),
        ),
      ),
    );
  }
}