// Formatadores pt-BR usados pelas telas (sem depender do pacote intl).

/// 1234.5 -> "R$ 1.234,50"
String formatarMoeda(num valor) {
  final sinal = valor < 0 ? '-' : '';
  final centavosTotais = (valor.abs() * 100).round();
  final reais = centavosTotais ~/ 100;
  final centavos = centavosTotais % 100;
  final reaisComPonto = reais.toString().replaceAllMapped(
    RegExp(r'\B(?=(\d{3})+(?!\d))'),
    (match) => '.',
  );
  final centavosTexto = centavos.toString().padLeft(2, '0');
  return '${sinal}R\$ $reaisComPonto,$centavosTexto';
}

/// 4.3 -> "4,3" (sempre 1 casa decimal, igual ao formatarMedia do web).
String formatarMedia(double media) => media.toStringAsFixed(1).replaceAll('.', ',');

/// DateTime -> "dd/MM/yyyy".
String formatarData(DateTime data) {
  final dia = data.day.toString().padLeft(2, '0');
  final mes = data.month.toString().padLeft(2, '0');
  return '$dia/$mes/${data.year}';
}

/// Abrevia o nome pro header: "Guilherme Augusto da Silva Almeida" vira
/// "Guilherme Almeida" — primeiro e último nome, pra caber em telas
/// estreitas sem estourar o layout. Nomes com 1 ou 2 palavras voltam
/// como estão (nesse caso "primeiro e último" já é o nome inteiro).
String abreviarNome(String nome) {
  final partes = nome
      .trim()
      .split(RegExp(r'\s+'))
      .where((p) => p.isNotEmpty)
      .toList();
  if (partes.length <= 2) return partes.join(' ');
  return '${partes.first} ${partes.last}';
}
