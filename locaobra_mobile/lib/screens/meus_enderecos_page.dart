import 'package:flutter/material.dart';
import '../widgets/header_voltar.dart';
import 'package:locaobra_mobile/models/endereco.dart';
import 'package:locaobra_mobile/services/endereco_service.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';
import 'package:locaobra_mobile/widgets/cep_field.dart';

/// "Meus Endereços" — equivalente a `Perfil/meusEnderecos.jsx`: lista, cria,
/// edita, remove e define o endereço principal do cliente logado.
class MeusEnderecosPage extends StatefulWidget {
  const MeusEnderecosPage({super.key});

  @override
  State<MeusEnderecosPage> createState() => _MeusEnderecosPageState();
}

class _MeusEnderecosPageState extends State<MeusEnderecosPage> {
  final EnderecoService _service = EnderecoService();

  bool _carregando = true;
  String? _erro;
  List<Endereco> _enderecos = const [];

  bool _formAberto = false;
  int? _editandoId;
  bool _salvando = false;

  final _apelidoCtrl = TextEditingController();
  final _cepCtrl = TextEditingController();
  final _ruaCtrl = TextEditingController();
  final _numeroCtrl = TextEditingController();
  final _complementoCtrl = TextEditingController();
  final _bairroCtrl = TextEditingController();
  final _cidadeCtrl = TextEditingController();
  final _estadoCtrl = TextEditingController();
  bool _principal = false;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void dispose() {
    for (final ctrl in [
      _apelidoCtrl,
      _cepCtrl,
      _ruaCtrl,
      _numeroCtrl,
      _complementoCtrl,
      _bairroCtrl,
      _cidadeCtrl,
      _estadoCtrl,
    ]) {
      ctrl.dispose();
    }
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Carregamento
  // ---------------------------------------------------------------------------

  Future<void> _carregar() async {
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final enderecos = await _service.listarMeus();
      if (!mounted) return;
      setState(() {
        _enderecos = enderecos;
        _carregando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _erro = 'Não foi possível carregar seus endereços.';
        _carregando = false;
      });
    }
  }

  // ---------------------------------------------------------------------------
  // Formulário
  // ---------------------------------------------------------------------------

  void _limparForm() {
    _apelidoCtrl.clear();
    _cepCtrl.clear();
    _ruaCtrl.clear();
    _numeroCtrl.clear();
    _complementoCtrl.clear();
    _bairroCtrl.clear();
    _cidadeCtrl.clear();
    _estadoCtrl.clear();
    _principal = false;
  }

  void _abrirNovo() {
    setState(() {
      _editandoId = null;
      _limparForm();
      _principal = _enderecos.isEmpty;
      _formAberto = true;
    });
  }

  void _abrirEdicao(Endereco endereco) {
    setState(() {
      _editandoId = endereco.id;
      _apelidoCtrl.text = endereco.apelido;
      _cepCtrl.text = endereco.cep;
      _ruaCtrl.text = endereco.rua;
      _numeroCtrl.text = endereco.numero;
      _complementoCtrl.text = endereco.complemento;
      _bairroCtrl.text = endereco.bairro;
      _cidadeCtrl.text = endereco.cidade;
      _estadoCtrl.text = endereco.estado;
      _principal = endereco.principal;
      _formAberto = true;
    });
  }

  void _fecharForm() {
    setState(() {
      _formAberto = false;
      _editandoId = null;
      _limparForm();
    });
  }

  Endereco get _enderecoDoForm => Endereco(
        apelido: _apelidoCtrl.text,
        cep: _cepCtrl.text,
        rua: _ruaCtrl.text,
        numero: _numeroCtrl.text,
        complemento: _complementoCtrl.text,
        bairro: _bairroCtrl.text,
        cidade: _cidadeCtrl.text,
        estado: _estadoCtrl.text,
        principal: _principal,
      );

