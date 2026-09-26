import 'package:locaobra_mobile/cart/cart_item.dart';
import 'package:locaobra_mobile/models/endereco.dart';
import 'package:locaobra_mobile/services/pedido_service.dart';

/// Tudo que o carrinho já validou e resolveu, carregado até a tela de
/// pagamento simulado — que é quem de fato chama `PedidoService.criar`
/// depois do "pagamento". Equivalente ao `payload` que o carrinho.jsx do
/// web monta e manda via `location.state` pra `pagamento.jsx`.
class DadosCheckout {
  final DateTime dataInicio;
  final DateTime dataFim;
  final TipoEntrega tipoEntrega;

  /// No máximo um dos dois preenchido — endereço salvo (por id) ou digitado
  /// na hora. Ambos nulos quando `tipoEntrega` é retirada.
  final int? enderecoId;
  final Endereco? enderecoNovo;

  final String? observacoesCliente;
  final List<CartItem> itens;

  /// Total já calculado no carrinho (itens + frete) — só pra exibir na tela
  /// de pagamento, não é reenviado à API.
  final double valorTotal;

  const DadosCheckout({
    required this.dataInicio,
    required this.dataFim,
    required this.tipoEntrega,
    this.enderecoId,
    this.enderecoNovo,
    this.observacoesCliente,
    required this.itens,
    required this.valorTotal,
  });
}
