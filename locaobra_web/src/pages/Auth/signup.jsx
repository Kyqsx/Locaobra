import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../utils/useAuth';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faEyeSlash, faUser, faEnvelope, faLock, faHardHat, faRocket } from '@fortawesome/free-solid-svg-icons';
import api from '../../service/api';
import './auth.css';
import loginImg from '../../assets/AuthImage.png';

const Signup = () => {
    const { login } = useAuth();
    const navigate = useNavigate();

    const [nome, setNome] = useState('');
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    // Força da senha
    const [powerPointWidth, setPowerPointWidth] = useState('1%');
    const [powerPointColor, setPowerPointColor] = useState('#D73F40');

    const [showError, setShowError] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    const passwordStrength = (value) => {
        let point = 0;
        const widthPower = ['1%', '25%', '50%', '75%', '100%'];
        const colorPower = ['#D73F40', '#DC6551', '#F2B84F', '#BDE952', '#3ba62f'];

        if (value.length >= 6) {
            const tests = [/[0-9]/, /[a-z]/, /[A-Z]/, /[^0-9a-zA-Z]/];
            tests.forEach((test) => { if (test.test(value)) point += 1; });
        }
        setPowerPointWidth(widthPower[point]);
        setPowerPointColor(colorPower[point]);
    };

    const handlePasswordChange = (e) => {
        const value = e.target.value;
        setSenha(value);
        passwordStrength(value);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        console.log("Iniciando processo de cadastro...");

        if (senha !== confirmPassword) {
            setErrorMessage('As senhas não coincidem');
            setShowError(true);
            return;
        }

        if (senha.length < 6) {
            setErrorMessage('A senha deve ter pelo menos 6 caracteres');
            setShowError(true);
            return;
        }

        // Montando o objeto para debug
        const payload = {
            nome,
            email,
            senha,
            tipo: 'CLIENTE'
        };

        console.log("Enviando dados para o servidor:", { ...payload, senha: "[PROTEGIDO]" });

        try {
            const response = await api.post('/api/auth/signup', payload);
            console.log("Resposta do servidor (Sucesso):", response.data);

            if (response.data.token || response.status === 201) {
                // Se o backend retornar o token no signup (login automático)
                if (response.data.token) {
                    const { token, email: e_resp } = response.data;
                    await login(e_resp || email, token);
                }

                console.log("Cadastro concluído! Redirecionando...");
                navigate('/');
            }
        } catch (error) {
            console.error("Erro detalhado na requisição:", error);
            
            // CORREÇÃO CRÍTICA: Extraindo apenas a mensagem de texto
            // Se error.response.data for um objeto {status, message, timestamp}, pegamos só o .message
            let msgFim = "Erro ao cadastrar. Verifique os dados e tente novamente.";
            
            if (error.response && error.response.data) {
                if (typeof error.response.data === 'string') {
                    msgFim = error.response.data;
                } else if (error.response.data.message) {
                    msgFim = error.response.data.message;
                }
                console.log("Mensagem de erro do Backend:", msgFim);
            }

            setErrorMessage(msgFim);
            setShowError(true);
        }
    };

    return (
        <div className="loginSplitPage">
            {/* Metade Esquerda: Imagem Identica ao Login */}
            <div className="loginImageSide" style={{ backgroundImage: `url(${loginImg})` }}>
                <div className="imageOverlay">
                    <div className="brandLogo">
                        <FontAwesomeIcon icon={faHardHat} /> <span>LocaObra</span>
                    </div>
                    <h1>Construa seus projetos com os melhores equipamentos.</h1>
                </div>
            </div>

            {/* Metade Direita: Formulário */}
            <div className="loginFormSide">
                <div className="login-card">
                    <h2 className="loginCardTitle">Crie sua conta! <FontAwesomeIcon icon={faRocket} /></h2>
                    <p>Faça seu cadastro para começar a alugar.</p>

                    <form className="login-form" onSubmit={handleSubmit}>

                        <div className="loginInputGroup">
                            <label className="loginInputLabel">Nome Completo</label>
                            <div className="inputWithIcon">
                                <FontAwesomeIcon icon={faUser} className="inputIcon" />
                                <input
                                    type="text"
                                    placeholder="Seu nome"
                                    value={nome}
                                    onChange={(e) => setNome(e.target.value)}
                                    required
                                    className="loginInputField"
                                />
                            </div>
                        </div>

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
                                        placeholder="Mínimo 6 caracteres"
                                        value={senha}
                                        onChange={handlePasswordChange}
                                        required
                                        className="loginInputField"
                                    />
                                    <button type="button" className="loginEye-button" onClick={() => setShowPassword(!showPassword)}>
                                        <FontAwesomeIcon icon={showPassword ? faEye : faEyeSlash} />
                                    </button>
                                </div>
                            </div>
                            {/* Barra de Força da Senha estilizada via CSS do login */}
                            <div className="power-container">
                                <div className="power-point" style={{ width: powerPointWidth, backgroundColor: powerPointColor }}></div>
                            </div>
                        </div>

                        <div className="loginInputGroup">
                            <label className="loginInputLabel">Confirmar Senha</label>
                            <div className="inputWithIcon">
                                <FontAwesomeIcon icon={faLock} className="inputIcon" />
                                <div className="loginPasswordWrapper">
                                    <input
                                        type={showConfirmPassword ? 'text' : 'password'}
                                        placeholder="Repita a senha"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                        className="loginInputField"
                                    />
                                    <button type="button" className="loginEye-button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                                        <FontAwesomeIcon icon={showConfirmPassword ? faEye : faEyeSlash} />
                                    </button>
                                </div>
                            </div>
                        </div>

                        <button type="submit" className="login-button">Criar conta</button>
                    </form>

                    <div className="login-footer">
                        <p>Já possui conta? <a href="/login">Faça Login</a></p>
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

export default Signup;