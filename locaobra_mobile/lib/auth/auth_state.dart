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

  // 'CLIENTE' ou 'FUNCIONARIO' — vem de GET /api/auth/me (campo "tipo").
  static String? tipoUsuario;

  // Cargo do funcionário (ex.: 'ENTREGADOR', 'CONFERENTE', 'ADMIN'...), só
  // preenchido quando tipoUsuario == 'FUNCIONARIO'. É o mesmo campo
  // "cargoFuncionario" que o web usa (useAuth) pra decidir o que mostrar.
  static String? cargoFuncionario;

  static int? idFuncionario;

  // Único ponto de verdade pra saber se quem logou é o app do entregador
  // (área "dashboard" no mobile) — por enquanto o único cargo com acesso
  // ao mobile fora do fluxo de cliente.
  static bool get ehEntregador => cargoFuncionario == 'ENTREGADOR';

  static bool get estaLogado => usuarioLogado.value != null;

  static void login(String nome) {
    usuarioLogado.value = nome;
  }

  /// Preenche os dados de perfil vindos de GET /api/auth/me.
  static void definirPerfil({
    String? tipo,
    String? cargo,
    int? idFuncionario,
  }) {
    tipoUsuario = tipo;
    cargoFuncionario = cargo;
    AuthState.idFuncionario = idFuncionario;
  }

  static void logout() {
    usuarioLogado.value = null;
    tipoUsuario = null;
    cargoFuncionario = null;
    idFuncionario = null;
  }
}