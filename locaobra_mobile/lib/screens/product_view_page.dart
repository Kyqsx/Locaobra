import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/auth/login_page.dart';
import 'package:locaobra_mobile/cart/cart_item.dart';
import 'package:locaobra_mobile/cart/cart_navigation.dart';
import 'package:locaobra_mobile/cart/cart_state.dart';
import 'package:locaobra_mobile/models/categoria.dart';
import 'package:locaobra_mobile/models/equipamento.dart';
import 'package:locaobra_mobile/services/catalogo_service.dart';
import 'package:locaobra_mobile/utils/formatters.dart';
import 'package:locaobra_mobile/widgets/avaliacoes_section.dart';
import 'package:locaobra_mobile/widgets/estrelas.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Resultado de tentar colocar o equipamento no carrinho.
enum _ResultadoCarrinho { interrompido, adicionado, limiteAtingido }

/// Tela de detalhes do equipamento — equivalente ao
/// `ProductView/productview.jsx` do locaobra_web, com dados reais da API
/// (GET /api/equipamentos/{id}).
///
/// [equipamentoInicial] (opcional) é o item que já veio da listagem: aparece
/// na hora enquanto a versão completa é buscada na API.
class ProductViewPage extends StatefulWidget {
  final int equipamentoId;
  final Equipamento? equipamentoInicial;

  const ProductViewPage({
    super.key,
    required this.equipamentoId,
    this.equipamentoInicial,
  });

  @override
  State<ProductViewPage> createState() => _ProductViewPageState();
}

class _ProductViewPageState extends State<ProductViewPage> {
  final CatalogoService _catalogoService = CatalogoService();
  final ScrollController _scrollController = ScrollController();
  final PageController _galeriaController = PageController();
  final GlobalKey _avaliacoesKey = GlobalKey();

  Equipamento? _equipamento;
  bool _carregando = true;
  String? _erro;

  int _fotoAtiva = 0;
  int _diasLocacao = 1; // 1 = diária, 7 = semanal, 30 = mensal
  int _quantidade = 1;
  bool _ocupado = false; // evita toques duplos enquanto o login/adição roda

  @override
  void initState() {
    super.initState();
    _equipamento = widget.equipamentoInicial;
    _carregando = _equipamento == null;
    _carregar();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _galeriaController.dispose();
    super.dispose();
  }

  Future<void> _carregar() async {
    try {
      final equipamento = await _catalogoService.buscarPorId(
        widget.equipamentoId,
      );
      if (!mounted) return;
      setState(() {
        _equipamento = equipamento;
        _erro = null;
        _carregando = false;
        _quantidade = _limitarQuantidade(_quantidade, equipamento);
      });
    } on EquipamentoNaoEncontradoException {
      if (!mounted) return;
      setState(() {
        _equipamento = null;
        _erro = 'Equipamento não encontrado.';
        _carregando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _carregando = false;
        // Se já há o item da listagem na tela, mantém ele e não mostra erro.
        if (_equipamento == null) {
          _erro = 'Não foi possível carregar o equipamento.';
        }
      });
    }
  }

  int _limitarQuantidade(int quantidade, Equipamento equipamento) {
    final maximo = equipamento.quantidadeDisponivel < 1
        ? 1
        : equipamento.quantidadeDisponivel;
    return quantidade.clamp(1, maximo).toInt();
  }

  // ---------------------------------------------------------------------------
  // Ações
  // ---------------------------------------------------------------------------

