import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import './Pedidos.css';

const STATUS_INFO = {
  SOLICITADO: { label: 'Aguardando revisão', className: 'status-solicitado' },
  CONFIRMADO: { label: 'Em análise de crédito', className: 'status-confirmado' },
  APROVADO: { label: 'Aprovado', className: 'status-aprovado' },
  RECUSADO: { label: 'Recusado', className: 'status-recusado' },
  REPROVADO: { label: 'Crédito reprovado', className: 'status-recusado' },
  CANCELADO: { label: 'Cancelado', className: 'status-cancelado' },
};

const formatarData = (iso) => {
  if (!iso) return '—';
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
};

function MeusPedidos() {
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelandoId, setCancelandoId] = useState(null);

  const carregar = () => {
    setLoading(true);
    setError(null);
    api.get('/api/pedidos/meus')
      .then((res) => setPedidos(res.data))
      .catch(() => setError('Não foi possível carregar seus pedidos.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    carregar();
  }, []);

  const podeCancelar = (status) => status === 'SOLICITADO' || status === 'CONFIRMADO';

  const handleCancelar = async (id) => {
    if (!window.confirm('Tem certeza que deseja cancelar esse pedido?')) return;
    setCancelandoId(id);
    try {
      await api.post(`/api/pedidos/${id}/cancelar`);
      carregar();
    } catch (err) {
      alert(err?.response?.data?.message || 'Não foi possível cancelar o pedido.');
    } finally {
      setCancelandoId(null);
    }
  };

  return (
    <div className="pedidos-container">
      <div className="pedidos-header">
        <h1>Meus Pedidos</h1>
        <p className="pedidos-subtitle">Acompanhe o status dos seus pedidos de aluguel.</p>
      </div>

      {loading ? (
        <div className="loading-container">Carregando pedidos...</div>
      ) : error ? (
        <div className="error-container">{error}</div>
      ) : pedidos.length === 0 ? (
        <div className="pedidos-vazio">
          <p>Você ainda não fez nenhum pedido.</p>
          <Link to="/" className="btnPrimary">Explorar catálogo</Link>
        </div>
      ) : (
        <div className="pedidos-lista">
          {pedidos.map((pedido) => {
            const statusInfo = STATUS_INFO[pedido.status] || { label: pedido.status, className: '' };
            return (
              <div key={pedido.id} className="pedido-card">
                <div className="pedido-card-header">
                  <div>
                    <span className="pedido-codigo">{pedido.codigo}</span>
                    <span className={`pedido-status-badge ${statusInfo.className}`}>{statusInfo.label}</span>
                  </div>
                  <span className="pedido-valor">R$ {Number(pedido.valorTotalEstimado || 0).toFixed(2)}</span>
                </div>

                <div className="pedido-card-body">
                  <div className="pedido-info-row">
                    <span><strong>Período:</strong> {formatarData(pedido.dataInicio)} a {formatarData(pedido.dataFim)} ({pedido.diasLocacao} dia{pedido.diasLocacao > 1 ? 's' : ''})</span>
                  </div>
                  <div className="pedido-info-row">
                    <span><strong>Endereço:</strong> {pedido.enderecoEntrega}</span>
                  </div>

                  <div className="pedido-itens">
                    {pedido.itens.map((item) => (
                      <div key={item.id} className="pedido-item">
                        <span>{item.equipamentoNome} × {item.quantidade}</span>
                        <span>R$ {Number(item.valorDiariaSnapshot).toFixed(2)}/dia</span>
                      </div>
                    ))}
                  </div>

                  {pedido.observacoesCliente && (
                    <div className="pedido-info-row">
                      <span><strong>Suas observações:</strong> {pedido.observacoesCliente}</span>
                    </div>
                  )}

                  {pedido.motivoRecusa && (
                    <div className="pedido-motivo-recusa">
                      <strong>Motivo:</strong> {pedido.motivoRecusa}
                    </div>
                  )}
                </div>

                {podeCancelar(pedido.status) && (
                  <div className="pedido-card-actions">
                    <button
                      className="btnSecondary"
                      onClick={() => handleCancelar(pedido.id)}
                      disabled={cancelandoId === pedido.id}
                    >
                      {cancelandoId === pedido.id ? 'Cancelando...' : 'Cancelar pedido'}
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default MeusPedidos;
