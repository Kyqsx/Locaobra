import 'package:flutter_test/flutter_test.dart';
import 'package:locaobra_mobile/utils/formatters.dart';

void main() {
  group('abreviarNome', () {
    test('nome com uma palavra volta como está', () {
      expect(abreviarNome('Guilherme'), 'Guilherme');
    });

    test('nome e sobrenome já são primeiro e último', () {
      expect(abreviarNome('Maria Souza'), 'Maria Souza');
    });

    test('nome grande é abreviado para primeiro + último nome', () {
      expect(
        abreviarNome('Guilherme Augusto da Silva Almeida'),
        'Guilherme Almeida',
      );
      expect(
        abreviarNome('Maria José Santos Oliveira'),
        'Maria Oliveira',
      );
    });

    test('espaços extras são normalizados', () {
      expect(abreviarNome('  João   da Silva  '), 'João Silva');
    });

    test('string vazia ou só espaços resulta em vazio', () {
      expect(abreviarNome(''), '');
      expect(abreviarNome('   '), '');
    });
  });
}