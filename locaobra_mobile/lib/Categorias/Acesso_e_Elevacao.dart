import 'package:flutter/material.dart';
import 'package:locaobra_mobile/Categorias/Concretagem.dart';
import 'package:locaobra_mobile/Categorias/Ferramentas_Eletricas.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';

// Modelo simples de produto, usado só para preencher os cards da lista.
// Depois, o ideal é isso vir de uma API/banco de dados em vez de ficar
// fixo (hardcoded) no código.
class Produto {
  final String imagePath;
  final String nome;
  final String descricao;
  final double precoPorDia;

  const Produto({
    required this.imagePath,
    required this.nome,
    required this.descricao,
    required this.precoPorDia,
  });
}

// Tela de listagem de produtos da categoria "Ferramentas Elétricas".
// Layout: cabeçalho, abas de navegação, breadcrumb + botão Filtrar,
// e uma grade (grid) de cards de produto.
class AcessoPage extends StatelessWidget {
  const AcessoPage({super.key});

  // Nome desta categoria, usado no breadcrumb e para destacar a aba certa
  // na linha de navegação.
  static const String _categoriaAtual = 'Acesso e Elevação';

  // Lista de produtos dessa categoria.
  // Substituir por dados reais (de uma API, banco local, etc).
  static const List<Produto> _produtos = [
    Produto(
      imagePath: '',
      nome: 'Guinchos de Coluna 350kg Aplicação',
      descricao:
          'Guinchos de Coluna 350kg Aplicação',
      precoPorDia: 35.00,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text(_categoriaAtual),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Linha de abas de navegação por categoria, igual à do
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24.0,
                  vertical: 12.0,
                ),
                child: Row(
                  children: [
                    _buildNavTab(
                      title: 'Acesso e Elevação',
                      onTap: () {
                        Navigator.pushReplacement(
                          context,
                          MaterialPageRoute(builder: (_) => const AcessoPage()),
                        );
                      },
                    ),
                    const SizedBox(width: 24),
                    _buildNavTab(
                      title: 'Concretagem',
                      onTap: () {
                        Navigator.pushReplacement(
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
                        Navigator.pushReplacement(
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
              const Divider(height: 1, color: Colors.grey),

              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Breadcrumb (Início > categoria) + botão Filtrar
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(child: _buildBreadcrumb(context)),
                        _buildFiltrarButton(),
                      ],
                    ),
                    const SizedBox(height: 16),
                    const Divider(height: 1, color: Colors.grey),
                    const SizedBox(height: 16),

                    // Grade de produtos: 2 colunas, cada célula usa
                    GridView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: _produtos.length,
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 2,
                            crossAxisSpacing: 16,
                            mainAxisSpacing: 16,
                            childAspectRatio: 0.6,
                          ),
                      itemBuilder: (context, index) {
                        return _buildProductCard(_produtos[index]);
                      },
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

  // Aba de navegação por categoria.
  Widget _buildNavTab({required String title, required VoidCallback onTap}) {
    final bool isSelected = title == _categoriaAtual;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Text(
          title,
          style: TextStyle(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: isSelected ? Colors.orange : Colors.black87,
          ),
        ),
      ),
    );
  }

  // "Início" agora é clicável e volta para a tela de boas-vindas.
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
              color: Colors.grey.shade600,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        Icon(Icons.chevron_right, size: 16, color: Colors.grey.shade600),
        Text(
          _categoriaAtual,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }

  // Botão "Filtrar"
  Widget _buildFiltrarButton() {
    return OutlinedButton.icon(
      onPressed: () {
        // Abrir tela de filtros
      },
      icon: const Icon(Icons.filter_list, size: 18, color: Colors.black87),
      label: const Text(
        'Filtrar',
        style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
      ),
      style: OutlinedButton.styleFrom(
        backgroundColor: Colors.white,
        side: BorderSide(color: Colors.grey.shade300),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      ),
    );
  }

  // Card individual de produto: foto, nome, descrição curta e preço/dia
  Widget _buildProductCard(Produto produto) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Material(
        color: Colors.white,
        child: InkWell(
          onTap: () {
            // Navegar para a tela de detalhes do produto
          },
          child: Container(
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300, width: 1),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Foto do produto
                AspectRatio(
                  aspectRatio: 1,
                  child: Image.asset(
                    produto.imagePath,
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
                      // Nome do produto
                      Text(
                        produto.nome,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 4),
                      // Descrição curta (limitada a 2 linhas)
                      Text(
                        produto.descricao,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey.shade600,
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Divider(height: 1, color: Colors.grey),
                      const SizedBox(height: 8),
                      // Preço por dia
                      RichText(
                        text: TextSpan(
                          children: [
                            TextSpan(
                              text:
                                  'R\$ ${produto.precoPorDia.toStringAsFixed(2)}',
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: Colors.orange,
                              ),
                            ),
                            TextSpan(
                              text: ' / dia',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.grey.shade600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
