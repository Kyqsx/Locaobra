import 'package:flutter/material.dart';
import '../models/equipamento.dart';
import '../services/catalogo_service.dart';

class CatalogoPagina extends StatefulWidget {
  final String categoriaSlug;

  const CatalogoPagina({super.key, required this.categoriaSlug});

  @override
  State<CatalogoPagina> createState() => _CatalogoPaginaState();
}

class _CatalogoPaginaState extends State<CatalogoPagina> {
  final CatalogoService _catalogoService = CatalogoService();
  late Future<List<Equipamento>> _futureDados;

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
      _futureDados = _catalogoService.buscarPorCategoria(widget.categoriaSlug);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Catálogo: ${widget.categoriaSlug}')),
      body: FutureBuilder<List<Equipamento>>(
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

          final equipamentos = snapshot.data ?? [];

          if (equipamentos.isEmpty) {
            return const Center(
              child: Text('Nenhum item encontrado nesta categoria.'),
            );
          }

          return ListView.builder(
            itemCount: equipamentos.length,
            itemBuilder: (context, index) {
              final equipamento = equipamentos[index];
              return ListTile(
                leading: equipamento.imagemPrincipal.isNotEmpty
                    ? Image.network(
                        equipamento.imagemPrincipal,
                        width: 48,
                        height: 48,
                        fit: BoxFit.cover,
                      )
                    : const Icon(Icons.build_outlined),
                title: Text(equipamento.nome),
                subtitle: Text(
                  'R\$ ${equipamento.valorDiaria.toStringAsFixed(2)} / dia',
                ),
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
