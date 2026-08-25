import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faUsers, faToolbox, faDollarSign, faFileAlt, faFilter, faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import './AdminDashboard.css';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

const AdminDashboard = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState(null);
  const [equipamentos, setEquipamentos] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [dashRes, equipRes] = await Promise.all([
          api.get('/api/dashboard'),
          api.get('/api/equipamentos?apenasAtivos=true'),
        ]);

        setDashboardData(dashRes.data);
        setEquipamentos(Array.isArray(equipRes.data) ? equipRes.data : []);
      } catch (err) {
        console.error('Erro ao carregar dados do dashboard', err);
      }
    };

    fetchData();
  }, []);

  if (!canAccessAdminRoute(user, '/admin')) {
    return <Navigate to="/" replace />;
  }

  const imageUrl = (path) => {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
  };


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
                          <p className="statValue">{dashboardData.totalClientes ?? 0}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#D9820F' }}>
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

              {/* Charts Section */}
              <div className="chartsSection">
                <div className="chartCard">
                  <div className="chartHeader">
                    <h3>Receita Mensal</h3>
                    <button className="chartMenu">
                      <FontAwesomeIcon icon={faEllipsisV} />
                    </button>
                  </div>
                  <div className="chartPlaceholder">
                    <div className="fakeChart">
                      <div className="chartBar" style={{ height: '60%' }}></div>
                      <div className="chartBar" style={{ height: '75%' }}></div>
                      <div className="chartBar" style={{ height: '45%' }}></div>
                      <div className="chartBar" style={{ height: '90%' }}></div>
                      <div className="chartBar" style={{ height: '70%' }}></div>
                    </div>
                  </div>
                </div>

                <div className="chartCard">
                  <div className="chartHeader">
                    <h3>Distribuição por Tipo</h3>
                    <button className="chartMenu">
                      <FontAwesomeIcon icon={faEllipsisV} />
                    </button>
                  </div>
                  <div className="pieChartPlaceholder">
                    <svg viewBox="0 0 100 100">
                      <circle cx="50" cy="50" r="45" fill="none" stroke="#D9820F" strokeWidth="15" strokeDasharray="70 314" />
                      <circle cx="50" cy="50" r="45" fill="none" stroke="#4CAF50" strokeWidth="15" strokeDasharray="120 314" strokeDashoffset="-70" />
                      <circle cx="50" cy="50" r="45" fill="none" stroke="#2196F3" strokeWidth="15" strokeDasharray="124 314" strokeDashoffset="-190" />
                    </svg>
                    <div className="pieLabels">
                      <div className="pieLabel"><span className="pieLegend" style={{ background: '#D9820F' }}></span> Clientes</div>
                      <div className="pieLabel"><span className="pieLegend" style={{ background: '#4CAF50' }}></span> Fornecedores</div>
                      <div className="pieLabel"><span className="pieLegend" style={{ background: '#2196F3' }}></span> Admins</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Equipamentos ativos */}
              <div className="recentUsersSection">
                <div className="sectionHeader">
                  <h3><FontAwesomeIcon icon={faToolbox} /> Equipamentos Ativos</h3>
                  <button className="filterBtn">
                    <FontAwesomeIcon icon={faFilter} /> Filtrar
                  </button>
                </div>

                <div className="equipGrid">
                  {equipamentos.length > 0 ? (
                    equipamentos.slice(0, 8).map(eq => (
                      <div key={eq.id} className="equipCard">
                        <div className="equipImage">
                          {eq.imagens && eq.imagens.length > 0 ? (
                            <img src={imageUrl(eq.imagens[0])} alt={eq.nome} />
                          ) : (
                            <FontAwesomeIcon icon={faToolbox} />
                          )}
                        </div>
                        <div className="equipInfo">
                          <h4>{eq.nome || eq.descricao || 'Equipamento'}</h4>
                          <p className="equipStatus">{eq.ativo === false ? 'Indisponível' : 'Disponível'}</p>
                          <p className="equipPrice">{eq.precoPadrao ? `R$ ${eq.precoPadrao}/dia` : ''}</p>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="emptyState">Nenhum equipamento ativo.</div>
                  )}
                </div>
              </div>
            </div>

    </div>
  );
};

export default AdminDashboard;