  /// Pedido é uma ação de cliente logado (igual ao garantirPodeComprar do web).
  Future<bool> _garantirPodeComprar() async {
    if (!AuthState.estaLogado) {
      final logou = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => const LoginPage(voltarAoConcluir: true),
        ),
      );
      if (logou != true || !mounted) return false;
    }

    if (AuthState.tipoUsuario == 'FUNCIONARIO') {
      _mostrarAviso(
        'Apenas clientes podem solicitar um pedido de aluguel pelo catálogo.',
      );
      return false;
    }
    return true;
  }

  Future<_ResultadoCarrinho> _colocarNoCarrinho(Equipamento equipamento) async {
    if (!await _garantirPodeComprar() || !mounted) {
      return _ResultadoCarrinho.interrompido;
    }
    final mudou = CartState.adicionarItem(equipamento, quantidade: _quantidade);
    return mudou
        ? _ResultadoCarrinho.adicionado
        : _ResultadoCarrinho.limiteAtingido;
  }

  Future<void> _adicionarAoCarrinho() async {
    final equipamento = _equipamento;
    if (equipamento == null || _ocupado) return;

    setState(() => _ocupado = true);
    final resultado = await _colocarNoCarrinho(equipamento);
    if (!mounted) return;
    setState(() => _ocupado = false);

    switch (resultado) {
      case _ResultadoCarrinho.adicionado:
        _mostrarAviso('Adicionado ao carrinho.', comAcaoCarrinho: true);
      case _ResultadoCarrinho.limiteAtingido:
        _mostrarAviso(
          'Você já tem todas as unidades disponíveis deste equipamento no carrinho.',
          comAcaoCarrinho: true,
        );
      case _ResultadoCarrinho.interrompido:
        break;
    }
  }

  Future<void> _comprarAgora() async {
    final equipamento = _equipamento;
    if (equipamento == null || _ocupado) return;

    setState(() => _ocupado = true);
    final resultado = await _colocarNoCarrinho(equipamento);
    if (!mounted) return;
    setState(() => _ocupado = false);

    if (resultado != _ResultadoCarrinho.interrompido) {
      abrirCarrinho(context);
    }
  }

  void _mostrarAviso(String mensagem, {bool comAcaoCarrinho = false}) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(mensagem),
          action: comAcaoCarrinho
              ? SnackBarAction(
                  label: 'VER CARRINHO',
                  onPressed: () => abrirCarrinho(context),
                )
              : null,
        ),
      );
  }

  void _irParaAvaliacoes() {
    final contexto = _avaliacoesKey.currentContext;
    if (contexto == null) return;
    Scrollable.ensureVisible(
      contexto,
      duration: const Duration(milliseconds: 350),
      curve: Curves.easeInOut,
    );
  }

  void _abrirTelaCheia(List<ImagemEquipamento> fotos, int inicial) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        fullscreenDialog: true,
        builder: (_) => _GaleriaTelaCheia(fotos: fotos, inicial: inicial),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    final equipamento = _equipamento;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: HeaderVoltar(
        actions: [
          ValueListenableBuilder<List<CartItem>>(
            valueListenable: CartState.itens,
            builder: (context, itens, _) {
              final total = CartState.totalItens;
              return IconButton(
                tooltip: 'Carrinho',
                onPressed: () => abrirCarrinho(context),
                icon: Badge(
                  isLabelVisible: total > 0,
                  label: Text('$total'),
                  child: const Icon(Icons.shopping_cart_outlined),
                ),
              );
            },
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: _buildCorpo(equipamento),
      bottomNavigationBar: equipamento == null
          ? null
          : _buildBarraCompra(equipamento),
    );
  }

  Widget _buildCorpo(Equipamento? equipamento) {
    if (_carregando) {
      return const Center(child: CircularProgressIndicator());
    }

    if (equipamento == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _erro ?? 'Equipamento não encontrado.',
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.gray700),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () {
                  setState(() {
                    _carregando = true;
                    _erro = null;
                  });
                  _carregar();
                },
                child: const Text('Tentar novamente'),
              ),
            ],
          ),
        ),
      );
    }

    return SingleChildScrollView(
      controller: _scrollController,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildGaleria(equipamento),
          const SizedBox(height: 16),
          _buildCabecalho(equipamento),
          const SizedBox(height: 16),
          _buildEspecificacoes(equipamento),
          const SizedBox(height: 16),
          _buildOpcoes(equipamento),
          const SizedBox(height: 16),
          _buildInfoAluguel(),
          const SizedBox(height: 16),
          AvaliacoesSection(
            key: _avaliacoesKey,
            equipamentoId: equipamento.id,
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }

  // --- Galeria ---------------------------------------------------------------

  Widget _buildGaleria(Equipamento equipamento) {
    final fotos = equipamento.fotos;

    return Column(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(AppRadius.xl),
          child: AspectRatio(
            aspectRatio: 1,
            child: fotos.isEmpty
                ? const _FotoPlaceholder()
                : PageView.builder(
                    controller: _galeriaController,
                    itemCount: fotos.length,
                    onPageChanged: (indice) =>
                        setState(() => _fotoAtiva = indice),
                    itemBuilder: (context, indice) => GestureDetector(
                      onTap: () => _abrirTelaCheia(fotos, indice),
                      child: _FotoEquipamento(foto: fotos[indice]),
                    ),
                  ),
          ),
        ),
        if (fotos.length > 1) ...[
          const SizedBox(height: 12),
          SizedBox(
            height: 64,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: fotos.length,
              separatorBuilder: (context, indice) => const SizedBox(width: 8),
              itemBuilder: (context, indice) {
                final ativa = indice == _fotoAtiva;
                return GestureDetector(
                  onTap: () => _galeriaController.animateToPage(
                    indice,
                    duration: const Duration(milliseconds: 250),
                    curve: Curves.easeOut,
                  ),
                  child: Container(
                    width: 64,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                      border: Border.all(
                        color: ativa ? AppColors.primary : AppColors.gray300,
                        width: ativa ? 2 : 1,
                      ),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(AppRadius.md),
                      child: _FotoEquipamento(foto: fotos[indice]),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ],
    );
  }

  // --- Cabeçalho (nome, avaliação, descrição, dados) ---------------------------

  Widget _buildCabecalho(Equipamento equipamento) {
    final temAvaliacoes = equipamento.totalAvaliacoes > 0;
    final rotuloAvaliacoes =
        equipamento.totalAvaliacoes == 1 ? 'avaliação' : 'avaliações';
    final disponivel = equipamento.quantidadeDisponivel;
    final descricao = equipamento.descricao?.trim() ?? '';

    return _Cartao(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            equipamento.nome,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          InkWell(
            onTap: _irParaAvaliacoes,
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: temAvaliacoes
                  ? Row(
                      children: [
                        Estrelas(valor: equipamento.mediaAvaliacoes, tamanho: 16),
                        const SizedBox(width: 8),
                        Flexible(
                          child: Text(
                            '${formatarMedia(equipamento.mediaAvaliacoes)} · '
                            '${equipamento.totalAvaliacoes} $rotuloAvaliacoes',
                            style: TextStyle(
                              fontSize: 12,
                              color: AppColors.gray700,
                              decoration: TextDecoration.underline,
                            ),
                          ),
                        ),
                      ],
                    )
                  : Text(
                      'Sem avaliações ainda',
                      style: TextStyle(
                        fontSize: 12,
                        color: AppColors.gray700,
                        decoration: TextDecoration.underline,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            descricao.isEmpty ? 'Sem descrição disponível.' : descricao,
            style: TextStyle(
              fontSize: 14,
              height: 1.4,
              color: AppColors.gray800,
            ),
          ),
          const Divider(height: 28),
          _LinhaInfo(
            rotulo: 'Categoria',
            valor: _nomeCategoria(equipamento.categoria),
          ),
          _LinhaInfo(
            rotulo: 'Disponíveis',
            valor: disponivel == 1 ? '1 unidade' : '$disponivel unidades',
          ),
          _LinhaInfo(
            rotulo: 'Valor da diária',
            valor: formatarMoeda(equipamento.valorDiaria),
            destaque: true,
          ),
        ],
      ),
    );
  }

  /// O campo `categoria` da API é um texto livre (normalmente o slug). Se
  /// bater com uma categoria conhecida mostra o nome bonito; senão o texto puro.
  String _nomeCategoria(String categoria) {
    if (categoria.isEmpty) return '—';
    for (final conhecida in kCategorias) {
      if (conhecida.slug.toLowerCase() == categoria.toLowerCase()) {
        return conhecida.nome;
      }
    }
    return categoria;
  }

  // --- Especificações ----------------------------------------------------------

  Widget _buildEspecificacoes(Equipamento equipamento) {
    final List<MapEntry<String, String>> especificacoes =
        equipamento.especificacoes.isNotEmpty
            ? equipamento.especificacoes.entries.toList()
            : [
                MapEntry('Categoria', _nomeCategoria(equipamento.categoria)),
                MapEntry(
                  'Disponibilidade',
                  '${equipamento.quantidadeDisponivel} de ${equipamento.quantidadeTotal} unidades',
                ),
                MapEntry('Status', equipamento.status ?? '—'),
                MapEntry(
                  'Criado em',
                  equipamento.criadoEm != null
                      ? formatarData(equipamento.criadoEm!)
                      : '—',
                ),
              ];

    return _Cartao(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _TituloSecao('Especificações Técnicas'),
          for (var i = 0; i < especificacoes.length; i++) ...[
            if (i > 0) const Divider(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 4,
                  child: Text(
                    especificacoes[i].key,
                    style: TextStyle(fontSize: 13, color: AppColors.gray600),
                  ),
                ),
                Expanded(
                  flex: 5,
                  child: Text(
                    especificacoes[i].value,
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  // --- Tipo de locação + quantidade ------------------------------------------

  Widget _buildOpcoes(Equipamento equipamento) {
    final disponivel = equipamento.quantidadeDisponivel;
    final rotuloDisponivel =
        disponivel == 1 ? '1 disponível' : '$disponivel disponíveis';

    return _Cartao(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _TituloSecao('Tipo de locação'),
          SizedBox(
            width: double.infinity,
            child: SegmentedButton<int>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: 1, label: Text('Diária')),
                ButtonSegment(value: 7, label: Text('Semanal')),
                ButtonSegment(value: 30, label: Text('Mensal')),
              ],
              selected: {_diasLocacao},
              onSelectionChanged: (selecao) =>
                  setState(() => _diasLocacao = selecao.first),
              style: SegmentedButton.styleFrom(
                selectedBackgroundColor: AppColors.primaryTint,
                selectedForegroundColor: AppColors.primaryDark,
              ),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _diasLocacao == 1 ? '1 dia' : '$_diasLocacao dias',
            style: TextStyle(fontSize: 12, color: AppColors.gray600),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: Text(
                  'Quantidade ($rotuloDisponivel)',
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              _SeletorQuantidade(
                valor: _quantidade,
                maximo: disponivel < 1 ? 1 : disponivel,
                habilitado: disponivel > 0,
                onChanged: (novo) => setState(() => _quantidade = novo),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // --- Informações de aluguel (mesmos textos do web) ---------------------------

  Widget _buildInfoAluguel() {
    return const _Cartao(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _TituloSecao('Informações de Aluguel'),
          _LinhaBeneficio(
            icone: Icons.inventory_2_outlined,
            texto: 'Entrega e retirada gratuitas em São Paulo',
          ),
          _LinhaBeneficio(
            icone: Icons.shield_outlined,
            texto: 'Produto com seguro incluído',
          ),
          _LinhaBeneficio(
            icone: Icons.settings_outlined,
            texto: 'Suporte técnico 24/7',
          ),
          _LinhaBeneficio(
            icone: Icons.credit_card,
            texto: 'Pagamento seguro com parcelamento',
          ),
        ],
      ),
    );
  }

  // --- Barra fixa de compra ----------------------------------------------------

  Widget _buildBarraCompra(Equipamento equipamento) {
    final indisponivel = equipamento.quantidadeDisponivel < 1;
    final estimado = equipamento.valorDiaria * _diasLocacao * _quantidade;

    return Material(
      color: AppColors.white,
      elevation: 8,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Valor estimado',
                    style: TextStyle(fontSize: 13, color: AppColors.gray700),
                  ),
                  Text(
                    formatarMoeda(estimado),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: indisponivel || _ocupado
                          ? null
                          : _adicionarAoCarrinho,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.primary,
                        side: const BorderSide(color: AppColors.primary),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 14,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppRadius.lg),
                        ),
                      ),
                      child: const Text(
                        'Adicionar ao carrinho',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: ElevatedButton(
                      onPressed:
                          indisponivel || _ocupado ? null : _comprarAgora,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: AppColors.white,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 14,
                        ),
                        elevation: 0,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppRadius.lg),
                        ),
                      ),
                      child: Text(
                        indisponivel ? 'Indisponível no momento' : 'Alugar agora',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// Widgets de apoio (privados a esta tela)
// =============================================================================

class _Cartao extends StatelessWidget {
  final Widget child;

  const _Cartao({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: child,
    );
  }
}

class _TituloSecao extends StatelessWidget {
  final String texto;

  const _TituloSecao(this.texto);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Text(
        texto,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.bold,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}

class _LinhaInfo extends StatelessWidget {
  final String rotulo;
  final String valor;
  final bool destaque;

  const _LinhaInfo({
    required this.rotulo,
    required this.valor,
    this.destaque = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(
            child: Text(
              rotulo,
              style: TextStyle(fontSize: 13, color: AppColors.gray600),
            ),
          ),
          Text(
            valor,
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.bold,
              color: destaque ? AppColors.primary : AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}

class _LinhaBeneficio extends StatelessWidget {
  final IconData icone;
  final String texto;

  const _LinhaBeneficio({required this.icone, required this.texto});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Icon(icone, size: 18, color: AppColors.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              texto,
              style: TextStyle(fontSize: 13, color: AppColors.gray800),
            ),
          ),
        ],
      ),
    );
  }
}

/// Seletor − / número / + com limites de 1 até [maximo].
class _SeletorQuantidade extends StatelessWidget {
  final int valor;
  final int maximo;
  final bool habilitado;
  final ValueChanged<int> onChanged;

  const _SeletorQuantidade({
    required this.valor,
    required this.maximo,
    required this.habilitado,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: AppColors.gray300),
        borderRadius: BorderRadius.circular(AppRadius.lg),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            onPressed: habilitado && valor > 1 ? () => onChanged(valor - 1) : null,
            icon: const Icon(Icons.remove, size: 18),
            visualDensity: VisualDensity.compact,
            tooltip: 'Diminuir quantidade',
          ),
          SizedBox(
            width: 32,
            child: Text(
              '$valor',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
          ),
          IconButton(
            onPressed:
                habilitado && valor < maximo ? () => onChanged(valor + 1) : null,
            icon: const Icon(Icons.add, size: 18),
            visualDensity: VisualDensity.compact,
            tooltip: 'Aumentar quantidade',
          ),
        ],
      ),
    );
  }
}

/// Foto do equipamento com o ponto focal salvo no Admin (mesmo
/// `object-position` do web): a foto inteira vai para o app e o enquadramento
/// quadrado é centrado no ponto escolhido.
class _FotoEquipamento extends StatelessWidget {
  final ImagemEquipamento foto;
  final BoxFit fit;

  const _FotoEquipamento({required this.foto, this.fit = BoxFit.cover});

  @override
  Widget build(BuildContext context) {
    return Image.network(
      foto.urlCompleta,
      fit: fit,
      width: double.infinity,
      height: double.infinity,
      alignment: Alignment(foto.focoX / 50 - 1, foto.focoY / 50 - 1),
      errorBuilder: (context, error, stackTrace) => const _FotoPlaceholder(),
      loadingBuilder: (context, child, progresso) {
        if (progresso == null) return child;
        return Container(
          color: AppColors.gray100,
          alignment: Alignment.center,
          child: const SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        );
      },
    );
  }
}

class _FotoPlaceholder extends StatelessWidget {
  const _FotoPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.gray100,
      alignment: Alignment.center,
      child: const Text('📐', style: TextStyle(fontSize: 40)),
    );
  }
}

/// Visualizador em tela cheia (equivalente ao Lightbox do web): passa as fotos
/// com o dedo e dá zoom com o gesto de pinça.
class _GaleriaTelaCheia extends StatefulWidget {
  final List<ImagemEquipamento> fotos;
  final int inicial;

  const _GaleriaTelaCheia({required this.fotos, required this.inicial});

  @override
  State<_GaleriaTelaCheia> createState() => _GaleriaTelaCheiaState();
}

class _GaleriaTelaCheiaState extends State<_GaleriaTelaCheia> {
  late final PageController _controller;
  late int _atual;

  @override
  void initState() {
    super.initState();
    _atual = widget.inicial;
    _controller = PageController(initialPage: widget.inicial);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: AppColors.white,
        elevation: 0,
        title: Text('${_atual + 1} / ${widget.fotos.length}'),
      ),
      body: PageView.builder(
        controller: _controller,
        itemCount: widget.fotos.length,
        onPageChanged: (indice) => setState(() => _atual = indice),
        itemBuilder: (context, indice) => InteractiveViewer(
          minScale: 1,
          maxScale: 4,
          child: Center(
            child: _FotoEquipamento(
              foto: widget.fotos[indice],
              fit: BoxFit.contain,
            ),
          ),
        ),
      ),
    );
  }
}
