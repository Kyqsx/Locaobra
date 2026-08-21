import React, { useEffect, useState } from 'react';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
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

function PedidoCard({ pedido, children }) {
  const statusInfo = STATUS_INFO[pedido.status] || { label: pedido.status, className: '' };
  return (
    <div className="pedido-card">
      <div className="pedido-card-header">
        <div>
          <span className="pedido-codigo">{pedido.codigo}</span>
          <span className={`pedido-status-badge ${statusInfo.className}`}>{statusInfo.label}</span>
        </div>
        <span className="pedido-valor">R$ {Number(pedido.valorTotalEstimado || 0).toFixed(2)}</span>
      </div>

      <div className="pedido-card-body">
        <div className="pedido-info-row">
          <span><strong>Cliente:</strong> {pedido.clienteNome}</span>
        </div>
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
            <span><strong>Observações do cliente:</strong> {pedido.observacoesCliente}</span>
          </div>
        )}
      </div>

      {children && <div className="pedido-card-actions">{children}</div>}
    </div>
  );
}

function MotivoModal({ titulo, onConfirm, onClose, enviando }) {
  const [motivo, setMotivo] = useState('');
  return (
    <div className="checkoutBackdrop" onClick={onClose}>
      <div className="checkoutModalCard motivoModalCard" onClick={(e) => e.stopPropagation()}>
        <div className="checkoutModalHeader">
          <h3>{titulo}</h3>
          <button type="button" className="checkoutCloseBtn" onClick={onClose}>✕</button>
        </div>
        <div className="checkoutField">
          <label>Motivo</label>
          <textarea
            rows={3}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Explique o motivo para o cliente..."
            autoFocus
          />
        </div>
        <div className="checkoutModalActions">
          <button type="button" className="btnSecondary" onClick={onClose} disabled={enviando}>Cancelar</button>
          <button
            type="button"
            className="btnDanger"
            disabled={enviando || !motivo.trim()}
            onClick={() => onConfirm(motivo.trim())}
          >
            {enviando ? 'Enviando...' : 'Confirmar'}
          </button>
        </div>
      </div>
    </div>
  );
}

function AdminPedidos() {
  const { user } = useAuth();
  const cargo = user?.cargoFuncionario;
  const isAdminGeral = user?.tipo === 'ADMIN' || cargo === 'GERENTE_OPERACOES';

  const podeConsultor = isAdminGeral || cargo === 'CONSULTOR_LOCACAO';
  const podeCredito = isAdminGeral || cargo === 'ANALISTA_CREDENCIAMENTO';

  const [aba, setAba] = useState(podeConsultor ? 'consultor' : 'credito');
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processandoId, setProcessandoId] = useState(null);
  const [modalRecusa, setModalRecusa] = useState(null); // { pedidoId, tipo: 'recusar' | 'reprovar' }

  const endpoint = aba === 'consultor' ? '/api/pedidos/fila-consultor' : '/api/pedidos/fila-credito';

  const carregar = () => {
    setLoading(true);
    setError(null);
    api.get(endpoint)
      .then((res) => setPedidos(res.data))
      .catch(() => setError('Não foi possível carregar os pedidos.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aba]);

  const handleConfirmar = async (id) => {
    setProcessandoId(id);
    try {
      await api.patch(`/api/pedidos/${id}/confirmar`, {});
      carregar();
    } catch (err) {
      alert(err?.response?.data?.message || 'Não foi possível confirmar o pedido.');
    } finally {
      setProcessandoId(null);
    }
  };

  const handleAprovarCredito = async (id) => {
    setProcessandoId(id);
    try {
      await api.patch(`/api/pedidos/${id}/aprovar-credito`, {});
      carregar();
    } catch (err) {
      alert(err?.response?.data?.message || 'Não foi possível aprovar o crédito.');
    } finally {
      setProcessandoId(null);
    }
  };

  const handleConfirmarRecusa = async (motivo) => {
    const { pedidoId, tipo } = modalRecusa;
    setProcessandoId(pedidoId);
    try {
      const rota = tipo === 'recusar' ? 'recusar' : 'reprovar-credito';
      await api.patch(`/api/pedidos/${pedidoId}/${rota}`, { motivo });
      setModalRecusa(null);
      carregar();
    } catch (err) {
      alert(err?.response?.data?.message || 'Não foi possível concluir a ação.');
    } finally {
      setProcessandoId(null);
    }
  };

  return (
    <div className="pedidos-container admin-pedidos">
      <div className="pedidos-header">
        <h1>Pedidos</h1>
        <p className="pedidos-subtitle">Solicitações de aluguel enviadas pelos clientes pelo catálogo.</p>
      </div>

      {(podeConsultor && podeCredito) && (
        <div className="pedidos-tabs">
          <button
            className={`pedidos-tab-btn ${aba === 'consultor' ? 'active' : ''}`}
            onClick={() => setAba('consultor')}
          >
            Solicitações
          </button>
          <button
            className={`pedidos-tab-btn ${aba === 'credito' ? 'active' : ''}`}
            onClick={() => setAba('credito')}
          >
            Análise de Crédito
          </button>
        </div>
      )}

      {loading ? (
        <div className="loading-container">Carregando pedidos...</div>
      ) : error ? (
        <div className="error-container">{error}</div>
      ) : pedidos.length === 0 ? (
        <div className="pedidos-vazio">
          <p>Nenhum pedido aguardando {aba === 'consultor' ? 'revisão' : 'análise de crédito'} no momento.</p>
        </div>
      ) : (
        <div className="pedidos-lista">
          {pedidos.map((pedido) => (
            <PedidoCard key={pedido.id} pedido={pedido}>
              {aba === 'consultor' ? (
                <>
                  <button
                    className="btnSecondary btnDanger"
                    disabled={processandoId === pedido.id}
                    onClick={() => setModalRecusa({ pedidoId: pedido.id, tipo: 'recusar' })}
                  >
                    Recusar
                  </button>
                  <button
                    className="btnPrimary"
                    disabled={processandoId === pedido.id}
                    onClick={() => handleConfirmar(pedido.id)}
                  >
                    {processandoId === pedido.id ? 'Confirmando...' : 'Confirmar e enviar p/ crédito'}
                  </button>
                </>
              ) : (
                <>
                  <button
                    className="btnSecondary btnDanger"
                    disabled={processandoId === pedido.id}
                    onClick={() => setModalRecusa({ pedidoId: pedido.id, tipo: 'reprovar' })}
                  >
                    Reprovar crédito
                  </button>
                  <button
                    className="btnPrimary"
                    disabled={processandoId === pedido.id}
                    onClick={() => handleAprovarCredito(pedido.id)}
                  >
                    {processandoId === pedido.id ? 'Aprovando...' : 'Aprovar crédito'}
                  </button>
                </>
              )}
            </PedidoCard>
          ))}
        </div>
      )}

      {modalRecusa && (
        <MotivoModal
          titulo={modalRecusa.tipo === 'recusar' ? 'Recusar pedido' : 'Reprovar crédito'}
          onConfirm={handleConfirmarRecusa}
          onClose={() => setModalRecusa(null)}
          enviando={processandoId === modalRecusa.pedidoId}
        />
      )}
    </div>
  );
}

export default AdminPedidos;
