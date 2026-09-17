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
                        const perfilResponse = await api.get('/api/clientes/perfil');
                        const perfil = perfilResponse.data;

                        userData.nome = perfil.nome || userData.nome;
                        userData.id_cliente = perfil.id; // ID da tabela de clientes, se for diferente do user_id
                        userData.enderecos = perfil.enderecos || [];
                    } catch (err) {
                        console.warn("⚠️ Perfil detalhado não encontrado. Usando dados básicos da conta.");
                    }
                }

                setUser(userData);
                return userData;
            } else {
                logout();
                return null;
            }
        } catch (error) {
            console.error("Sessão inválida ou expirada");
            logout();
            return null;
        } finally {
            setLoading(false); // SÓ FINALIZA O LOADING AQUI
        }
    };

    const login = async (loginEmail, token) => {
        console.log("Iniciando persistência de login para:", loginEmail);

        localStorage.setItem('token', token);
        localStorage.setItem('userEmail', loginEmail);

        api.defaults.headers.common['Authorization'] = `Bearer ${token}`;

        // Importante: não montamos o "user" só com o que veio da tela de login
        // (id/nome/tipo) — isso deixava cargoFuncionario indefinido até o
        // próximo reload da página, e as rotas /admin/* que dependem do cargo
        // (ex.: entregador, conferente) bloqueavam o acesso logo após o login.
        // Buscamos a sessão completa (com cargoFuncionario) antes de continuar,
        // e devolvemos o usuário pra quem chamou decidir o redirecionamento.
        setLoading(true);
        return await checkSession();
    };

    const logout = () => {
        console.log("Encerrando sessão...");
        localStorage.removeItem('token');
        localStorage.removeItem('userEmail');
        delete api.defaults.headers.common['Authorization'];
        setUser(null);
        setLoading(false);
    };

    // Recarrega só a lista de endereços do cliente logado (ex.: depois de
    // adicionar/editar/remover um endereço na tela "Meus Endereços"), sem
    // precisar re-buscar a sessão inteira.
    const recarregarEnderecos = async () => {
        if (!user || user.tipo !== 'CLIENTE') return;
        try {
            const response = await api.get('/api/clientes/meus-enderecos');
            setUser(prev => (prev ? { ...prev, enderecos: response.data } : prev));
        } catch (err) {
            console.warn('⚠️ Não foi possível recarregar os endereços.');
        }
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
        recarregarEnderecos,
        loading,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}