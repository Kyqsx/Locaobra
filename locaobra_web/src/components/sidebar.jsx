import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import './components.css';
import { useAuth } from '../utils/useAuth';
import { canAccessAdminRoute } from '../utils/permissions';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faRightFromBracket,
  faChartPie,
  faTools,
  faUsers,
  faUserTie,
  faTruck,
  faWrench,
  faBell,
  faArrowLeft,
  faBriefcase,
  faBuilding,
  faChevronDown,
  faChevronRight,
} from '@fortawesome/free-solid-svg-icons';

const Sidebar = () => {
  const { logout, user } = useAuth();
  const [openGroups, setOpenGroups] = useState({
    cadastros: false,
    operacoes: false,
  });

  const canAccess = (path) => canAccessAdminRoute(user, path);

  const toggleGroup = (group) => {
    setOpenGroups((prev) => ({
      ...prev,
      [group]: !prev[group],
    }));
  };

  const menuGroups = [
    {
      id: 'cadastros',
      label: 'Cadastros',
      icon: faBuilding,
      items: [
        { to: '/admin/equipamentos', icon: faTools, label: 'Equipamentos' },
        { to: '/admin/clientes', icon: faUsers, label: 'Clientes' },
        { to: '/admin/funcionarios', icon: faUserTie, label: 'Funcionários' },
        { to: '/admin/cargos', icon: faBriefcase, label: 'Cargos' },
        { to: '/admin/departamentos', icon: faBuilding, label: 'Departamentos' },
      ],
    },
    {
      id: 'operacoes',
      label: 'Operações',
      icon: faWrench,
      items: [
        { to: '/admin/expedicao', icon: faTruck, label: 'Expedição' },
        { to: '/admin/ordens-servico', icon: faWrench, label: 'Ordens de Serviço' },
        { to: '/admin/notificacoes', icon: faBell, label: 'Notificações de Avarias' },
      ],
    },
  ];

  const visibleGroups = menuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => canAccess(item.to)),
    }))
    .filter((group) => group.items.length > 0);

  return (
    <aside className="admin-sidebar">
      <div className="sidebar-container">
        <div className="sidebar-header">
          <Link to="/" className="sidebar-logo">
            <span className="loca">LOCA</span><span className="obra">OBRA</span>
          </Link>
          <p className="sidebar-subtitle">Painel de Controle</p>
        </div>

        <nav className="sidebar-nav user-dropdown-content" style={{ display: 'block', position: 'static', boxShadow: 'none', opacity: 1 }}>
          {canAccess('/admin') && (
            <Link to="/admin" className="sub-item sidebar-plain-item dashboard-item">
              <FontAwesomeIcon icon={faChartPie} /> Dashboard
            </Link>
          )}

          {visibleGroups.map((group) => (
            <div key={group.id} className={`sidebar-group ${openGroups[group.id] ? 'open' : ''}`}>
              <button
                type="button"
                className="sidebar-group-header"
                onClick={() => toggleGroup(group.id)}
              >
                <span>
                  <FontAwesomeIcon icon={group.icon} />
                  {group.label}
                </span>
                <FontAwesomeIcon icon={openGroups[group.id] ? faChevronDown : faChevronRight} />
              </button>

              {openGroups[group.id] && (
                <div className="sidebar-group-content">
                  {group.items.map((item) => (
                    <Link key={item.to} to={item.to} className="sub-item">
                      <FontAwesomeIcon icon={item.icon} /> {item.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}

          <div className="sidebar-divider"></div>

          <Link to="/" className="sub-item sidebar-plain-item">
            <FontAwesomeIcon icon={faArrowLeft} /> Voltar ao Site
          </Link>

          <button className="sub-item sidebar-plain-item logout-dropdown" onClick={logout}>
            <FontAwesomeIcon icon={faRightFromBracket} /> Sair
          </button>
        </nav>
      </div>
    </aside>
  );
};

export default Sidebar;