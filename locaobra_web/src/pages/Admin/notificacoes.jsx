import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBell, faCheckDouble, faExclamationTriangle, faList } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

function formatDate(dateStr) {
    if (!dateStr) return '---';
    const d = new Date(dateStr);
    return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export default function Notificacoes() {
    const { user } = useAuth();
    const [notificacoes, setNotificacoes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);

    const destinatario = 'ANALISTA_FINANCEIRO';

    function fetchList() {
        setLoading(true);
        api.get('/api/notificacoes', { params: { destinatario } })
            .then(response => setNotificacoes(response.data || []))
            .catch(err => setMessage({ type: 'error', text: 'Erro ao buscar: ' + (err.response?.data || err.message) }))
            .finally(() => setLoading(false));
    }

    useEffect(() => {
        fetchList();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    if (!canAccessAdminRoute(user, '/admin/notificacoes')) {
        return <Navigate to="/admin" replace />;
    }

    function handleMarcarLida(id) {
        api.patch(`/api/notificacoes/${id}/lida`)
            .then(() => fetchList())
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    function handleMarcarTodasLidas() {
        api.patch('/api/notificacoes/marcar-todas-lidas', { destinatario })
            .then(() => fetchList())
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    const naoLidas = notificacoes.filter(n => !n.lida).length;

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">
                    <FontAwesomeIcon icon={faBell} /> Notificações de Avarias
                </h2>
                <div className="headerRight">
                    {naoLidas > 0 && (
                        <button className="addBtn" onClick={handleMarcarTodasLidas}>
                            <FontAwesomeIcon icon={faCheckDouble} /> Marcar todas como lidas
                        </button>
                    )}
                </div>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
            )}

            <div className="settingsCard">
                <h3>
                    <FontAwesomeIcon icon={faList} /> Avarias registradas pelo Conferente
                    {naoLidas > 0 && <span className="notificationBadge" style={{ marginLeft: '10px' }}>{naoLidas} novas</span>}
                </h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Quando o Conferente registra avarias ou peças faltantes na devolução de um equipamento,
                    uma notificação é gerada automaticamente aqui para cobrança do cliente.
                </p>
            </div>

            <div className="recentUsersSection">
                <div className="tableWrapper">
                    <table className="usersTable">
                        <thead>
                            <tr>
                                <th>Status</th>
                                <th>Título</th>
                                <th>Mensagem</th>
                                <th>Data</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && notificacoes.map(n => (
                                <tr key={n.id} className="tableRow" style={{ opacity: n.lida ? 0.6 : 1 }}>
                                    <td>
                                        {n.lida ? (
                                            <span className="typeTag concluido">Lida</span>
                                        ) : (
                                            <span className="typeTag pendente">
                                                <FontAwesomeIcon icon={faExclamationTriangle} /> Nova
                                            </span>
                                        )}
                                    </td>
                                    <td className="nameCell">
                                        <div className="userCell">
                                            <div className="userCellAvatar" style={{ background: '#D9820F' }}>
                                                <FontAwesomeIcon icon={faExclamationTriangle} />
                                            </div>
                                            <div>
                                                <div className="userName">{n.titulo}</div>
                                                <div className="userRole">{n.tipo}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>{n.mensagem}</td>
                                    <td className="dateCell">{formatDate(n.criadaEm)}</td>
                                    <td className="actionsCell">
                                        {!n.lida && (
                                            <button className="actionBtn view" onClick={() => handleMarcarLida(n.id)} title="Marcar como lida">
                                                <FontAwesomeIcon icon={faCheckDouble} />
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {!loading && notificacoes.length === 0 && (
                                <tr className="tableRow">
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#999' }}>
                                        Nenhuma notificação de avaria registrada.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}