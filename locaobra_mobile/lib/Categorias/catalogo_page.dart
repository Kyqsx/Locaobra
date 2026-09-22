import 'package:flutter/material.dart';
import '../models/slug_resonse.dart';
import '../services/slug_service.dart';

class CatalogoPagina extends StatefulWidget {
  final String categoriaSlug;

  const CatalogoPagina({super.key, required this.categoriaSlug});

  @override
  State<CatalogoPagina> createState() => _CatalogoPaginaState();
}

class _CatalogoPaginaState extends State<CatalogoPagina> {
  final SlugService _slugService = SlugService();
  late Future<SlugResonse> _futureDados;

  @override
  void initState() {
    super.initState();
    _carregarDados();
  }

  // Recarrega se o slug mudar
  @override
  void didUpdateWidget(covariant CatalogoPagina oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.categoriaSlug != widget.categoriaSlug) {
      _carregarDados();
    }
  }

  void _carregarDados() {
    setState(() {
      _futureDados = _slugService.buscarPorSlug(widget.categoriaSlug);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Catálogo: ${widget.categoriaSlug}')),
      body: FutureBuilder<SlugResonse>(
        future: _futureDados,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Erro ao carregar catálogo: ${snapshot.error}'),
            );
          }

          final dados = snapshot.data;

          if (dados == null || dados.produtos.isEmpty) {
            return const Center(
              child: Text('Nenhum item encontrado nesta categoria.'),
            );
          }

          return ListView.builder(
            itemCount: dados.produtos.length,
            itemBuilder: (context, index) {
              final produto = dados.produtos[index];
              return ListTile(
                title: Text(produto['nome'] ?? 'Sem nome'),
                subtitle: Text('Preço: R\$ ${produto['preco'] ?? '0.00'}'),
                onTap: () {
                  // Navegação para o detalhe do item se necessário
                },
              );
            },
          );
        },
      ),
    );
  }
}
