import 'package:flutter/foundation.dart';

// Estado de login compartilhado pelo app inteiro.
//
// É um "singleton" simples: em vez de usar um pacote de gerenciamento de
// estado (Provider, Riverpod, etc.), guardamos o nome do usuário logado
// num ValueNotifier estático. Qualquer tela pode ouvir mudanças aqui com
// um ValueListenableBuilder, e são avisadas automaticamente quando o
// usuário faz login ou logout.
class AuthState {
  AuthState._(); // impede criar instâncias (só usamos os membros static)

  // null = ninguém logado. Com alguém logado, guarda o nome de exibição.
  static final ValueNotifier<String?> usuarioLogado = ValueNotifier<String?>(
    null,
  );

  static bool get estaLogado => usuarioLogado.value != null;

  static void login(String nome) {
    usuarioLogado.value = nome;
  }

  static void logout() {
    usuarioLogado.value = null;
  }
}