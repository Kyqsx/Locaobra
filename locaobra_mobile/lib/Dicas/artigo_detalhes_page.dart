import 'package:flutter/material.dart';
import 'package:locaobra_mobile/Dicas/dicas_locaobra_page.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/models/artigo.dart';

// Tela de artigo completo: breadcrumb, título, autor/data, imagem,
// introdução, itens numerados do corpo e uma caixa de dica de segurança
// (quando o artigo tiver uma).
class ArtigoDetalhesPage extends StatelessWidget {
  final Artigo artigo;

  const ArtigoDetalhesPage({super.key, required this.artigo});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('Dicas LocaObra'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
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
                  color: Colors.black87,
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
                    color: Colors.grey.shade600,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    artigo.autor,
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                  ),
                  const SizedBox(width: 16),
                  Icon(
                    Icons.calendar_today_outlined,
                    size: 13,
                    color: Colors.grey.shade600,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    artigo.data,
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Imagem de capa do artigo
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
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
                  color: Colors.blueGrey.shade700,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 16),
              const Divider(height: 1, color: Colors.grey),
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
                        color: Colors.grey.shade800,
                        height: 1.5,
                      ),
                      children: [
                        TextSpan(
                          text: '${index + 1}. ${item.titulo}\n',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.black87,
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
                const Divider(height: 1, color: Colors.grey),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.orange,
                    border: Border.all(color: Colors.orange),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: RichText(
                    text: TextSpan(
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.grey.shade800,
                        height: 1.4,
                      ),
                      children: [
                        const TextSpan(
                          text: 'Dica de Segurança: ',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.orange,
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
                    Icon(Icons.arrow_back, size: 16, color: Colors.orange),
                    const SizedBox(width: 6),
                    const Text(
                      'Voltar para Dicas LocaObra',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Colors.orange,
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
              color: Colors.grey.shade600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 14, color: Colors.grey.shade600),
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
              color: Colors.grey.shade600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 14, color: Colors.grey.shade600),
        Text(
          artigo.titulo,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }
}