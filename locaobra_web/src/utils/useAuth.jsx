import { useState, useEffect, useContext, createContext } from 'react';
import api from '../service/api';

const AuthContext = createContext();

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (token) {
            // Configura o cabeçalho IMEDIATAMENTE antes de chamar a sessão
            api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            checkSession();
        } else {
            setLoading(false);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const checkSession = async () => {
        try {
            // Se o token estiver expirado, o JwtService no Java vai retornar 401/403
            // e o catch(error) vai disparar o logout.
            const response = await api.get('/api/auth/me');
            const sessionData = response.data;

            // Verifique se o seu backend retorna o campo 'login' (subject do JWT)
            if (sessionData && (sessionData.id || sessionData.login)) {
                const userEmail = sessionData.login || sessionData.email;

                let userData = {
                    id: sessionData.id,
                    nome: sessionData.nome || userEmail.split('@')[0],
                    email: userEmail,
                    tipo: sessionData.tipo,
                    idFuncionario: sessionData.idFuncionario || null,
                    cargoFuncionario: sessionData.cargoFuncionario || null,
                };

                // Busca dados de cliente se necessário...
                if (userData.tipo === "CLIENTE") {
                    try {
                        // Busca o perfil pelo email ou ID
                        const encodedEmail = encodeURIComponent(userEmail);
                        const perfilResponse = await api.get(`/api/v1/clientes/perfil?email=${encodedEmail}`);
                        const perfil = perfilResponse.data;

                        userData.nome = perfil.nome || userData.nome;
                        userData.id_cliente = perfil.id; // ID da tabela de clientes, se for diferente do user_id
                    } catch (err) {
                        console.warn("⚠️ Perfil detalhado não encontrado. Usando dados básicos da conta.");
                    }
                }

                setUser(userData);
            } else {
                logout();
            }
        } catch (error) {
            console.error("Sessão inválida ou expirada");
            logout();
        } finally {
            setLoading(false); // SÓ FINALIZA O LOADING AQUI
        }
    };

    const login = (loginEmail, token, userDataFromLogin = {}) => {
        console.log("Iniciando persistência de login para:", loginEmail);

        localStorage.setItem('token', token);
        localStorage.setItem('userEmail', loginEmail);

        api.defaults.headers.common['Authorization'] = `Bearer ${token}`;

        const userToSet = {
            id: userDataFromLogin.id || null,
            nome: userDataFromLogin.nome || loginEmail.split('@')[0],
            email: loginEmail,
            tipo: userDataFromLogin.tipo || "CLIENTE",
        };

        setUser(userToSet);
        setLoading(false);
    };

    const logout = () => {
        console.log("Encerrando sessão...");
        localStorage.removeItem('token');
        localStorage.removeItem('userEmail');
        delete api.defaults.headers.common['Authorization'];
        setUser(null);
        setLoading(false);
    };

    const value = {
        user,
        userId: user?.id,
        userEmail: user?.email,
        isAuthenticated: !!user,
        isCliente: user?.tipo === 'CLIENTE',
        isFuncionario: user?.tipo === 'FUNCIONARIO',
        isAdmin: user?.tipo === 'ADMIN',
        login,
        logout,
        loading,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}