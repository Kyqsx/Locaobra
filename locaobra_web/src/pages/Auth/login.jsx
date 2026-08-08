import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../utils/useAuth';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faEyeSlash, faLock, faEnvelope, faHardHat, faRocket } from '@fortawesome/free-solid-svg-icons';
import api from '../../service/api';
import './auth.css';
import loginImg from '../../assets/AuthImage.png';

const Login = () => {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showError, setShowError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    console.log("--- INÍCIO DO PROCESSO DE LOGIN ---");
    console.log("Tentativa para:", email);

    if (!email || !senha) {
      setErrorMessage('Por favor, preencha todos os campos.');
      setShowError(true);
      return;
    }

    try {
      const response = await api.post('/api/auth/login', { email, senha });

      // LOG 1: Ver o que vem do Backend
      console.log("DEBUG: Resposta bruta do servidor:", response.data);

      const { token, tipo, id, nome } = response.data;

      // LOG 2: Verificar campos específicos
      console.log("DEBUG: Dados desestruturados ->", {
        temToken: !!token,
        tipo: tipo,
        id: id,
        nome: nome
      });

      if (!token || !tipo) {
        console.error("ERRO: O servidor não enviou token ou tipo de usuário!");
        throw new Error('Resposta inválida do servidor: Faltam dados essenciais (token/tipo).');
      }

      // LOG 3: Antes de chamar a função do Contexto
      console.log("DEBUG: Chamando função login() do useAuth...");
      login(email, token, { id, nome, tipo });

      // LOG 4: Verificar se o LocalStorage foi preenchido
      console.log("DEBUG: Token no LocalStorage após login():", localStorage.getItem('token'));

      console.log(`✅ Login bem-sucedido! Usuário tipo: ${tipo}. Redirecionando...`);

      if (tipo === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/');
      }

    } catch (error) {
      console.log("--- ERRO NO LOGIN ---");
      console.error('Erro detalhado:', error);

      let msgFim = 'Erro inesperado. Tente novamente.';

      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        console.log(`DEBUG: Erro HTTP ${status} recebido do backend.`);

        if (status === 401 || status === 403) {
          msgFim = 'Email ou senha inválidos.';
        } else {
          msgFim = typeof data === 'string' ? data : (data.message || 'Erro no servidor.');
        }
      } else if (error.request) {
        console.log("DEBUG: O servidor não respondeu à requisição.");
        msgFim = 'Não foi possível conectar ao servidor. Verifique sua internet.';
      }

      setErrorMessage(msgFim);
      setShowError(true);
    }
  };

  return (
    <div className="loginSplitPage">
      {/* Metade Esquerda: Imagem */}
      <div className="loginImageSide" style={{ backgroundImage: `url(${loginImg})` }}>
        <div className="imageOverlay">
          <div className="brandLogo">
            <FontAwesomeIcon icon={faHardHat} className="brandIcon" /> <span>LocaObra</span>
          </div>
          <h1>As melhores ferramentas para sua obra, a um clique de distância.</h1>
        </div>
      </div>

      {/* Metade Direita: Formulário */}
      <div className="loginFormSide">
        <div className="login-card">
          <h2 className="loginCardTitle">Bem-vindo de volta!</h2>
          <p>Entre com sua conta para alugar e gerenciar.</p>

          <form className="login-form" onSubmit={handleSubmit}>
            <div className="loginInputGroup">
              <label className="loginInputLabel">Email</label>
              <div className="inputWithIcon">
                <FontAwesomeIcon icon={faEnvelope} className="inputIcon" />
                <input
                  type="email"
                  placeholder="seu@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="loginInputField"
                />
              </div>
            </div>

            <div className="loginInputGroup">
              <label className="loginInputLabel">Senha</label>
              <div className="inputWithIcon">
                <FontAwesomeIcon icon={faLock} className="inputIcon" />
                <div className="loginPasswordWrapper">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Sua senha"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    required
                    className="loginInputField"
                  />
                  <button
                    type="button"
                    className="loginEye-button"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    <FontAwesomeIcon icon={showPassword ? faEye : faEyeSlash} />
                  </button>
                </div>
              </div>
            </div>

            <div className="forgotPassword">
              <a href="/recuperar-senha">Esqueci minha senha</a>
            </div>

            <button type="submit" className="login-button">Entrar</button>
          </form>

          <div className="login-footer">
            <p>Ainda não aluga conosco? <a href="/signup">Cadastre-se</a></p>
          </div>
        </div>
      </div>

      {/* Popup de Erro */}
      {showError && (
        <div className="loginPopup">
          <div className="loginPopup-content">
            <p>{errorMessage}</p>
            <button onClick={() => setShowError(false)}>Entendi</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Login;