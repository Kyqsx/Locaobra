import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Guarda o token de autenticação.
///
/// - Mobile (Android/iOS): flutter_secure_storage (Keychain / EncryptedSharedPreferences).
/// - Web: shared_preferences (localStorage). O flutter_secure_storage não tem
///   uma implementação web confiável em todos os cenários (dá
///   MissingPluginException dependendo do setup) — localStorage é o padrão
///   aceito pra token em app web mesmo, já que o navegador não oferece um
///   cofre nativo de verdade de qualquer forma.
class TokenStorage {
  TokenStorage._();

  static const _storage = FlutterSecureStorage();
  static const _tokenKey = 'auth_token';

  static Future<void> salvar(String token) async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_tokenKey, token);
    } else {
      await _storage.write(key: _tokenKey, value: token);
    }
  }

  static Future<String?> obter() async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString(_tokenKey);
    }
    return _storage.read(key: _tokenKey);
  }

  static Future<void> limpar() async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_tokenKey);
    } else {
      await _storage.delete(key: _tokenKey);
    }
  }
}
