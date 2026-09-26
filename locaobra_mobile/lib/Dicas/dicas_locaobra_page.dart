import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/Dicas/artigo_detalhes_page.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/models/artigo.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

// Tela de listagem "Dicas LocaObra": breadcrumb, título, subtítulo e uma
// grade de cards de artigo (foto, título, resumo, autor e data).
class DicasLocaObraPage extends StatelessWidget {
  const DicasLocaObraPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: SingleChildScrollView(
        // Status bar + toolbar no padding do scroll (sem SafeArea), pra
        // conteúdo rolar por baixo do header transparente. Lateral 6px.
        padding: EdgeInsets.fromLTRB(
          6,
          MediaQuery.of(context).padding.top + kToolbarHeight + 8,
          6,
          8,
        ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Breadcrumb "Início > Dicas LocaObra"
              _buildBreadcrumb(context),
              const SizedBox(height: 16),

              const Text(
                'Dicas LocaObra',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Conteúdo para ajudar você a planejar sua obra e aproveitar '
                'melhor os equipamentos alugados.',
                style: TextStyle(fontSize: 13, color: AppColors.gray700),
              ),
              const SizedBox(height: 16),

              // Grade de artigos: 2 colunas
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: artigosDisponiveis.length,
                gridDelegate:
                    const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      crossAxisSpacing: 12,
                      mainAxisSpacing: 12,
                      childAspectRatio: 0.68,
                    ),
                itemBuilder: (context, index) {
                  return _buildArtigoCard(context, artigosDisponiveis[index]);
                },
              ),
            ],
          ),
        ),
    );
  }

  // Trilha "Início > Dicas LocaObra". "Início" volta pra tela inicial.
  Widget _buildBreadcrumb(BuildContext context) {
    return Row(
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
              fontSize: 13,
              color: AppColors.gray600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 16, color: AppColors.gray600),
        const Text(
          'Dicas LocaObra',
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }

  // Card de artigo: foto no topo, título, resumo, autor e data embaixo.
  // Mesmo visual dos cards do catálogo: borda no shape da Material (acompanha
  // os cantos arredondados) e sem divisória interna.
  Widget _buildArtigoCard(BuildContext context, Artigo artigo) {
    return Material(
      color: AppColors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.xl),
        side: const BorderSide(color: AppColors.gray300, width: 1),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => ArtigoDetalhesPage(artigo: artigo),
              ),
            );
          },
          child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AspectRatio(
                  aspectRatio: 1.6,
                  child: Image.asset(
                    artigo.imagePath,
                    fit: BoxFit.cover,
                    width: double.infinity,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        artigo.titulo,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        artigo.resumo,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 11,
                          color: AppColors.gray600,
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 8),
                      // Linha com autor e data
                      Row(
                        children: [
                          Icon(
                            Icons.person_outline,
                            size: 12,
                            color: AppColors.gray600,
                          ),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              artigo.autor,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 10,
                                color: AppColors.gray600,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Row(
                        children: [
                          Icon(
                            Icons.calendar_today_outlined,
                            size: 11,
                            color: AppColors.gray600,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            artigo.data,
                            style: TextStyle(
                              fontSize: 10,
                              color: AppColors.gray600,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
    );
  }
}