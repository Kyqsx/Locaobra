import 'package:flutter/material.dart';

class AndaimesPage extends StatelessWidget {
  const AndaimesPage ({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Andaimes e Escadas'),
        backgroundColor: Colors.orange,
        foregroundColor: Colors.white,
      ),
      body: const Center(
        child: Text('Lista de produtos de Andaimes e Escadas em breve.'),
      ),
    );
  }
}