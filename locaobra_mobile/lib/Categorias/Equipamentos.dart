import 'package:flutter/material.dart';

class EquipamentosPage extends StatelessWidget {
  const EquipamentosPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Equipamentos Pesados'),
        backgroundColor: Colors.orange,
        foregroundColor: Colors.white,
      ),
      body: const Center(
        child: Text('Lista de produtos de Equipamentos Pesados em breve.'),
      ),
    );
  }
}