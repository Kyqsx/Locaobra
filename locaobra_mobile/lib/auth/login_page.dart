import 'package:flutter/material.dart';
import 'package:locaobra_mobile/auth/auth_state.dart';
import 'package:locaobra_mobile/auth/cadastro_page.dart';
import 'package:locaobra_mobile/entregador/screens/entregador_home_page.dart';
import 'package:locaobra_mobile/screens/home_screen.dart';
import 'package:locaobra_mobile/services/auth_service.dart';
import 'package:locaobra_mobile/theme/app_theme.dart';
import 'package:locaobra_mobile/widgets/header_voltar.dart';

class LoginPage extends StatefulWidget {
  /// Quando true, ao concluir o login a tela só fecha devolvendo `true`
  /// (usado pela tela de produto, que precisa voltar pro mesmo lugar em vez
  /// de ir pra Home). O padrão continua sendo ir pra Home/área do entregador.
  final bool voltarAoConcluir;

  const LoginPage({super.key, this.voltarAoConcluir = false});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _senhaController = TextEditingController();

  bool _senhaVisivel = false;
  bool _carregando = false;
  String? _erro;

  final _authService = AuthService();

  @override
  void dispose() {
    _emailController.dispose();
    _senhaController.dispose();
    super.dispose();
  }

  void _entrar() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _carregando = true;
      _erro = null;
    });

    final resultado = await _authService.login(
      _emailController.text.trim(),
      _senhaController.text,
    );

    if (!mounted) return;
    setState(() => _carregando = false);

    if (!resultado.sucesso) {
      setState(() => _erro = resultado.mensagemErro);
      return;
    }

    AuthState.login(resultado.nome ?? _emailController.text.trim());

    // GET /api/auth/me — descobre se quem logou é FUNCIONARIO com cargo
    // ENTREGADOR. É esse cargo que decide a tela seguinte: dashboard do
    // entregador (mobile) ou o app normal de cliente. Se a busca falhar por
    // qualquer motivo, trata como cliente (comportamento já existente).
    final perfil = await _authService.buscarPerfil();
    AuthState.definirPerfil(
      tipo: perfil?.tipo,
      cargo: perfil?.cargoFuncionario,
      idFuncionario: perfil?.idFuncionario,
    );

    if (!mounted) return;

    if (widget.voltarAoConcluir) {
      Navigator.pop(context, true);
      return;
    }

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (_) => AuthState.ehEntregador
            ? const EntregadorHomePage()
            : const HomeScreen(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: const HeaderVoltar(),
      extendBodyBehindAppBar: true,
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(
          24, MediaQuery.of(context).padding.top + kToolbarHeight, 24, 0,
        ),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 16),


                const SizedBox(height: 4),

                // Logo
                Stack(
                  alignment: Alignment.center,
                  children: [
                    Align(alignment: Alignment.centerLeft),
                    Image.asset(
                      'assets/imagens/Logo_LOCAOBRA.png',
                      width: 160,
                      height: 100,
                    ),
                  ],
                ),

                const SizedBox(height: 32),

                const Text(
                  'Login',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),

                const SizedBox(height: 8),

                Text(
                  'Entre com sua conta para continuar.',
                  style: TextStyle(fontSize: 14, color: AppColors.gray600),
                ),

                const SizedBox(height: 32),

                // Campo e-mail
                const Text(
                  'E-mail',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: InputDecoration(
                    hintText: 'seuemail@exemplo.com',
                    prefixIcon: const Icon(Icons.email_outlined),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.xl),
                    ),
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Informe seu e-mail';
                    }
                    if (!value.contains('@')) {
                      return 'E-mail inválido';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 20),

                // Campo senha
                const Text(
                  'Senha',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _senhaController,
                  obscureText: !_senhaVisivel,
                  decoration: InputDecoration(
                    hintText: '••••••••',
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _senhaVisivel
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                      ),
                      onPressed: () {
                        setState(() => _senhaVisivel = !_senhaVisivel);
                      },
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.xl),
                    ),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Informe sua senha';
                    }
                    if (value.length < 6) {
                      return 'A senha deve ter pelo menos 6 caracteres';
                    }
                    return null;
                  },
                ),

                const SizedBox(height: 8),

                // Esqueci minha senha
                Align(
                  alignment: Alignment.center,
                  child: TextButton(
                    onPressed: () {},
                    child: const Text('Esqueci minha senha'),
                  ),
                ),

                if (_erro != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    _erro!,
                    style: const TextStyle(color: AppColors.error, fontSize: 13),
                  ),
                ],

                const SizedBox(height: 16),

                // Botão Entrar
                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color.fromARGB(255, 255, 128, 0),
                      foregroundColor: AppColors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(AppRadius.xl2),
                      ),
                      elevation: 0,
                    ),
                    onPressed: _carregando ? null : _entrar,
                    child: _carregando
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              color: AppColors.white,
                              strokeWidth: 2.5,
                            ),
                          )
                        : const Text(
                            'Entrar',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                  ),
                ),

                const SizedBox(height: 24),

                // Link para cadastro
                Center(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('Não tem uma conta?'),
                      TextButton(
                        onPressed: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const CadastroPage(),
                            ),
                          );
                        },
                        child: const Text(
                          'Criar conta',
                          style: TextStyle(
                            fontWeight: FontWeight.w600),
                          
                        ),
                        
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
    );
  }
}
