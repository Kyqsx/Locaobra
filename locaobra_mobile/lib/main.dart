import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';
import 'package:locaobra_mobile/Categorias/catalogo_page.dart'; // Ajuste o caminho se necessário

// Definição do GoRouter com as rotas
final GoRouter _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const WelcomeScreen(),
    ),
    GoRoute(
      path: '/catalogo/:categoria',
      builder: (context, state) {
        final categoriaSlug = state.pathParameters['categoria'] ?? '';
        return CatalogoPagina(categoriaSlug: categoriaSlug);
      },
    ),
  ],
);

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Altere para MaterialApp.router para aceitar o _router
    return MaterialApp.router(
      debugShowCheckedModeBanner: false,
      routerConfig: _router,
    );
  }
}