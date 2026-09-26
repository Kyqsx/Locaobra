import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import '../models/equipamento.dart';
import '../screens/product_view_page.dart';
import '../services/catalogo_service.dart';
import '../widgets/category_listagem.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

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
  final TextEditingController _buscaController = TextEditingController();
  late Future<List<Equipamento>> _futureDados;
  late String? _slugAtual;
  String _termoBusca = '';
  String _ordenacao = 'relevancia';
  bool _apenasDisponiveis = false;
  int get _filtrosAtivos =>
      (_ordenacao != 'relevancia' ? 1 : 0) + (_apenasDisponiveis ? 1 : 0);

  @override
  void initState() {
    super.initState();
    _slugAtual = widget.categoriaSlug;
    _carregarDados();
  }

  @override
  void dispose() {
    _buscaController.dispose();
    super.dispose();
  }

  void _carregarDados() {
    setState(() {
      _futureDados = _slugAtual == null
          ? _catalogoService.buscarTodos()
          : _catalogoService.buscarPorCategoria(_slugAtual!);
    });
  }

  void _abrirProduto(Equipamento equipamento) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ProductViewPage(
          equipamentoId: equipamento.id,
          equipamentoInicial: equipamento,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: SingleChildScrollView(
        // Status bar + toolbar como padding do scroll (não envolvendo), pra
        // o conteúdo começar abaixo do header mas passar por baixo dele ao
        // rolar. Usando MediaQuery direto em vez de SafeArea pra evitar
        // duplicação de padding com extendBodyBehindAppBar.
        padding: EdgeInsets.only(
          top: MediaQuery.of(context).padding.top + kToolbarHeight,
        ),
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(6, 8, 6, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: _buildCampoBusca()),
                        const SizedBox(width: 12),
                        _buildFiltrarButton(),
                      ],
                    ),
                    const SizedBox(height: 6),
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
                                style: TextStyle(color: AppColors.gray700),
                              ),
                            ),
                          );
                        }
                        final dados = snapshot.data ?? const <Equipamento>[];
                        final listaFiltrada = _aplicarFiltros(dados);
                        if (listaFiltrada.isEmpty) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 48),
                            child: Center(
                              child: Text(
                                _termoBusca.trim().isNotEmpty
                                    ? 'Nenhum equipamento encontrado para "${_termoBusca.trim()}".'
                                    : _filtrosAtivos > 0
                                        ? 'Nenhum equipamento disponível com os filtros aplicados.'
                                        : 'Nenhum equipamento disponível.',
                                textAlign: TextAlign.center,
                                style: TextStyle(color: AppColors.gray700),
                              ),
                            ),
                          );
                        }
                        return CategoryListagem(
                          equipamentos: listaFiltrada,
                          onTapEquipamento: _abrirProduto,
                        );
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
    );
  }

  // Campo de pesquisa — filtra os equipamentos já carregados por nome ou
  // descrição, sem nova chamada de API (igual ao catálogo do web).
  Widget _buildCampoBusca() {
    return SizedBox(
      height: 36,
      child: TextField(
      controller: _buscaController,
      onChanged: (valor) => setState(() => _termoBusca = valor),
      style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
      decoration: InputDecoration(
        hintText: 'Buscar equipamento...',
        hintStyle: TextStyle(fontSize: 14, color: AppColors.gray500),
        prefixIcon: Icon(Icons.search, size: 20, color: AppColors.gray600),
        isDense: true,
        filled: true,
        fillColor: AppColors.white,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 10,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: BorderSide(color: AppColors.gray300),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: BorderSide(color: AppColors.gray300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: const BorderSide(color: AppColors.primary),
        ),
        suffixIcon: _termoBusca.isEmpty
            ? null
            : IconButton(
                visualDensity: VisualDensity.compact,
                icon: Icon(Icons.close, size: 18, color: AppColors.gray600),
                onPressed: () {
                  _buscaController.clear();
                  setState(() => _termoBusca = '');
                },
              ),
      ),
        ),
    );
  }

  // Busca (nome/descrição) + filtros do painel, sobre os equipamentos já
  // carregados — sem nova chamada de API (igual ao catálogo do web).
  List<Equipamento> _aplicarFiltros(List<Equipamento> dados) {
    var lista = dados;
    final termo = _termoBusca.trim().toLowerCase();
    if (termo.isNotEmpty) {
      lista = lista.where((eq) {
        final nome = eq.nome.toLowerCase();
        final descricao = eq.descricao?.toLowerCase() ?? '';
        return nome.contains(termo) || descricao.contains(termo);
      }).toList();
    }
    if (_apenasDisponiveis) {
      lista = lista.where((eq) => eq.quantidadeDisponivel > 0).toList();
    }
    return _ordenar(lista);
  }

  // Devolve uma cópia ordenada da lista conforme a ordenação escolhida.
  List<Equipamento> _ordenar(List<Equipamento> lista) {
    final ordenada = [...lista];
    switch (_ordenacao) {
      case 'menor-preco':
        ordenada.sort((a, b) => a.valorDiaria.compareTo(b.valorDiaria));
      case 'maior-preco':
        ordenada.sort((a, b) => b.valorDiaria.compareTo(a.valorDiaria));
      case 'nome':
        ordenada.sort(
          (a, b) => a.nome.toLowerCase().compareTo(b.nome.toLowerCase()),
        );
      case 'avaliacao':
        ordenada.sort((a, b) {
          final porNota = b.mediaAvaliacoes.compareTo(a.mediaAvaliacoes);
          return porNota != 0
              ? porNota
              : b.totalAvaliacoes.compareTo(a.totalAvaliacoes);
        });
    }
    return ordenada;
  }

  // Painel de filtros em bottom sheet: ordenação + "somente disponíveis".
  // As escolhas ficam em variáveis temporárias e só valem ao tocar "Aplicar".
  Future<void> _abrirFiltros() async {
    String? ordenacaoTemp = _ordenacao;
    bool disponiveisTemp = _apenasDisponiveis;

    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.xl2)),
      ),
      builder: (contextSheet) {
        return StatefulBuilder(
          builder: (contextSheet, setSheetState) {
            Widget opcaoOrdenacao(String valor, String rotulo) {
              return RadioListTile<String>(
                value: valor,
                title: Text(
                  rotulo,
                  style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
                ),
                activeColor: AppColors.primary,
                dense: true,
                contentPadding: EdgeInsets.zero,
              );
            }

            return SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Filtros',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        TextButton(
                          onPressed: () {
                            setSheetState(() {
                              ordenacaoTemp = 'relevancia';
                              disponiveisTemp = false;
                            });
                          },
                          child: Text(
                            'Limpar',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: AppColors.primary,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const Text(
                      'ORDENAR POR',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: AppColors.gray500,
                      ),
                    ),
                    RadioGroup<String>(
                      groupValue: ordenacaoTemp,
                      onChanged: (novo) =>
                          setSheetState(() => ordenacaoTemp = novo),
                      child: Column(
                        children: [
                          opcaoOrdenacao('relevancia', 'Relevância'),
                          opcaoOrdenacao('menor-preco', 'Menor preço'),
                          opcaoOrdenacao('maior-preco', 'Maior preço'),
                          opcaoOrdenacao('nome', 'Nome (A-Z)'),
                          opcaoOrdenacao('avaliacao', 'Melhor avaliados'),
                        ],
                      ),
                    ),
                    const Divider(),
                    SwitchListTile(
                      value: disponiveisTemp,
                      onChanged: (v) => setSheetState(() => disponiveisTemp = v),
                      title: const Text(
                        'Somente equipamentos disponíveis',
                        style: TextStyle(fontSize: 14, color: AppColors.textPrimary),
                      ),
                      activeThumbColor: AppColors.primary,
                      contentPadding: EdgeInsets.zero,
                    ),
                    const SizedBox(height: 8),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: AppColors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(AppRadius.lg),
                          ),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        onPressed: () {
                          setState(() {
                            _ordenacao = ordenacaoTemp ?? 'relevancia';
                            _apenasDisponiveis = disponiveisTemp;
                          });
                          Navigator.pop(contextSheet);
                        },
                        child: const Text(
                          'Aplicar',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildFiltrarButton() {
    final comFiltro = _filtrosAtivos > 0;
    return OutlinedButton.icon(
      onPressed: _abrirFiltros,
      icon: const Icon(Icons.filter_list, size: 18),
      label: Text(
        comFiltro ? 'Filtrar ($_filtrosAtivos)' : 'Filtrar',
        style: const TextStyle(fontWeight: FontWeight.w600),
      ),
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(0, 36),
        backgroundColor: comFiltro ? AppColors.primary : AppColors.white,
        foregroundColor: comFiltro ? AppColors.white : AppColors.textPrimary,
        side: BorderSide(
          color: comFiltro ? AppColors.primary : AppColors.gray300,
        ),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.lg)),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 0),
      ),
    );
  }
}