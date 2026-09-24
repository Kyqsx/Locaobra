/// Endereço do cliente — espelha EnderecoRequest/EnderecoResponse da API
/// (GET/POST /api/clientes/meus-enderecos). Usado tanto na listagem de
/// endereços salvos quanto no formulário de endereço novo do checkout.
class Endereco {
  /// Nulo para um endereço digitado na hora (ainda não salvo).
  final int? id;
  final String apelido;
  final String cep;
  final String rua;
  final String numero;
  final String complemento;
  final String bairro;
  final String cidade;
  final String estado;
  final bool principal;

  /// Linha única pronta pra exibição (só vem preenchida pela API).
  final String? formatado;

  const Endereco({
    this.id,
    this.apelido = '',
    this.cep = '',
    this.rua = '',
    this.numero = '',
    this.complemento = '',
    this.bairro = '',
    this.cidade = '',
    this.estado = '',
    this.principal = false,
    this.formatado,
  });

  /// Endereço em branco, pronto pro formulário de "endereço novo".
  static const vazio = Endereco();

  factory Endereco.fromJson(Map<String, dynamic> json) {
    return Endereco(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}'),
      apelido: json['apelido']?.toString() ?? '',
      cep: json['cep']?.toString() ?? '',
      rua: json['rua']?.toString() ?? '',
      numero: json['numero']?.toString() ?? '',
      complemento: json['complemento']?.toString() ?? '',
      bairro: json['bairro']?.toString() ?? '',
      cidade: json['cidade']?.toString() ?? '',
      estado: json['estado']?.toString() ?? '',
      principal: json['principal'] == true,
      formatado: json['formatado']?.toString(),
    );
  }

  /// Formato que o EnderecoRequest da API espera (POST/PUT e no pedido).
  Map<String, dynamic> toRequestJson() => {
        'apelido': apelido.trim().isEmpty ? null : apelido.trim(),
        'cep': cep.trim().isEmpty ? null : cep.trim(),
        'rua': rua.trim(),
        'numero': numero.trim().isEmpty ? null : numero.trim(),
        'complemento': complemento.trim().isEmpty ? null : complemento.trim(),
        'bairro': bairro.trim().isEmpty ? null : bairro.trim(),
        'cidade': cidade.trim(),
        'estado': estado.trim().toUpperCase(),
        'principal': principal,
      };

  /// Resumo pra exibir como subtítulo quando [formatado] não veio da API
  /// (ex.: endereço ainda sendo digitado no checkout).
  String get resumo {
    if (formatado != null && formatado!.isNotEmpty) return formatado!;
    final partes = <String>[
      if (rua.trim().isNotEmpty) rua.trim(),
      if (numero.trim().isNotEmpty) numero.trim(),
      if (bairro.trim().isNotEmpty) bairro.trim(),
      if (cidade.trim().isNotEmpty) cidade.trim(),
      if (estado.trim().isNotEmpty) estado.trim(),
    ];
    return partes.join(', ');
  }

  bool get temMinimoParaFrete => cidade.trim().isNotEmpty && estado.trim().isNotEmpty;

  Endereco copyWith({
    String? apelido,
    String? cep,
    String? rua,
    String? numero,
    String? complemento,
    String? bairro,
    String? cidade,
    String? estado,
    bool? principal,
  }) {
    return Endereco(
      id: id,
      apelido: apelido ?? this.apelido,
      cep: cep ?? this.cep,
      rua: rua ?? this.rua,
      numero: numero ?? this.numero,
      complemento: complemento ?? this.complemento,
      bairro: bairro ?? this.bairro,
      cidade: cidade ?? this.cidade,
      estado: estado ?? this.estado,
      principal: principal ?? this.principal,
      formatado: formatado,
    );
  }
}
