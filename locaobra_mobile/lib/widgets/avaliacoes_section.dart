import 'package:flutter/material.dart';
import 'package:locaobra_mobile/models/avaliacao.dart';
import 'package:locaobra_mobile/services/avaliacao_service.dart';
import 'package:locaobra_mobile/utils/formatters.dart';
import 'package:locaobra_mobile/widgets/estrelas.dart';

/// Seção "Avaliações" da tela de produto: resumo (média + distribuição por
/// nota) e lista de comentários. Somente leitura — o formulário de
/// avaliar/editar/excluir do web fica para um patch próprio.
class AvaliacoesSection extends StatefulWidget {
  final int equipamentoId;

  const AvaliacoesSection({super.key, required this.equipamentoId});

  @override
  State<AvaliacoesSection> createState() => _AvaliacoesSectionState();
}

class _AvaliacoesSectionState extends State<AvaliacoesSection> {
  static const int _limiteInicial = 5;

  final AvaliacaoService _service = AvaliacaoService();
  late Future<ResumoAvaliacoes> _futuro;
  bool _mostrarTodas = false;

  @override
  void initState() {
    super.initState();
    _futuro = _service.listarPorEquipamento(widget.equipamentoId);
  }

  void _recarregar() {
    setState(() {
      _futuro = _service.listarPorEquipamento(widget.equipamentoId);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Avaliações',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 12),
          FutureBuilder<ResumoAvaliacoes>(
            future: _futuro,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              if (snapshot.hasError) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Não foi possível carregar as avaliações.',
                      style: TextStyle(color: Colors.grey.shade700),
                    ),
                    TextButton(
                      onPressed: _recarregar,
                      child: const Text('Tentar novamente'),
                    ),
                  ],
                );
              }
              return _buildConteudo(snapshot.data!);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildConteudo(ResumoAvaliacoes resumo) {
    if (resumo.total == 0 || resumo.avaliacoes.isEmpty) {
      return Text(
        'Ainda não há avaliações para este equipamento.',
        style: TextStyle(color: Colors.grey.shade700),
      );
    }

    final visiveis = _mostrarTodas
        ? resumo.avaliacoes
        : resumo.avaliacoes.take(_limiteInicial).toList();
    final restantes = resumo.avaliacoes.length - visiveis.length;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildResumo(resumo),
        const SizedBox(height: 8),
        const Divider(height: 24),
        for (var i = 0; i < visiveis.length; i++) ...[
          if (i > 0) const Divider(height: 24),
          _buildAvaliacao(visiveis[i]),
        ],
        if (restantes > 0) ...[
          const SizedBox(height: 8),
          TextButton(
            onPressed: () => setState(() => _mostrarTodas = true),
            child: Text('Ver todas ($restantes a mais)'),
          ),
        ],
      ],
    );
  }

  Widget _buildResumo(ResumoAvaliacoes resumo) {
    final rotulo = resumo.total == 1 ? 'avaliação' : 'avaliações';

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              formatarMedia(resumo.media),
              style: const TextStyle(
                fontSize: 36,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            Estrelas(valor: resumo.media, tamanho: 16),
            const SizedBox(height: 4),
            Text(
              '${resumo.total} $rotulo',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
            ),
          ],
        ),
        const SizedBox(width: 24),
        Expanded(
          child: Column(
            children: [
              for (var nota = 5; nota >= 1; nota--)
                _buildBarraDistribuicao(
                  nota,
                  resumo.distribuicao[nota] ?? 0,
                  resumo.total,
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildBarraDistribuicao(int nota, int quantidade, int total) {
    final proporcao = total > 0 ? quantidade / total : 0.0;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          SizedBox(
            width: 10,
            child: Text(
              '$nota',
              style: TextStyle(fontSize: 11, color: Colors.grey.shade700),
            ),
          ),
          const Icon(Icons.star, size: 11, color: Colors.orange),
          const SizedBox(width: 6),
          Expanded(
            child: LinearProgressIndicator(
              value: proporcao,
              minHeight: 6,
              color: Colors.orange,
              backgroundColor: Colors.grey.shade200,
              borderRadius: BorderRadius.circular(3),
            ),
          ),
          const SizedBox(width: 6),
          SizedBox(
            width: 22,
            child: Text(
              '$quantidade',
              textAlign: TextAlign.right,
              style: TextStyle(fontSize: 11, color: Colors.grey.shade700),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvaliacao(Avaliacao avaliacao) {
    final comentario = avaliacao.comentario?.trim() ?? '';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                avaliacao.autor,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
            ),
            if (avaliacao.criadoEm != null)
              Text(
                formatarData(avaliacao.criadoEm!),
                style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
              ),
          ],
        ),
        const SizedBox(height: 4),
        Estrelas(valor: avaliacao.nota.toDouble(), tamanho: 14),
        if (comentario.isNotEmpty) ...[
          const SizedBox(height: 6),
          Text(
            comentario,
            style: TextStyle(fontSize: 13, color: Colors.grey.shade800),
          ),
        ],
      ],
    );
  }
}
