import 'package:flutter/material.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart';

/// Mesmas cores usadas no badge de status da tela de Expedição do web
/// (statusBadge AGENDADO/EM_TRANSITO/ENTREGUE/CONCLUIDO/CANCELADO em
/// Expedicao.css), adaptadas pro Material.
Color corDoStatus(StatusExpedicao status) {
  switch (status) {
    case StatusExpedicao.agendado:
      return const Color(0xFF6B7280); // cinza
    case StatusExpedicao.emTransito:
      return const Color(0xFF2563EB); // azul
    case StatusExpedicao.entregue:
      return const Color(0xFF16A34A); // verde
    case StatusExpedicao.concluido:
      return const Color(0xFF15803D); // verde escuro
    case StatusExpedicao.cancelado:
      return const Color(0xFFDC2626); // vermelho
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
        color: cor.withOpacity(0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: cor.withOpacity(0.4)),
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
