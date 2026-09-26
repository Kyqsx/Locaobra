import 'package:flutter/material.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Mesmas cores usadas no badge de status da tela de Expedição do web
/// (statusBadge AGENDADO/EM_TRANSITO/ENTREGUE/CONCLUIDO/CANCELADO em
/// Expedicao.css), agora vindas do design system compartilhado.
Color corDoStatus(StatusExpedicao status) {
  switch (status) {
    case StatusExpedicao.agendado:
      return AppColors.gray500;
    case StatusExpedicao.emTransito:
      return AppColors.info;
    case StatusExpedicao.entregue:
      return AppColors.success;
    case StatusExpedicao.concluido:
      return AppColors.success;
    case StatusExpedicao.cancelado:
      return AppColors.error;
  }
}

class StatusExpedicaoBadge extends StatelessWidget {
  final StatusExpedicao status;

  const StatusExpedicaoBadge({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    final cor = corDoStatus(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: cor.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppRadius.full),
        border: Border.all(color: cor.withValues(alpha: 0.4)),
      ),
      child: Text(
        statusExpedicaoLabel(status),
        style: TextStyle(
          color: cor,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
