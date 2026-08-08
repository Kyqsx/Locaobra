import { BrowserRouter, Routes, Route, Outlet, Navigate, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from './utils/useAuth';
import { canAccessAdminRoute } from './utils/permissions';

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
    if (!isAuthenticated || !canAccessAdminRoute(user, location.pathname)) {
        return <Navigate to="/" replace />;
    }
    return children;
}

function RotasApp() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/signup" element={<Signup />} />

                    <Route element={<ProtectedHandler><Outlet /></ProtectedHandler>}>
                        
                        <Route element={<SiteLayout />}>
                            <Route path="/" element={<Home />} />
                            <Route path="/catalogo/:slug" element={<Catalogo />} />
                            <Route path="/productview" element={<ProductView />} />
                            <Route path="/productview/:id" element={<ProductView />} />
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
                        </Route>

                    </Route>

                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default RotasApp;