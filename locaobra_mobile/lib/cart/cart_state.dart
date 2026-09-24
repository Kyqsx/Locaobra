import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:locaobra_mobile/models/equipamento.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'cart_item.dart';

/// Estado do carrinho compartilhado pelo app inteiro — equivalente ao
/// CartContext do locaobra_web. Segue o mesmo padrão do AuthState: membros
/// estáticos + ValueNotifier, então qualquer tela pode ouvir com um
/// ValueListenableBuilder.
///
/// O carrinho é salvo no aparelho (shared_preferences), como o web faz no
/// localStorage. Chame [carregar] uma vez no main() antes do runApp.
class CartState {
  CartState._();

  static const String _chaveStorage = 'locaobra_carrinho';

  static final ValueNotifier<List<CartItem>> itens =
      ValueNotifier<List<CartItem>>(const <CartItem>[]);

  /// Soma das quantidades (o número do "balãozinho" do ícone do carrinho).
  static int get totalItens =>
      itens.value.fold<int>(0, (soma, item) => soma + item.quantidade);

  /// Custo de UMA diária de tudo que está no carrinho.
  static double get totalDiaria =>
      itens.value.fold<double>(0, (soma, item) => soma + item.subtotalDiaria);

  /// Restaura o carrinho salvo. Nunca lança: falha de storage vira carrinho vazio.
  static Future<void> carregar() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final bruto = prefs.getString(_chaveStorage);
      if (bruto == null || bruto.isEmpty) return;

      final decodificado = jsonDecode(bruto);
      if (decodificado is! List) return;

      itens.value = List<CartItem>.unmodifiable(
        decodificado
            .whereType<Map<String, dynamic>>()
            .map(CartItem.fromJson)
            .where((item) => item.equipamentoId > 0 && item.quantidade > 0),
      );
    } catch (e) {
      debugPrint('CartState: falha ao carregar o carrinho salvo ($e).');
    }
  }

  /// Igual ao adicionarItem do web: se o equipamento já está no carrinho,
  /// soma a quantidade (limitada ao disponível); senão cria o item.
  ///
  /// Devolve true se o carrinho mudou, ou false se nada foi alterado
  /// (equipamento sem estoque ou carrinho já com todas as unidades).
  static bool adicionarItem(
    Equipamento equipamento, {
    int quantidade = 1,
    String observacaoItem = '',
  }) {
    final disponivel = equipamento.quantidadeDisponivel;
    if (disponivel < 1) return false;

    final lista = List<CartItem>.of(itens.value);
    final indice = lista.indexWhere((i) => i.equipamentoId == equipamento.id);

    if (indice >= 0) {
      final existente = lista[indice];
      // Adicione o .toInt() no final da função min
      final novaQuantidade = min(
        disponivel,
        existente.quantidade + max(1, quantidade),
      ).toInt(); // <--- Mudança aqui

      final mudou = novaQuantidade != existente.quantidade;
      if (mudou || existente.quantidadeDisponivel != disponivel) {
        lista[indice] = existente.copyWith(
          quantidade: novaQuantidade,
          quantidadeDisponivel: disponivel,
        );
        _definir(lista);
      }
      return mudou;
    }

    final foto = equipamento.fotos.isNotEmpty ? equipamento.fotos.first : null;
    lista.add(
      CartItem(
        equipamentoId: equipamento.id,
        nome: equipamento.nome,
        imagem: foto?.url,
        focoX: foto?.focoX ?? 50,
        focoY: foto?.focoY ?? 50,
        valorDiaria: equipamento.valorDiaria,
        quantidadeDisponivel: disponivel,
        quantidade: min(disponivel, max(1, quantidade)),
        observacaoItem: observacaoItem,
      ),
    );
    _definir(lista);
    return true;
  }

  static void atualizarQuantidade(int equipamentoId, int quantidade) {
    _definir([
      for (final item in itens.value)
        if (item.equipamentoId == equipamentoId)
          item.copyWith(
            quantidade: min(item.quantidadeDisponivel, max(1, quantidade)),
          )
        else
          item,
    ]);
  }

  static void atualizarObservacao(int equipamentoId, String observacaoItem) {
    _definir([
      for (final item in itens.value)
        if (item.equipamentoId == equipamentoId)
          item.copyWith(observacaoItem: observacaoItem)
        else
          item,
    ]);
  }

  static void removerItem(int equipamentoId) {
    _definir(
      itens.value.where((i) => i.equipamentoId != equipamentoId).toList(),
    );
  }

  static void limpar() => _definir(const <CartItem>[]);

  static void _definir(List<CartItem> nova) {
    itens.value = List<CartItem>.unmodifiable(nova);
    unawaited(_persistir());
  }

  static Future<void> _persistir() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(
        _chaveStorage,
        jsonEncode(itens.value.map((i) => i.toJson()).toList()),
      );
    } catch (e) {
      // Sem storage o carrinho continua funcionando em memória.
      debugPrint('CartState: falha ao salvar o carrinho ($e).');
    }
  }
}
