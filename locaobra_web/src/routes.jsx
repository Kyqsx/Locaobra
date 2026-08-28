import { BrowserRouter, Routes, Route, Outlet, Navigate, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from './utils/useAuth';
import { CartProvider } from './context/CartContext';
import { canAccessAdminRoute, getDefaultAdminPath } from './utils/permissions';

import Home from './pages/Home/index';
import Login from "./pages/Auth/login";
import Signup from './pages/Auth/signup';
import Catalogo from './pages/Catalogo/catalogo';
import ProductView from "./pages/ProductView/productview";
import AdminDashboard from './pages/Admin/dashboard'; 
import AdminEquipamentos from './pages/Admin/equipamentos';
import AdminClientes from './pages/Admin/clientes';
import AdminFuncionarios from './pages/Admin/funcionarios';
import AdminExpedicao from './pages/Admin/expedicao';
import AdminOrdensServico from './pages/Admin/ordensServico';
import AdminNotificacoes from './pages/Admin/notificacoes';
import AdminCargos from './pages/Admin/Cargos';
import AdminDepartamentos from './pages/Admin/Departamentos';
import AdminDepositos from './pages/Admin/Depositos';
import AdminPedidos from './pages/Admin/pedidos';
import MeusPedidos from './pages/Pedidos/meusPedidos';
import Carrinho from './pages/Cart/carrinho';
import MeusEnderecos from './pages/Perfil/meusEnderecos';

import Header from "./components/header";
import Sidebar from "./components/sidebar";

function SiteLayout() {
    return (
        <>
            <Header />
            <main style={{ minHeight: '80vh' }}>
                <Outlet />
            </main>
        </>
    );
}

function AdminLayout() {
    return (
        <div style={{ display: 'flex', minHeight: '100vh' }}>
            <Sidebar />
            <main style={{ flex: 1, padding: '20px', backgroundColor: '#f4f7f6' }}>
                <Outlet />
            </main>
        </div>
    );
}

function ProtectedHandler({ children }) {
    const { loading } = useAuth();
    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <p>Carregando sessão...</p> 
            </div>
        );
    }
    return children;
}

function AdminRoute({ children }) {
    const { user, isAuthenticated, loading } = useAuth();
    const location = useLocation();

    if (loading) return null;
    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }
    if (!canAccessAdminRoute(user, location.pathname)) {
        // Se a pessoa não pode ver essa página específica (ex.: dashboard),
        // manda pra primeira página que faz sentido pro cargo dela, em vez
        // de simplesmente expulsar do painel administrativo.
        const fallback = getDefaultAdminPath(user);
        if (fallback && fallback !== location.pathname) {
            return <Navigate to={fallback} replace />;
        }
        return <Navigate to="/" replace />;
    }
    return children;
}

function ClienteRoute({ children }) {
    const { user, loading } = useAuth();
    if (loading) return null;
    if (!user) {
        return <Navigate to="/login" replace />;
    }
    return children;
}

function RotasApp() {
    return (
        <AuthProvider>
            <CartProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/signup" element={<Signup />} />

                    <Route element={<ProtectedHandler><Outlet /></ProtectedHandler>}>
                        
                        <Route element={<SiteLayout />}>
                            <Route path="/" element={<Home />} />
                            <Route path="/catalogo" element={<Catalogo />} />
                            <Route path="/catalogo/:slug" element={<Catalogo />} />
                            <Route path="/productview" element={<ProductView />} />
                            <Route path="/productview/:id" element={<ProductView />} />
                            <Route path="/meus-pedidos" element={<ClienteRoute><MeusPedidos /></ClienteRoute>} />
                            <Route path="/meus-enderecos" element={<ClienteRoute><MeusEnderecos /></ClienteRoute>} />
                            <Route path="/carrinho" element={<ClienteRoute><Carrinho /></ClienteRoute>} />
                        </Route>

                        <Route path="/admin" element={
                            <AdminRoute>
                                <AdminLayout />
                            </AdminRoute>
                        }>
                            <Route index element={<AdminDashboard />} />
                            <Route path="equipamentos" element={<AdminEquipamentos />} />  
                            <Route path="clientes" element={<AdminClientes />} /> 
                            <Route path="funcionarios" element={<AdminFuncionarios />} />
                            <Route path="expedicao" element={<AdminExpedicao />} />
                            <Route path="ordens-servico" element={<AdminOrdensServico />} />
                            <Route path="notificacoes" element={<AdminNotificacoes />} />
                            <Route path="cargos" element={<AdminCargos />} />
                            <Route path="departamentos" element={<AdminDepartamentos />} />
                            <Route path="depositos" element={<AdminDepositos />} />
                            <Route path="pedidos" element={<AdminPedidos />} />
                        </Route>

                    </Route>

                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </BrowserRouter>
            </CartProvider>
        </AuthProvider>
    );
}

export default RotasApp;