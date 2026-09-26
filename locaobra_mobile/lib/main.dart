import 'package:flutter/material.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/cart/cart_state.dart';
import 'package:locaobra_mobile/entregador/screens/entregador_home_page.dart';
import 'package:locaobra_mobile/screens/home_screen.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/services/auth_service.dart';
import 'package:locaobra_mobile/services/token_storage.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Restaura o carrinho salvo no aparelho antes da primeira tela.
  await CartState.carregar();

  // Restaura a sessão: o token fica salvo no aparelho (TokenStorage), mas
  // o AuthState (quem está logado, tipo, cargo...) é só em memória e some
  // toda vez que o processo do app é encerrado. Sem isso, o app sempre
  // abria deslogado mesmo com um token válido guardado.
  final telaInicial = await _restaurarSessao();

  runApp(MyApp(telaInicial: telaInicial));
}

/// Se houver um token salvo, confirma com a API (GET /api/auth/me) que ele
/// ainda é válido e repopula o AuthState antes de decidir a primeira tela.
/// Token ausente ou inválido/expirado -> segue deslogado (e limpa o token
/// morto, pra não tentar de novo em toda chamada).
Future<Widget> _restaurarSessao() async {
  String? token;
  try {
    token = await TokenStorage.obter();
  } catch (_) {
    token = null;
  }

  if (token == null) return const WelcomeScreen();

  final resultado = await AuthService().restaurarSessao();

  if (resultado.tokenInvalido) {
    await TokenStorage.limpar();
    return const WelcomeScreen();
  }

  final perfil = resultado.perfil;
  if (perfil == null) {
    // Sem internet / erro temporário: mantém o token salvo e entra
    // deslogado só nessa sessão — tenta restaurar de novo na próxima vez
    // que o app abrir.
    return const WelcomeScreen();
  }

  AuthState.login(perfil.nome ?? '');
  AuthState.definirPerfil(
    tipo: perfil.tipo,
    cargo: perfil.cargoFuncionario,
    idFuncionario: perfil.idFuncionario,
  );

  return AuthState.ehEntregador ? const EntregadorHomePage() : const HomeScreen();
}

class MyApp extends StatelessWidget {
  final Widget telaInicial;
  const MyApp({super.key, required this.telaInicial});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: telaInicial,
    );
  }
}

