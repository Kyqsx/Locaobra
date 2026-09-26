import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/Dicas/dicas_locaobra_page.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/models/artigo.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

// Tela de artigo completo: breadcrumb, título, autor/data, imagem,
// introdução, itens numerados do corpo e uma caixa de dica de segurança
// (quando o artigo tiver uma).
class ArtigoDetalhesPage extends StatelessWidget {
  final Artigo artigo;

  const ArtigoDetalhesPage({super.key, required this.artigo});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: const HeaderVoltar(),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Breadcrumb "Início > Dicas LocaObra > Título do artigo"
              _buildBreadcrumb(context),
              const SizedBox(height: 16),

              // Título do artigo
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

              // Autor e data
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
                    style: TextStyle(fontSize: 12, color: AppColors.gray700),
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
                    style: TextStyle(fontSize: 12, color: AppColors.gray700),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Imagem de capa do artigo
              ClipRRect(
                borderRadius: BorderRadius.circular(AppRadius.xl),
                child: Image.asset(
                  artigo.imagePath,
                  width: double.infinity,
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(height: 16),

              // Parágrafo de introdução, em destaque (azul, como no site)
              Text(
                artigo.introducao,
                style: TextStyle(
                  fontSize: 14,
                  color: AppColors.steel700,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 16),
              const Divider(height: 1, color: AppColors.gray500),
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

              // Caixa de "Dica de Segurança", só aparece se o artigo tiver uma
              if (artigo.dicaSeguranca != null) ...[
                const Divider(height: 1, color: AppColors.gray500),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    border: Border.all(color: AppColors.primary),
                    borderRadius: BorderRadius.circular(AppRadius.lg),
                  ),
                  child: RichText(
                    text: TextSpan(
                      style: TextStyle(
                        fontSize: 13,
                        color: AppColors.gray800,
                        height: 1.4,
                      ),
                      children: [
                        const TextSpan(
                          text: 'Dica de Segurança: ',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: AppColors.primary,
                          ),
                        ),
                        TextSpan(text: artigo.dicaSeguranca),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 20),
              ],

              // Link para voltar à listagem
              InkWell(
                onTap: () {
                  Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(
                      builder: (_) => const DicasLocaObraPage(),
                    ),
                  );
                },
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.arrow_back, size: 16, color: AppColors.primary),
                    const SizedBox(width: 6),
                    const Text(
                      'Voltar para Dicas LocaObra',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
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
        Text(
          artigo.titulo,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}