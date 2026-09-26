import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:locaobra_mobile/entregador/models/expedicao.dart';
import 'package:locaobra_mobile/services/api_client.dart';

/// Resultado padrão das ações que alteram uma expedição (confirmar entrega,
/// não-realizada). Em caso de sucesso já devolve a expedição atualizada pra
/// tela não precisar recarregar a lista inteira.
class ExpedicaoResult {
  final bool sucesso;
  final Expedicao? expedicao;
  final String? mensagemErro;

  ExpedicaoResult.sucesso(this.expedicao)
    : sucesso = true,
      mensagemErro = null;

  ExpedicaoResult.erro(this.mensagemErro)
    : sucesso = false,
      expedicao = null;
}

/// Espelha o consumo que expedicao.jsx faz de /api/expedicoes, mas só a
/// fatia usada pelo ENTREGADOR: listar as expedições em que ele é o
/// motorista designado (a API já filtra isso sozinha a partir do token —
/// ver ExpedicaoService.filtrarPorMotorista no backend), ver o detalhe, e
/// registrar a confirmação de entrega/coleta ou uma ocorrência.
class ExpedicaoApiService {
  Future<List<Expedicao>> listar() async {
    final response = await ApiClient.get('/api/expedicoes');
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível carregar as expedições (status ${response.statusCode}).');
    }
    final data = jsonDecode(response.body);
    if (data is! List) return [];
    return data
        .map((item) => Expedicao.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Expedicao> buscarPorId(int id) async {
    final response = await ApiClient.get('/api/expedicoes/$id');
    if (response.statusCode != 200) {
      throw Exception(_extrairMensagem(response.body) ??
          'Não foi possível carregar a expedição (status ${response.statusCode}).');
    }
    return Expedicao.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// Passo 3 do fluxo (POST /{id}/confirmar-entrega, multipart): nome +
  /// documento de quem recebeu, a assinatura desenhada (PNG) e a foto
  /// tirada no local. A data/hora fica por conta do servidor.
  Future<ExpedicaoResult> confirmarEntrega({
    required int id,
    required String assinatura,
    required String documento,
    required List<int> assinaturaImagemBytes,
    required List<int> fotoBytes,
    required String fotoNomeArquivo,
    String? observacao,
  }) async {
    try {
      final streamed = await ApiClient.postMultipart(
        '/api/expedicoes/$id/confirmar-entrega',
        campos: {
          'assinatura': assinatura,
          'documento': documento,
          if (observacao != null && observacao.isNotEmpty) 'observacao': observacao,
        },
        arquivos: {
          'assinaturaImagem': http.MultipartFile.fromBytes(
            'assinaturaImagem',
            assinaturaImagemBytes,
            filename: 'assinatura.png',
            contentType: MediaType('image', 'png'),
          ),
          'foto': http.MultipartFile.fromBytes(
            'foto',
            fotoBytes,
            filename: fotoNomeArquivo,
            contentType: _contentTypeDaFoto(fotoNomeArquivo),
          ),
        },
      );
      final response = await http.Response.fromStream(streamed);
      if (response.statusCode != 200) {
        return ExpedicaoResult.erro(_extrairMensagem(response.body) ??
            'Erro ao confirmar (status ${response.statusCode}).');
      }
      return ExpedicaoResult.sucesso(
        Expedicao.fromJson(jsonDecode(response.body) as Map<String, dynamic>),
      );
    } catch (_) {
      return ExpedicaoResult.erro('Não foi possível conectar ao servidor.');
    }
  }

  /// O entregador chegou e não conseguiu entregar/coletar (cliente ausente,
  /// recusou, endereço não encontrado...). Encerra a expedição com o motivo.
  Future<ExpedicaoResult> registrarNaoRealizada({
    required int id,
    required String motivo,
  }) async {
    try {
      final response = await ApiClient.post(
        '/api/expedicoes/$id/nao-realizada',
        {'motivo': motivo},
      );
      if (response.statusCode != 200) {
        return ExpedicaoResult.erro(_extrairMensagem(response.body) ??
            'Erro ao registrar ocorrência (status ${response.statusCode}).');
      }
      return ExpedicaoResult.sucesso(
        Expedicao.fromJson(jsonDecode(response.body) as Map<String, dynamic>),
      );
    } catch (_) {
      return ExpedicaoResult.erro('Não foi possível conectar ao servidor.');
    }
  }

  /// O backend (ExpedicaoController.exigirImagem) rejeita o upload se o
  /// Content-Type do part não começar com "image/" — e MultipartFile.fromBytes
  /// manda "application/octet-stream" por padrão quando não informamos o
  /// contentType. Por isso montamos aqui a partir da extensão do arquivo que
  /// o image_picker devolveu, caindo em image/jpeg (padrão da câmera) se não
  /// reconhecer a extensão.
  MediaType _contentTypeDaFoto(String nomeArquivo) {
    final ext = nomeArquivo.toLowerCase().split('.').last;
    switch (ext) {
      case 'png':
        return MediaType('image', 'png');
      case 'heic':
        return MediaType('image', 'heic');
      case 'heif':
        return MediaType('image', 'heif');
      case 'webp':
        return MediaType('image', 'webp');
      case 'jpg':
      case 'jpeg':
      default:
        return MediaType('image', 'jpeg');
    }
  }

  /// A API costuma responder erros como {status, message, timestamp}.
  String? _extrairMensagem(String body) {
    try {
      final data = jsonDecode(body);
      if (data is Map<String, dynamic> && data['message'] != null) {
        return data['message'].toString();
      }
    } catch (_) {
      // corpo não é JSON, ignora
    }
    return null;
  }
}