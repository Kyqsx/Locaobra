import 'package:flutter/material.dart';
import 'package:locaobra_mobile/models/produto.dart';

// Tela de detalhes de um produto: galeria de fotos, descrição,
// especificações técnicas, seletor de quantidade e botões de compra.
//
// É um StatefulWidget porque a foto principal e a quantidade escolhida
// mudam conforme o usuário toca nas miniaturas ou nos botões +/-.
class ProdutoDetalhesPage extends StatefulWidget {
  final Produto produto;

  const ProdutoDetalhesPage({super.key, required this.produto});

  @override
  State<ProdutoDetalhesPage> createState() => _ProdutoDetalhesPageState();
}

class _ProdutoDetalhesPageState extends State<ProdutoDetalhesPage> {
  // Índice da foto atualmente exibida em destaque
  int _fotoSelecionada = 0;
  // Quantidade escolhida pelo usuário (começa em 1)
  int _quantidade = 1;

  @override
  Widget build(BuildContext context) {
    final produto = widget.produto;
    final double valorEstimado = produto.precoPorDia * _quantidade;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(produto.nome),
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
              // Foto principal em destaque
              AspectRatio(
                aspectRatio: 1,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: Image.asset(
                    produto.imagePaths[_fotoSelecionada],
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Miniaturas: tocar em uma troca a foto principal
              if (produto.imagePaths.length > 1)
                SizedBox(
                  height: 64,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: produto.imagePaths.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
                    itemBuilder: (context, index) {
                      final bool selecionada = index == _fotoSelecionada;
                      return InkWell(
                        onTap: () {
                          setState(() {
                            _fotoSelecionada = index;
                          });
                        },
                        child: Container(
                          width: 64,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: selecionada
                                  ? Colors.orange
                                  : Colors.grey.shade300,
                              width: selecionada ? 2 : 1,
                            ),
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(6),
                            child: Image.asset(
                              produto.imagePaths[index],
                              fit: BoxFit.cover,
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),

              const SizedBox(height: 20),

              // Nome do produto
              Text(
                produto.nome,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: 10),

              // Descrição completa
              Text(
                produto.descricao,
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey.shade700,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 12),

              // Estrelinhas de avaliação
              _buildAvaliacao(produto.avaliacao),
              const SizedBox(height: 16),

              // Categoria, disponibilidade e valor da diária
              _buildInfoLinha('Categoria:', produto.categoria),
              const SizedBox(height: 6),
              _buildInfoLinha(
                'Disponíveis:',
                '${produto.quantidadeDisponivel} unidades',
              ),
              const SizedBox(height: 6),
              _buildInfoLinha(
                'Valor diária:',
                'R\$ ${produto.precoPorDia.toStringAsFixed(2)}',
              ),

              if (produto.especificacoes.isNotEmpty) ...[
                const SizedBox(height: 20),
                const Divider(height: 1, color: Colors.grey),
                const SizedBox(height: 16),
                const Text(
                  'Especificações Técnicas',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 12),
                // Grade de especificações, 2 colunas
                GridView.count(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  crossAxisCount: 2,
                  childAspectRatio: 3.2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 8,
                  children: produto.especificacoes.entries
                      .map((e) => _buildInfoLinha('${e.key}:', e.value))
                      .toList(),
                ),
              ],

              const SizedBox(height: 20),
              const Divider(height: 1, color: Colors.grey),
              const SizedBox(height: 16),

              // Seletor de quantidade
              const Text(
                'Quantidade',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                '(${produto.quantidadeDisponivel} disponíveis)',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
              ),
              const SizedBox(height: 10),
              _buildSeletorQuantidade(produto),
              const SizedBox(height: 16),

              // Valor estimado (preço/dia * quantidade escolhida)
              Text(
                'Valor estimado: R\$ ${valorEstimado.toStringAsFixed(2)}',
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: 24),

              // Botão "Comprar agora" (cheio, laranja)
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () {
                    // Todo: seguir para o checkout/pagamento
                  },
                  icon: const Icon(Icons.shopping_cart),
                  label: const Text('Comprar agora'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    elevation: 0,
                  ),
                ),
              ),
              const SizedBox(height: 10),

              // Botão "Adicionar ao carrinho" (contorno, laranja)
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () {
                    // Todo: adicionar o produto ao carrinho
                  },
                  icon: const Icon(Icons.add, color: Colors.orange),
                  label: const Text(
                    'Adicionar ao carrinho',
                    style: TextStyle(color: Colors.orange),
                  ),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: Colors.orange),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // Linha "Rótulo: valor", reaproveitada em várias partes da tela
  Widget _buildInfoLinha(String rotulo, String valor) {
    return RichText(
      text: TextSpan(
        style: const TextStyle(fontSize: 13, color: Colors.black87),
        children: [
          TextSpan(
            text: '$rotulo ',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          TextSpan(text: valor, style: const TextStyle(color: Colors.orange)),
        ],
      ),
    );
  }

  // Estrelinhas de avaliação (de 0 a 5), preenchidas em laranja
  Widget _buildAvaliacao(double nota) {
    return Row(
      children: [
        ...List.generate(5, (index) {
          final bool preenchida = index < nota.round();
          return Icon(
            preenchida ? Icons.star : Icons.star_border,
            size: 16,
            color: Colors.orange,
          );
        }),
        const SizedBox(width: 6),
        const Text('ativo', style: TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }

  // Botões -/+ com a quantidade escolhida no meio.
  // Não deixa passar de 1 nem do limite disponível do produto.
  Widget _buildSeletorQuantidade(Produto produto) {
    return Row(
      children: [
        _buildBotaoQuantidade(
          icon: Icons.remove,
          onTap: () {
            if (_quantidade > 1) {
              setState(() => _quantidade--);
            }
          },
        ),
        Container(
          width: 48,
          alignment: Alignment.center,
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
          ),
          child: Text(
            '$_quantidade',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        _buildBotaoQuantidade(
          icon: Icons.add,
          onTap: () {
            if (_quantidade < produto.quantidadeDisponivel) {
              setState(() => _quantidade++);
            }
          },
        ),
      ],
    );
  }

  Widget _buildBotaoQuantidade({
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: 40,
        height: 40,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
        ),
        child: Icon(icon, size: 18, color: Colors.black87),
      ),
    );
  }
}