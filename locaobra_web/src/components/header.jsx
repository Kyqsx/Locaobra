// src/components/Header.js
import React from 'react';
import { Link } from 'react-router-dom';
import './components.css';
import { useState } from 'react';
import { useAuth } from '../utils/useAuth';
import { useCart } from '../context/CartContext';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faRightFromBracket, faUser, faShield, faNewspaper, faList, faClipboardList, faCartShopping, faLocationDot } from '@fortawesome/free-solid-svg-icons';
import logo from '../assets/locaobraLogo.png';

function Header() {
    const { user, logout } = useAuth();
    const { totalItens } = useCart();

    const handleLogout = () => {
        logout();
    };

    return (
        <header>
            <div className="container">

                <div className="logo">
                    <Link to="/">
                        <img src={logo} className="logo-isotipo" alt="logo" />
                    </Link>
                </div>

                <nav>
                    {/* ==================== MENU PARA FUNCIONARIO ==================== */}
                    {user && user.tipo === 'FUNCIONARIO' && (
                        <>
                        </>
                    )}

                    {/* ==================== MENU PARA CLIENTE ==================== */}
                    {(!user || ['CLIENTE', 'ADMIN', 'FUNCIONARIO'].includes(user?.tipo)) && (
                        <>
                            <Link to="/catalogo/acesso-elevacao" className='abas'>Acesso e Elevação</Link>
                            <Link to="/catalogo/concretagem" className='abas'>Concretagem</Link>
                            <Link to="/catalogo/ferramentas-eletricas" className='abas'>Ferramentas Elétricas</Link>
                        </>
                    )}

                </nav>
                <nav>
                    {/* ==================== CARRINHO ==================== */}
                    {user?.tipo === 'CLIENTE' && (
                        <Link to="/carrinho" className="abas carrinho-link" aria-label="Carrinho">
                            <FontAwesomeIcon icon={faCartShopping} />
                            {totalItens > 0 && <span className="carrinho-badge">{totalItens}</span>}
                        </Link>
                    )}

                    {/* ==================== PERFIL E LOGOUT ==================== */}
                    {user ? (
                        <div className="dropdown">
                            <button className="abas perfil-link">
                                <FontAwesomeIcon icon={faUser} /> {user.nome || 'Perfil'}
                            </button>

                            <div className="dropdown-content user-dropdown-content">
                                {(!user || ['ADMIN', 'FUNCIONARIO'].includes(user?.tipo)) && (
                                    <>
                                        <Link to="/admin" className='sub-item'>
                                            <FontAwesomeIcon icon={faShield} /> Painel
                                        </Link>

                                    </>
                                )}
                                {user?.tipo === 'CLIENTE' && (
                                    <>
                                        <Link to="/meus-pedidos" className="sub-item">
                                            <FontAwesomeIcon icon={faClipboardList} /> Meus Pedidos
                                        </Link>
                                        <Link to="/meus-enderecos" className="sub-item">
                                            <FontAwesomeIcon icon={faLocationDot} /> Meus Endereços
                                        </Link>
                                    </>
                                )}
                                <Link to="/perfil" className="sub-item">
                                    <FontAwesomeIcon icon={faUser} /> Ver Perfil
                                </Link>
                                <button onClick={handleLogout} className="sub-item logout-dropdown">
                                    <FontAwesomeIcon icon={faRightFromBracket} /> Sair
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <div>
                                <Link to="/login" className='abas login-btn'>Entrar ou Cadastrar-se</Link>
                            </div>
                        </>
                    )}
                </nav>
            </div>
        </header>
    );
}

export default Header;
