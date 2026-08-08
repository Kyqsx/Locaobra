import React, { createContext, useContext, useState, useEffect } from 'react';
import api from 'axios';

const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

const isTokenExpired = (token) => {
    try {
        const tokenParts = token.split('.');
        if (tokenParts.length !== 3) return true;
        const payload = JSON.parse(atob(tokenParts[1]));
        const currentTime = Math.floor(Date.now() / 1000);
        return typeof payload.exp === 'number' && payload.exp < currentTime;
    } catch (e) {
        return true;
    }
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const restoreSession = () => {
            try {
                const token = localStorage.getItem('token');
                if (!token || isTokenExpired(token)) {
                    localStorage.removeItem('token');
                    setUser(null);
                    setLoading(false);
                    return;
                }

                const payload = JSON.parse(atob(token.split('.')[1]));
                const userData = {
                    id: payload.id,
                    email: payload.sub,
                    tipo: payload.tipo,
                };

                setUser(userData);
                api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            } catch (error) {
                console.error("Erro ao restaurar sessão:", error);
                localStorage.removeItem('token');
                setUser(null);
            } finally {
                setLoading(false);
            }
        };

        restoreSession();
    }, []);

    const login = (userData, token) => {
        setUser(userData);
        localStorage.setItem('token', token);
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem('token');
        delete api.defaults.headers.common['Authorization'];
    };

    const value = {
        user,
        userId: user?.id || null,
        isAuthenticated: !!user,
        isCliente: user?.tipo === 'CLIENTE',
        isFuncionario: user?.tipo === 'FUNCIONARIO',
        loading,
        login,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};