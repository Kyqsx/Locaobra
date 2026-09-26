import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/Dicas/dicas_locaobra_page.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/models/artigo.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

// Tela de artigo completo, com o mesmo design da visualização de produto:
// capa em tela cheia no topo (atrás do header transparente), fundo cinza e
// conteúdo em cards brancos com borda. Dentro dos cards: breadcrumb, título,
// autor/data, introdução, itens numerados do corpo e caixa de dica de
// segurança (quando o artigo tiver uma).
class ArtigoDetalhesPage extends StatelessWidget {
  final Artigo artigo;

  const ArtigoDetalhesPage({super.key, required this.artigo});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: SingleChildScrollView(
        // Sem padding do topo: a capa começa atrás do header transparente
        // (igual à galeria do product view). Lateral 6px no conteúdo.
        padding: const EdgeInsets.only(bottom: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Capa em tela cheia no topo
            AspectRatio(
              aspectRatio: 1.6,
              child: Image.asset(
                artigo.imagePath,
                fit: BoxFit.cover,
                width: double.infinity,
              ),
            ),

            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 12),

                  // Breadcrumb "Início > Dicas LocaObra > Título do artigo"
                  _buildBreadcrumb(context),
                  const SizedBox(height: 12),

                  // Card com título + autor/data
                  _buildCartao(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          artigo.titulo,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: AppColors.textPrimary,
                            height: 1.3,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Icon(
                              Icons.person_outline,
                              size: 14,
                              color: AppColors.gray600,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              artigo.autor,
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.gray700,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Icon(
                              Icons.calendar_today_outlined,
                              size: 13,
                              color: AppColors.gray600,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              artigo.data,
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.gray700,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Card com o conteúdo do artigo (introdução + itens + dica)
                  _buildCartao(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Introdução em destaque (azul, como no site)
                        Text(
                          artigo.introducao,
                          style: TextStyle(
                            fontSize: 14,
                            color: AppColors.steel700,
                            height: 1.5,
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Itens numerados do corpo do artigo
                        ...List.generate(artigo.itens.length, (index) {
                          final item = artigo.itens[index];
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 16),
                            child: RichText(
                              text: TextSpan(
                                style: TextStyle(
                                  fontSize: 14,
                                  color: AppColors.gray800,
                                  height: 1.5,
                                ),
                                children: [
                                  TextSpan(
                                    text: '${index + 1}. ${item.titulo}\n',
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  TextSpan(text: item.descricao),
                                ],
                              ),
                            ),
                          );
                        }),

                        // Caixa de "Dica de Segurança", só aparece se o artigo
                        // tiver uma. Texto branco sobre o laranja do tema.
                        if (artigo.dicaSeguranca != null)
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppColors.primary,
                              borderRadius: BorderRadius.circular(
                                AppRadius.lg,
                              ),
                            ),
                            child: RichText(
                              text: TextSpan(
                                style: TextStyle(
                                  fontSize: 13,
                                  color: AppColors.white,
                                  height: 1.4,
                                ),
                                children: [
                                  const TextSpan(
                                    text: 'Dica de Segurança: ',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  TextSpan(text: artigo.dicaSeguranca),
                                ],
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Card branco com borda, mesmo padrão dos cards do product view.
  Widget _buildCartao({required Widget child}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: child,
    );
  }

  // Trilha "Início > Dicas LocaObra > Título". Cada parte leva de volta
  // para a tela correspondente.
  Widget _buildBreadcrumb(BuildContext context) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        InkWell(
          onTap: () {
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (_) => const WelcomeScreen()),
              (route) => false,
            );
          },
          child: Text(
            'Início',
            style: TextStyle(
              fontSize: 12,
              color: AppColors.gray600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 14, color: AppColors.gray600),
        InkWell(
          onTap: () {
            Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => const DicasLocaObraPage()),
            );
          },
          child: Text(
            'Dicas LocaObra',
            style: TextStyle(
              fontSize: 12,
              color: AppColors.gray600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 14, color: AppColors.gray600),
        Expanded(
          child: Text(
            artigo.titulo,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
        ),
      ],
    );
  }
}

