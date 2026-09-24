import 'package:flutter/material.dart';
import 'package:locaobra_mobile/cart/cart_state.dart';
import 'package:locaobra_mobile/screens/welcome_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Restaura o carrinho salvo no aparelho antes da primeira tela.
  await CartState.carregar();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const WelcomeScreen(),
      
    );
  }
}

