import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:locaobra_mobile/Categorias/concretagem.dart';
import 'package:locaobra_mobile/Categorias/equipamentos_pesados.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/auth/login_page.dart';
import 'package:locaobra_mobile/Categorias/ferramentas_eletricas.dart';
import 'package:locaobra_mobile/Categorias/andaimes_e_escadas.dart';
import 'package:locaobra_mobile/Categorias/acesso_e_elevacao.dart';
import 'package:locaobra_mobile/models/artigo.dart';
import 'package:locaobra_mobile/Dicas/dicas_locaobra_page.dart';
import 'package:locaobra_mobile/Dicas/artigo_detalhes_page.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            children: [
              const SizedBox(height: 16),

              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24.0,
                  vertical: 12.0,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Image.asset(
                      'assets/imagens/Logo_LOCAOBRA.png',
                      width: 160,
                      height: 90,
                      fit: BoxFit.contain,
                    ),

                    // Botão de Entrar / Login
                    // Botão de Entrar/Cadastrar OU menu do usuário logado, dependendo do
                    // estado atual de login.
                    ValueListenableBuilder<String?>(
                      valueListenable: AuthState.usuarioLogado,
                      builder: (context, nomeUsuario, _) {
                        if (nomeUsuario == null) {
                          // Ninguém logado: mostra o botão de sempre
                          return Flexible(
                            child: ElevatedButton(
                              onPressed: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (_) => const LoginPage()),
                                );
                              },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 18,
                                  vertical: 10,
                                ),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                elevation: 0,
                              ),
                              child: const Text(
                                'Entrar ou Cadastrar',
                                textAlign: TextAlign.center,
                                style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                              ),
                            ),
                          );
                        }

                        // Usuário logado: mostra ícone + nome com menu suspenso
                        return PopupMenuButton<String>(
                          offset: const Offset(0, 40),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          onSelected: (opcao) {
                            if (opcao == 'sair') {
                              AuthState.logout();
                            }
                            // TODO: navegar para 'pedidos', 'enderecos', 'perfil' quando
                            // essas telas existirem.
                          },
                          itemBuilder: (context) => [
                            _buildMenuItem('pedidos', Icons.receipt_long, 'Meus Pedidos'),
                            _buildMenuItem('enderecos', Icons.location_on_outlined, 'Meus Endereços'),
                            _buildMenuItem('perfil', Icons.person_outline, 'Ver Perfil'),
                            const PopupMenuDivider(),
                            _buildMenuItem('sair', Icons.logout, 'Sair', cor: Colors.orange),
                          ],
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.person, color: Colors.orange, size: 18),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  nomeUsuario,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    color: Colors.orange,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 13,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

              // 1.1 Linha de abas de navegação por categoria
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Row(
                  children: [
                    _buildNavTab(
                      title: 'Acesso e Elevação',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const AcessoPage()),
                        );
                      },
                    ),
                    const SizedBox(width: 24),
                    _buildNavTab(
                      title: 'Concretagem',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const ConcretagemPage(),
                          ),
                        );
                      },
                    ),
                    const SizedBox(width: 24),
                    _buildNavTab(
                      title: 'Ferramentas Elétricas',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const FerramentasEletricasPage(),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              // 2. Container Cinza (Borda Infinita)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24.0,
                  vertical: 24.0,
                ),
                decoration: BoxDecoration(
                  image: DecorationImage(
                    image: const AssetImage('assets/imagens/homebanner1.png'),
                    fit: BoxFit.cover,
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Título
                    const Text(
                      'Equipamento certo, \nna hora certa.',
                      textAlign: TextAlign.left,
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        height: 1.2,
                      ),
                    ),
                    const SizedBox(height: 12),
                    // Subtítulo
                    const Text(
                      'Veja o que você precisa para construir o que você imagina.',
                      textAlign: TextAlign.left,
                      style: TextStyle(
                        fontSize: 14,
                        color: Color.fromARGB(255, 255, 255, 255),
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 24),
                    // Botão Ver catálogo Completo
                    ElevatedButton(
                      onPressed: () {},
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.orange,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 14,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                        elevation: 0,
                      ),
                      child: const Text(
                        'Ver catálogo completo',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // 3. Seção "Navegue por Categorias"
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Column(
                  children: [
                    const Text(
                      'Navegue por Categorias',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 24),

                    // Lista de Cards de Categorias
                    _buildCategoryCard(
                      imagePath: 'assets/imagens/ferramentas.svg',
                      title: 'Ferramentas Elétricas',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const FerramentasEletricasPage(),
                          ),
                        );
                      },
                    ),
                    _buildCategoryCard(
                      imagePath: 'assets/imagens/andaimes.svg',
                      title: 'Andaimes e Escadas',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const AndaimesEEscadasPage(),
                          ),
                        );
                      },
                    ),
                    _buildCategoryCard(
                      imagePath: 'assets/imagens/elevacao.svg',
                      title: 'Acesso e Elevação',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const AcessoPage()),
                        );
                      },
                    ),
                    _buildCategoryCard(
                      imagePath: 'assets/imagens/pesado.svg',
                      title: 'Equipamentos Pesados',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const EquipamentosPesadosPage(),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // 4. Seção "Dicas LocaObra"
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Dicas LocaObra',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    TextButton(
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const DicasLocaObraPage(),
                          ),
                        );
                      },
                      style: TextButton.styleFrom(
                        padding: EdgeInsets.zero,
                        minimumSize: Size.zero,
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            'Ver tudo',
                            style: TextStyle(
                              color: Colors.orange,
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                            ),
                          ),
                          Icon(
                            Icons.chevron_right,
                            color: Colors.orange,
                            size: 18,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              // Cards de artigo, empilhados verticalmente no mobile
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Column(
                  children: [
                    _buildArticleCard(
                      imagePath: artigosDisponiveis[0].imagePath,
                      title: artigosDisponiveis[0].titulo,
                      description: artigosDisponiveis[0].resumo,
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ArtigoDetalhesPage(
                              artigo: artigosDisponiveis[0],
                            ),
                          ),
                        );
                      },
                    ),
                    const SizedBox(height: 12),
                    _buildArticleCard(
                      imagePath: artigosDisponiveis[1].imagePath,
                      title: artigosDisponiveis[1].titulo,
                      description: artigosDisponiveis[1].resumo,
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ArtigoDetalhesPage(
                              artigo: artigosDisponiveis[1],
                            ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // 5. Seção "Dúvidas Frequentes"
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Column(
                  children: [
                    const Text(
                      'Dúvidas Frequentes',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 24),
                    _buildFaqItem(
                      question: 'Como funciona o aluguel?',
                      answer:
                          'Você escolhe o equipamento pelo site, define o período de locação e nós entregamos diretamente no seu canteiro de obras ou você retira em uma de nossas unidades.',
                    ),
                    _buildFaqItem(
                      question: 'Preciso pagar caução?',
                      answer:
                          'Sim, para equipamentos de alto valor solicitamos uma garantia (caução) que é estornada integralmente após a devolução do item em boas condições.',
                    ),
                    _buildFaqItem(
                      question: 'E se o equipamento quebrar?',
                      answer:
                          'Oferecemos suporte técnico especializado. Caso ocorra uma falha por desgaste natural, realizamos a substituição do equipamento em até 24 horas.',
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  // Card individual para as categorias
  // ClipRRect garante que o efeito de toque (ondinha) nunca vaze
  // para fora dos cantos arredondados do card.
  Widget _buildNavTab({required String title, required VoidCallback onTap}) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Text(
          title,
          style: const TextStyle(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
      ),
    );
  }

  // Item individual do menu suspenso do usuário logado
  PopupMenuItem<String> _buildMenuItem(
    String valor,
    IconData icone,
    String texto, {
    Color? cor,
  }) {
    return PopupMenuItem<String>(
      value: valor,
      child: Row(
        children: [
          Icon(icone, size: 18, color: cor ?? Colors.black87),
          const SizedBox(width: 10),
          Text(texto, style: TextStyle(color: cor ?? Colors.black87)),
        ],
      ),
    );
  }

  Widget _buildCategoryCard({
    String? imagePath,
    required String title,
    required VoidCallback onTap,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Material(
          color: Colors.white,
          child: InkWell(
            onTap: onTap,
            splashColor: Colors.orange,
            highlightColor: Colors.orange,
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 20),
              width: double.infinity,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300, width: 1),
              ),
              child: Column(
                children: [
                  SvgPicture.asset(imagePath!, width: 36, height: 36),
                  const SizedBox(height: 10),
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  // Card de artigo para a seção "Dicas LocaObra"
  Widget _buildArticleCard({
    required String imagePath,
    required String title,
    required String description,
    required VoidCallback onTap,
  }) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Material(
        color: Colors.white,
        child: InkWell(
          onTap: onTap,
          child: Container(
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300, width: 1),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Imagem à esquerda
                ClipRRect(
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(12),
                    bottomLeft: Radius.circular(12),
                  ),
                  child: Image.asset(
                    imagePath,
                    width: 130,
                    height: 140,
                    fit: BoxFit.cover,
                  ),
                ),

                // Texto à direita
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: Colors.black87,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey.shade600,
                            height: 1.3,
                          ),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Ler Artigo',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.orange,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // Item individual da seção "Dúvidas Frequentes"
  Widget _buildFaqItem({required String question, required String answer}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Theme(
          data: ThemeData().copyWith(dividerColor: Colors.transparent),
          child: ExpansionTile(
            shape: RoundedRectangleBorder(
              side: const BorderSide(color: Colors.orange),
              borderRadius: BorderRadius.circular(12),
            ),
            collapsedShape: RoundedRectangleBorder(
              side: BorderSide(color: Colors.orange),
              borderRadius: BorderRadius.circular(12),
            ),
            iconColor: Colors.orange,
            collapsedIconColor: Colors.orange,
            leading: SvgPicture.asset(
              'assets/imagens/interroga.svg',
              width: 22,
              height: 22,
            ),
            title: Text(
              question,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  answer,
                  style: TextStyle(
                    fontSize: 13,
                    color: Colors.grey.shade700,
                    height: 1.4,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}