  Future<void> _salvar() async {
    if (_ruaCtrl.text.trim().isEmpty || _cidadeCtrl.text.trim().isEmpty || _estadoCtrl.text.trim().isEmpty) {
      _mostrarMensagem('Preencha ao menos rua, cidade e UF do endereço.', erro: true);
      return;
    }

    setState(() => _salvando = true);
    try {
      if (_editandoId != null) {
        await _service.atualizar(_editandoId!, _enderecoDoForm);
      } else {
        await _service.adicionar(_enderecoDoForm);
      }
      if (!mounted) return;
      _fecharForm();
      await _carregar();
      _mostrarMensagem('Endereço salvo com sucesso!');
    } catch (e) {
      if (!mounted) return;
      _mostrarMensagem(e.toString().replaceFirst('Exception: ', ''), erro: true);
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  Future<void> _remover(Endereco endereco) async {
    final confirmou = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remover endereço'),
        content: Text('Remover o endereço "${endereco.apelido.isEmpty ? endereco.resumo : endereco.apelido}"?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Voltar')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Remover'),
          ),
        ],
      ),
    );
    if (confirmou != true || endereco.id == null) return;

    try {
      await _service.remover(endereco.id!);
      await _carregar();
    } catch (e) {
      if (!mounted) return;
      _mostrarMensagem(e.toString().replaceFirst('Exception: ', ''), erro: true);
    }
  }

  Future<void> _definirPrincipal(Endereco endereco) async {
    if (endereco.id == null) return;
    try {
      await _service.definirPrincipal(endereco.id!);
      await _carregar();
    } catch (e) {
      if (!mounted) return;
      _mostrarMensagem(e.toString().replaceFirst('Exception: ', ''), erro: true);
    }
  }

  void _mostrarMensagem(String texto, {bool erro = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(texto),
        backgroundColor: erro ? AppColors.error : AppColors.success,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_carregando) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_erro != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(_erro!, textAlign: TextAlign.center, style: TextStyle(color: AppColors.error)),
        ),
      );
    }

    return ListView(
      // Status bar + toolbar no padding do scroll (sem SafeArea), pra
      // conteúdo rolar por baixo do header transparente. Lateral 6px
      // igual ao catálogo/product view/carrinho.
      padding: EdgeInsets.fromLTRB(
        6,
        MediaQuery.of(context).padding.top + kToolbarHeight + 8,
        6,
        8,
      ),
      children: [
        const Text(
          'Endereços salvos pra agilizar o checkout — pode ter mais de um, ex: casa e obra.',
          style: TextStyle(color: AppColors.textSecondary, fontSize: 13),
        ),
        const SizedBox(height: 16),
        if (_enderecos.isEmpty && !_formAberto)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 24),
            child: Center(
              child: Text('Você ainda não tem nenhum endereço salvo.', style: TextStyle(color: AppColors.gray600)),
            ),
          ),
        for (final endereco in _enderecos)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _buildEnderecoCard(endereco),
          ),
        const SizedBox(height: 8),
        if (!_formAberto)
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _abrirNovo,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                foregroundColor: AppColors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
              child: const Text('+ Adicionar endereço'),
            ),
          ),
        if (_formAberto) _buildForm(),
      ],
    );
  }

  Widget _buildEnderecoCard(Endereco endereco) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                endereco.apelido.isEmpty ? 'Endereço' : endereco.apelido,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
              ),
              if (endereco.principal) ...[
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppColors.primaryTint,
                    borderRadius: BorderRadius.circular(AppRadius.full),
                    border: Border.all(color: AppColors.primaryTint),
                  ),
                  child: Text('Principal', style: TextStyle(color: AppColors.primaryDark, fontSize: 11)),
                ),
              ],
            ],
          ),
          const SizedBox(height: 6),
          Text(endereco.resumo, style: TextStyle(color: AppColors.gray700, fontSize: 13)),
          const SizedBox(height: 10),
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: [
              if (!endereco.principal)
                _linkBotao('Definir como principal', () => _definirPrincipal(endereco)),
              _linkBotao('Editar', () => _abrirEdicao(endereco)),
              _linkBotao('Remover', () => _remover(endereco), cor: AppColors.error),
            ],
          ),
        ],
      ),
    );
  }

  Widget _linkBotao(String texto, VoidCallback onTap, {Color? cor}) {
    return InkWell(
      onTap: onTap,
      child: Text(
        texto,
        style: TextStyle(color: cor ?? AppColors.primaryDark, fontWeight: FontWeight.w600, fontSize: 13),
      ),
    );
  }

  Widget _buildForm() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.gray300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _editandoId != null ? 'Editar endereço' : 'Novo endereço',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
          ),
          const SizedBox(height: 12),
          _campo('Apelido (ex: Casa, Obra Centro)', _apelidoCtrl),
          CepField(
            cepController: _cepCtrl,
            ruaController: _ruaCtrl,
            bairroController: _bairroCtrl,
            cidadeController: _cidadeCtrl,
            estadoController: _estadoCtrl,
          ),
          _campo('Rua *', _ruaCtrl),
          Row(
            children: [
              Expanded(child: _campo('Número', _numeroCtrl)),
              const SizedBox(width: 8),
              Expanded(child: _campo('Complemento', _complementoCtrl)),
            ],
          ),
          _campo('Bairro', _bairroCtrl),
          Row(
            children: [
              Expanded(flex: 3, child: _campo('Cidade *', _cidadeCtrl)),
              const SizedBox(width: 8),
              Expanded(flex: 1, child: _campo('UF *', _estadoCtrl, maxLength: 2, maiusculo: true)),
            ],
          ),
          CheckboxListTile(
            value: _principal,
            onChanged: (v) => setState(() => _principal = v ?? false),
            title: const Text('Definir como endereço principal', style: TextStyle(fontSize: 13)),
            controlAffinity: ListTileControlAffinity.leading,
            contentPadding: EdgeInsets.zero,
            dense: true,
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _salvando ? null : _fecharForm,
                  child: const Text('Cancelar'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton(
                  onPressed: _salvando ? null : _salvar,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                    foregroundColor: AppColors.white,
                  ),
                  child: Text(_salvando ? 'Salvando...' : 'Salvar endereço'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _campo(
    String rotulo,
    TextEditingController controller, {
    String? hint,
    int? maxLength,
    bool maiusculo = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextField(
        controller: controller,
        maxLength: maxLength,
        textCapitalization: maiusculo ? TextCapitalization.characters : TextCapitalization.words,
        decoration: InputDecoration(
          labelText: rotulo,
          hintText: hint,
          border: const OutlineInputBorder(),
          isDense: true,
          counterText: '',
          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        ),
      ),
    );
  }
}
