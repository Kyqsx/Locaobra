import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEnvelope, faCircleCheck, faCircleExclamation } from '@fortawesome/free-solid-svg-icons';
import api from '../../service/api';
import './auth.css';
import loginImg from '../../assets/AuthImage.png';

/**
 * Fluxo de verificação de email:
 *  - /verificar-email?email=xxx  → tela "confira sua caixa de entrada" (vinda do signup)
 *  - /verificar-email?token=xxx  → confirma automaticamente o token do link do email
 */
const VerificarEmail = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const emailParam = searchParams.get('email');
    const tokenParam = searchParams.get('token');

    const [email, setEmail] = useState(emailParam || '');
    const [status, setStatus] = useState(tokenParam ? 'verificando' : 'aguardando');
    const [mensagem, setMensagem] = useState('');

    useEffect(() => {
        if (!tokenParam) return;
        api.get('/api/auth/verificar', { params: { token: tokenParam } })
            .then(() => setStatus('sucesso'))
            .catch((err) => {
                setStatus('erro');
                setMensagem(err.response?.data?.message || err.response?.data || 'Link inválido ou expirado.');
            });
    }, [tokenParam]);

    const reenviar = async () => {
        if (!email) {
            setMensagem('Informe o email cadastrado.');
            return;
        }
        try {
            await api.post('/api/auth/reenviar-verificacao', { email });
            setStatus('aguardando');
            setMensagem('Email reenviado! Confira sua caixa de entrada (e o spam).');
        } catch (err) {
            setMensagem(err.response?.data?.message || err.response?.data || 'Erro ao reenviar email.');
        }
    };

    const inputEmail = (
        <div className="loginInputGroup">
            <label className="loginInputLabel">Email cadastrado</label>
            <div className="inputWithIcon">
                <FontAwesomeIcon icon={faEnvelope} className="inputIcon" />
                <input
                    type="email"
                    placeholder="seu@email.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="loginInputField"
                />
            </div>
        </div>
    );

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="login-image">
                    <img src={loginImg} alt="Verificação de email" />
                </div>

                <div className="login-form">
                    <h2>Verificação de email</h2>

                    {status === 'verificando' && <p>Confirmando seu email...</p>}

                    {status === 'aguardando' && (
                        <>
                            <p>
                                {email
                                    ? <>Enviamos um link de confirmação para <strong>{email}</strong>.</>
                                    : 'Enviamos um link de confirmação para o seu email.'}
                            </p>
                            <p>Clique no link recebido para ativar sua conta.</p>
                            {inputEmail}
                            <button type="button" className="login-button" onClick={reenviar}>
                                Reenviar email de verificação
                            </button>
                        </>
                    )}

                    {status === 'sucesso' && (
                        <>
                            <p style={{ color: '#3ba62f' }}>
                                <FontAwesomeIcon icon={faCircleCheck} /> Email verificado com sucesso!
                            </p>
                            <button type="button" className="login-button" onClick={() => navigate('/login')}>
                                Ir para o login
                            </button>
                        </>
                    )}

                    {status === 'erro' && (
                        <>
                            <p style={{ color: '#D73F40' }}>
                                <FontAwesomeIcon icon={faCircleExclamation} /> {mensagem || 'Não foi possível verificar seu email.'}
                            </p>
                            {inputEmail}
                            <button type="button" className="login-button" onClick={reenviar}>
                                Reenviar email de verificação
                            </button>
                        </>
                    )}

                    {mensagem && status === 'aguardando' && <p>{mensagem}</p>}

                    <div className="login-footer">
                        <p><a href="/login">Voltar ao login</a></p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default VerificarEmail;
