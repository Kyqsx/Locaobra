import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faUsers, faToolbox, faDollarSign, faFileAlt } from '@fortawesome/free-solid-svg-icons';
import './AdminDashboard.css';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

const AdminDashboard = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState(null);

  useEffect(() => {
    api.get('/api/dashboard')
      .then(response => setDashboardData(response.data))
      .catch(err => console.error('Erro ao carregar dados do dashboard', err));
  }, []);

  if (!canAccessAdminRoute(user, '/admin')) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="adminContent">
      <div className="viewHeader">
        <h2 className="pageTitle">
          <FontAwesomeIcon icon={faChartLine} /> Dashboard
        </h2>
        <div className="headerRight" />
      </div>

      <div className="dashboardView">
          {/* Stats Grid */}
          <div className="statsGrid">
                {dashboardData ? (
                  <>
                    <div className="statCard">
                      <div className="statCardTop">
                        <div className="statInfo">
                          <p className="statTitle">Usuários</p>
                          <p className="statValue">{dashboardData.totalUsuarios ?? 0}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#D9820F' }}>
                          <FontAwesomeIcon icon={faUsers} />
                        </div>
                      </div>
                    </div>

                    <div className="statCard">
                      <div className="statCardTop">
                        <div className="statInfo">
                          <p className="statTitle">Clientes</p>
                          <p className="statValue">{dashboardData.totalClientes ?? 0}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#4CAF50' }}>
                          <FontAwesomeIcon icon={faUsers} />
                        </div>
                      </div>
                    </div>

                    <div className="statCard">
                      <div className="statCardTop">
                        <div className="statInfo">
                          <p className="statTitle">Equipamentos</p>
                          <p className="statValue">{dashboardData.totalEquipamentos ?? 0}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#4CAF50' }}>
                          <FontAwesomeIcon icon={faToolbox} />
                        </div>
                      </div>
                    </div>

                    <div className="statCard">
                      <div className="statCardTop">
                        <div className="statInfo">
                          <p className="statTitle">Receita</p>
                          <p className="statValue">{dashboardData.receitaTotal ? `R$ ${dashboardData.receitaTotal}` : 'R$ 0'}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#2196F3' }}>
                          <FontAwesomeIcon icon={faDollarSign} />
                        </div>
                      </div>
                    </div>

                    <div className="statCard">
                      <div className="statCardTop">
                        <div className="statInfo">
                          <p className="statTitle">Aluguéis Ativos</p>
                          <p className="statValue">{dashboardData.alugueisAtivos ?? 0}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#9C27B0' }}>
                          <FontAwesomeIcon icon={faFileAlt} />
                        </div>
                      </div>
                    </div>
                  </>
                ) : (
                  <div>Carregando estatísticas...</div>
                )}
              </div>

            </div>

    </div>
  );
};

export default AdminDashboard;