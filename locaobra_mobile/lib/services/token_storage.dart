import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Guarda o token de autenticação de forma segura (Keychain no iOS,
/// EncryptedSharedPreferences no Android), pra o ApiClient conseguir
/// anexar o Authorization automaticamente sem precisar repassar o token
/// manualmente em cada chamada.
class TokenStorage {
  TokenStorage._();

  static const _storage = FlutterSecureStorage();
  static const _tokenKey = 'auth_token';

  static Future<void> salvar(String token) {
    return _storage.write(key: _tokenKey, value: token);
  }

  static Future<String?> obter() {
    return _storage.read(key: _tokenKey);
  }

  static Future<void> limpar() {
    return _storage.delete(key: _tokenKey);
  }
}
