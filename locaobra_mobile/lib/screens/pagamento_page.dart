import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:locaobra_mobile/cart/cart_state.dart';
import 'package:locaobra_mobile/cart/dados_checkout.dart';
import 'package:locaobra_mobile/services/pedido_service.dart';
import 'package:locaobra_mobile/utils/formatters.dart';

/// Pagamento simulado — equivalente a `Cart/pagamento.jsx` no web. Não
/// valida nem envia dado de cartão pra lugar nenhum: só espera ~1.4s pra
/// parecer um processamento real e, na sequência, chama
/// `PedidoService.criar(..., formaPagamento: ...)`, que é quem de fato
/// grava o pedido (já como PAGO).
///
/// Sucesso: limpa o carrinho e devolve o `PedidoCriado` pro carrinho via
/// `Navigator.pop` — é o carrinho quem mostra a tela de "Pedido enviado!".
class PagamentoPage extends StatefulWidget {
  final DadosCheckout dados;
  const PagamentoPage({super.key, required this.dados});

  @override
  State<PagamentoPage> createState() => _PagamentoPageState();
}

class _PagamentoPageState extends State<PagamentoPage> {
  final PedidoService _pedidoService = PedidoService();

  final TextEditingController _numeroCtrl = TextEditingController();
  final TextEditingController _nomeCtrl = TextEditingController();
  final TextEditingController _validadeCtrl = TextEditingController();
  final TextEditingController _cvvCtrl = TextEditingController();

  FormaPagamento _forma = FormaPagamento.cartaoCredito;
  bool _pagando = false;
  String? _erro;

