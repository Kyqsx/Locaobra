import 'package:flutter/material.dart';
import '../models/categoria.dart';
import '../models/equipamento.dart';
import '../services/catalogo_service.dart';
import '../widgets/category_nav_tabs.dart';
import '../widgets/category_listagem.dart';

/// Página única e dinâmica de catálogo/categoria — busca os equipamentos
/// na API (via [CatalogoService]) e filtra por [categoriaSlug], igual ao
/// locaobra_web faz em `Catalogo/catalogo.jsx` com a rota `/catalogo/:slug`.
///
/// Substitui as antigas telas hardcoded por categoria (Ferramentas_
/// Eletricas.dart, Concretagem.dart, Acesso_e_Elevacao.dart, etc): agora
/// é uma página só, e trocar de categoria é só trocar o slug, sem navegar
/// pra uma classe Dart diferente pra cada uma.
///
/// Use `categoriaSlug: null` para mostrar o catálogo completo.
class CatalogoPagina extends StatefulWidget {
  final String? categoriaSlug;

  const CatalogoPagina({super.key, this.categoriaSlug});

  @override
  State<CatalogoPagina> createState() => _CatalogoPaginaState();
}

class _CatalogoPaginaState extends State<CatalogoPagina> {
  final CatalogoService _catalogoService = CatalogoService();
  late Future<List<Equipamento>> _futureDados;
  late String? _slugAtual;

  @override
  void initState() {
    super.initState();
    _slugAtual = widget.categoriaSlug;
    _carregarDados();
  }

  void _carregarDados() {
    setState(() {
      _futureDados = _slugAtual == null
          ? _catalogoService.buscarTodos()
          : _catalogoService.buscarPorCategoria(_slugAtual!);
    });
  }

  void _selecionarSlug(String slug) {
    if (slug == _slugAtual) return;
    setState(() => _slugAtual = slug);
    _carregarDados();
  }

  String get _nomeFormatado {
    final slug = _slugAtual;
    if (slug == null) return 'Catálogo Completo';
    final encontrada = kCategorias.where((c) => c.slug == slug);
    if (encontrada.isNotEmpty) return encontrada.first.nome;
    return slug.replaceAll('-', ' ');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: Text(_nomeFormatado),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CategoryNavTabs(
                selectedSlug: _slugAtual,
                onSelect: _selecionarSlug,
              ),
              const Divider(height: 1, color: Colors.grey),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
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
                    FutureBuilder<List<Equipamento>>(
                      future: _futureDados,
                      builder: (context, snapshot) {
                        if (snapshot.connectionState == ConnectionState.waiting) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 48),
                            child: Center(child: CircularProgressIndicator()),
                          );
                        }
                        if (snapshot.hasError) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 48),
                            child: Center(
                              child: Text(
                                'Não foi possível carregar os equipamentos.',
                                style: TextStyle(color: Colors.grey.shade700),
                              ),
                            ),
                          );
                        }
                        return CategoryListagem(equipamentos: snapshot.data ?? const []);
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

  // "Início" volta pra primeira tela da pilha de navegação (Home/Welcome).
  Widget _buildBreadcrumb(BuildContext context) {
    return Row(
      children: [
        InkWell(
          onTap: () => Navigator.of(context).popUntil((route) => route.isFirst),
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
          _nomeFormatado,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }

  Widget _buildFiltrarButton() {
    return OutlinedButton.icon(
      onPressed: () {
        // TODO: abrir tela/bottom sheet de filtros
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
}