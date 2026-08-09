import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './components.css';
import { useAuth } from '../utils/useAuth';
import { canAccessAdminRoute } from '../utils/permissions';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faRightFromBracket, 
  faChartPie, 
  faTools, 
  faClipboardList, 
  faUsers,
  faUserTie,
  faTruck,
  faWrench,
  faBell,
  faArrowLeft 
} from '@fortawesome/free-solid-svg-icons';

const Sidebar = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const canAccess = (path) => canAccessAdminRoute(user, path);

  return (
    <aside className="admin-sidebar">
      <div className="sidebar-container">
        {/* Logo / Título */}
        <div className="sidebar-header">
          <Link to="/" className="sidebar-logo">
             <span className="loca">LOCA</span><span className="obra">OBRA</span>
          </Link>
          <p className="sidebar-subtitle">Painel de Controle</p>
        </div>

        {/* Links de Navegação - Usando os estilos do Dropdown */}
        <nav className="sidebar-nav user-dropdown-content" style={{ display: 'block', position: 'static', boxShadow: 'none', opacity: 1 }}>
          {canAccess('/admin') && (
            <Link to="/admin" className="sub-item">
              <FontAwesomeIcon icon={faChartPie} /> Dashboard
            </Link>
          )}
          {canAccess('/admin/equipamentos') && (
            <Link to="/admin/equipamentos" className="sub-item">
              <FontAwesomeIcon icon={faTools} /> Equipamentos
            </Link>
          )}
          {canAccess('/admin/pedidos') && (
            <Link to="/admin/pedidos" className="sub-item">
              <FontAwesomeIcon icon={faClipboardList} /> Pedidos
            </Link>
          )}
          {canAccess('/admin/clientes') && (
            <Link to="/admin/clientes" className="sub-item">
              <FontAwesomeIcon icon={faUsers} /> Clientes
            </Link>
          )}
          {canAccess('/admin/funcionarios') && (
            <Link to="/admin/funcionarios" className="sub-item">
              <FontAwesomeIcon icon={faUserTie} /> Funcionários
            </Link>
          )}
          {canAccess('/admin/expedicao') && (
            <Link to="/admin/expedicao" className="sub-item">
              <FontAwesomeIcon icon={faTruck} /> Expedição
            </Link>
          )}
          {canAccess('/admin/ordens-servico') && (
            <Link to="/admin/ordens-servico" className="sub-item">
              <FontAwesomeIcon icon={faWrench} /> Ordens de Serviço
            </Link>
          )}
          {canAccess('/admin/notificacoes') && (
            <Link to="/admin/notificacoes" className="sub-item">
              <FontAwesomeIcon icon={faBell} /> Notificações de Avarias
            </Link>
          )}

          <div className="sidebar-divider"></div>

          <Link to="/" className="sub-item">
            <FontAwesomeIcon icon={faArrowLeft} /> Voltar ao Site
          </Link>
          
          <button className="sub-item logout-dropdown" onClick={logout}>
            <FontAwesomeIcon icon={faRightFromBracket} /> Sair
          </button>
        </nav>
      </div>
    </aside>
  );
};

export default Sidebar;