  @override
  void dispose() {
    _numeroCtrl.dispose();
    _nomeCtrl.dispose();
    _validadeCtrl.dispose();
    _cvvCtrl.dispose();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Máscaras (mesmo padrão do mascararNumeroCartao/mascararValidade do web)
  // ---------------------------------------------------------------------------

  static String _soDigitos(String valor) => valor.replaceAll(RegExp(r'\D'), '');

  static String _mascararNumeroCartao(String valor) {
    final digitos = _soDigitos(valor).substring(0, _soDigitos(valor).length.clamp(0, 16));
    final grupos = <String>[];
    for (var i = 0; i < digitos.length; i += 4) {
      grupos.add(digitos.substring(i, (i + 4).clamp(0, digitos.length)));
    }
    return grupos.join(' ');
  }

  static String _mascararValidade(String valor) {
    final digitos = _soDigitos(valor).substring(0, _soDigitos(valor).length.clamp(0, 4));
    return digitos.length > 2 ? '${digitos.substring(0, 2)}/${digitos.substring(2)}' : digitos;
  }

  void _onNumeroChanged(String v) {
    final formatado = _mascararNumeroCartao(v);
    _numeroCtrl.value = TextEditingValue(
      text: formatado,
      selection: TextSelection.collapsed(offset: formatado.length),
    );
  }

  void _onValidadeChanged(String v) {
    final formatado = _mascararValidade(v);
    _validadeCtrl.value = TextEditingValue(
      text: formatado,
      selection: TextSelection.collapsed(offset: formatado.length),
    );
  }

  // ---------------------------------------------------------------------------
  // Validação e envio
  // ---------------------------------------------------------------------------

  bool get _cartaoValido {
    if (_forma != FormaPagamento.cartaoCredito) return true;
    return _soDigitos(_numeroCtrl.text).length >= 13 &&
        _nomeCtrl.text.trim().length > 2 &&
        _soDigitos(_validadeCtrl.text).length == 4 &&
        _soDigitos(_cvvCtrl.text).length >= 3;
  }

  Future<void> _pagar() async {
    setState(() => _erro = null);

    if (!_cartaoValido) {
      setState(() => _erro = 'Confira os dados do cartão antes de continuar.');
      return;
    }

    setState(() => _pagando = true);

    // Simulação: só um delay pra parecer um processamento real. Nenhum dado
    // de cartão é validado de verdade nem enviado a lugar nenhum — é a
    // chamada a PedidoService.criar logo abaixo que grava o pedido como PAGO.
    await Future.delayed(const Duration(milliseconds: 1400));
    if (!mounted) return;

    try {
      final dados = widget.dados;
      final pedido = await _pedidoService.criar(
        dataInicio: dados.dataInicio,
        dataFim: dados.dataFim,
        tipoEntrega: dados.tipoEntrega,
        enderecoId: dados.enderecoId,
        enderecoNovo: dados.enderecoNovo,
        observacoesCliente: dados.observacoesCliente,
        itens: dados.itens,
        formaPagamento: _forma,
      );
      if (!mounted) return;
      CartState.limpar();
      Navigator.of(context).pop(pedido);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _pagando = false;
        _erro = e is PedidoInvalidoException || e is Exception
            ? e.toString().replaceFirst('Exception: ', '')
            : 'Não foi possível concluir o pagamento. Tente novamente.';
      });
    }
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('Pagamento'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const Text(
              'Simulação de pagamento — nenhum valor é cobrado de verdade.',
              style: TextStyle(color: Colors.black54, fontSize: 12),
            ),
            const SizedBox(height: 16),
            _Cartao(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Forma de pagamento', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  if (_erro != null) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: Colors.red.shade50,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.red.shade200),
                      ),
                      child: Text(_erro!, style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
                    ),
                    const SizedBox(height: 12),
                  ],
                  Row(
                    children: [
                      for (final forma in FormaPagamento.values) ...[
                        Expanded(child: _buildAbaForma(forma)),
                        if (forma != FormaPagamento.values.last) const SizedBox(width: 8),
                      ],
                    ],
                  ),
                  const SizedBox(height: 16),
                  if (_forma == FormaPagamento.cartaoCredito) ..._buildFormularioCartao(),
                  if (_forma == FormaPagamento.pix)
                    Text(
                      'Ao confirmar, geraríamos um QR Code Pix aqui (simulado — o pedido já sai marcado como pago).',
                      style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                    ),
                  if (_forma == FormaPagamento.boleto)
                    Text(
                      'Ao confirmar, geraríamos um boleto aqui (simulado — o pedido já sai marcado como pago).',
                      style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                    ),
                  const Divider(height: 32),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Total a pagar',
                          style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.black87)),
                      Text(
                        formatarMoeda(widget.dados.valorTotal),
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: Colors.orange),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: _pagando ? null : _pagar,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                      ),
                      child: _pagando
                          ? const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                ),
                                SizedBox(width: 10),
                                Text('Processando pagamento...'),
                              ],
                            )
                          : Text('Pagar ${formatarMoeda(widget.dados.valorTotal)}'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Center(
                    child: TextButton(
                      onPressed: _pagando ? null : () => Navigator.of(context).pop(),
                      child: const Text('Voltar ao carrinho'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAbaForma(FormaPagamento forma) {
    final selecionado = _forma == forma;
    return InkWell(
      onTap: () => setState(() => _forma = forma),
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selecionado ? Colors.orange.shade50 : Colors.white,
          border: Border.all(color: selecionado ? Colors.orange : Colors.grey.shade300),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Text(
          forma.label,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
            color: selecionado ? Colors.orange.shade800 : Colors.grey.shade700,
          ),
        ),
      ),
    );
  }

  List<Widget> _buildFormularioCartao() {
    return [
      _buildCampo('Número do cartão', _numeroCtrl,
          hint: '0000 0000 0000 0000', onChanged: _onNumeroChanged, teclado: TextInputType.number),
      _buildCampo('Nome impresso no cartão', _nomeCtrl, hint: 'Como está no cartão'),
      Row(
        children: [
          Expanded(
            child: _buildCampo('Validade', _validadeCtrl,
                hint: 'MM/AA', onChanged: _onValidadeChanged, teclado: TextInputType.number),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _buildCampo(
              'CVV',
              _cvvCtrl,
              hint: '000',
              teclado: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)],
            ),
          ),
        ],
      ),
    ];
  }

  Widget _buildCampo(
    String rotulo,
    TextEditingController controller, {
    String? hint,
    ValueChanged<String>? onChanged,
    TextInputType? teclado,
    List<TextInputFormatter>? inputFormatters,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextField(
        controller: controller,
        keyboardType: teclado,
        onChanged: onChanged,
        inputFormatters: inputFormatters,
        decoration: InputDecoration(
          labelText: rotulo,
          hintText: hint,
          border: const OutlineInputBorder(),
          isDense: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        ),
      ),
    );
  }
}

class _Cartao extends StatelessWidget {
  final Widget child;
  const _Cartao({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: child,
    );
  }
}
