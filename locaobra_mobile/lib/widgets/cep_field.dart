import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:locaobra_mobile/services/viacep_service.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';

/// Campo de CEP que formata "00000-000" enquanto digita e, ao completar os
/// 8 dígitos, consulta o ViaCEP e preenche sozinho rua/bairro/cidade/UF —
/// usado tanto em "Meus endereços" quanto no endereço novo do checkout.
///
/// Recebe os TextEditingControllers dos campos de destino e só escreve
/// neles (`.text = ...`); quem usa o widget não precisa saber que o
/// ViaCEP existe, só passar os controllers que já tinha.
class CepField extends StatefulWidget {
  final TextEditingController cepController;
  final TextEditingController ruaController;
  final TextEditingController bairroController;
  final TextEditingController cidadeController;
  final TextEditingController estadoController;
  final String label;

  const CepField({
    super.key,
    required this.cepController,
    required this.ruaController,
    required this.bairroController,
    required this.cidadeController,
    required this.estadoController,
    this.label = 'CEP',
  });

  @override
  State<CepField> createState() => _CepFieldState();
}

class _CepFieldState extends State<CepField> {
  bool _buscando = false;
  bool _naoEncontrado = false;
  String? _ultimoCepBuscado;

  Future<void> _buscar(String cepDigitado) async {
    final digitos = ViaCepService.apenasDigitos(cepDigitado);
    if (digitos.length != 8) return;

    _ultimoCepBuscado = digitos;
    setState(() {
      _buscando = true;
      _naoEncontrado = false;
    });

    try {
      final endereco = await ViaCepService.buscar(digitos);
      // Se o usuário já mudou o CEP de novo enquanto a busca estava no ar,
      // descarta a resposta pra não sobrescrever com endereço desatualizado.
      if (!mounted || _ultimoCepBuscado != digitos) return;

      if (endereco == null) {
        setState(() => _naoEncontrado = true);
        return;
      }

      if (endereco.rua.isNotEmpty) widget.ruaController.text = endereco.rua;
      if (endereco.bairro.isNotEmpty) widget.bairroController.text = endereco.bairro;
      if (endereco.cidade.isNotEmpty) widget.cidadeController.text = endereco.cidade;
      if (endereco.estado.isNotEmpty) widget.estadoController.text = endereco.estado;
    } catch (_) {
      // Falha de rede não deve travar o formulário — o usuário ainda pode
      // preencher o endereço manualmente.
    } finally {
      if (mounted && _ultimoCepBuscado == digitos) {
        setState(() => _buscando = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: widget.cepController,
            keyboardType: TextInputType.number,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(8),
              _CepMaskFormatter(),
            ],
            onChanged: (valor) => _buscar(valor),
            decoration: InputDecoration(
              labelText: widget.label,
              hintText: '00000-000',
              border: const OutlineInputBorder(),
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
              suffixIcon: _buscando
                  ? const Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  : null,
            ),
          ),
          if (_naoEncontrado)
            Padding(
              padding: const EdgeInsets.only(top: 4, left: 4),
              child: Text(
                'CEP não encontrado, preencha o endereço manualmente.',
                style: TextStyle(fontSize: 11, color: AppColors.error),
              ),
            ),
        ],
      ),
    );
  }
}

/// Formata os dígitos digitados como "00000-000" (o FilteringTextInputFormatter
/// já garante que só chegam dígitos aqui).
class _CepMaskFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final formatado = ViaCepService.formatar(newValue.text);
    return TextEditingValue(
      text: formatado,
      selection: TextSelection.collapsed(offset: formatado.length),
    );
  }
}
