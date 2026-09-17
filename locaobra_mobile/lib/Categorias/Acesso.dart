import 'package:flutter/material.dart';

class AcessoPage extends StatelessWidget {
  const AcessoPage ({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Acesso e Elevação'),
        backgroundColor: Colors.orange,
        foregroundColor: Colors.white,
      ),
      body: const Center(
        child: Text('Lista de produtos de Acesso e Elevação em breve.'),
      ),
    );
  }
}