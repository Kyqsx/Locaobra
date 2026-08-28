import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
          <span><strong>Endereço:</strong> {pedido.enderecoEntrega?.formatado || '---'}</span>
        </div>

        <div className="pedido-itens">
          {pedido.itens.map((item) => (
            <div key={item.id} className="pedido-item">
              <span>
                {item.equipamentoNome} × {item.quantidade}
                {item.depositoNome && <span className="pedido-item-deposito"> · {item.depositoNome}</span>}
              </span>
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

// Modal de confirmação do consultor: mostra de qual depósito (ou depósitos)
// o sistema sugere atender o pedido, com um dropdown por item pra ajustar
// manualmente antes de confirmar. Quando os itens estão espalhados em mais
// de um depósito, o pedido vai gerar uma expedição por depósito depois
// (fila do conferente).
function AlocacaoDepositoModal({ pedido, onClose, onConfirmed }) {
  const [sugestao, setSugestao] = useState(null);
  const [depositos, setDepositos] = useState([]);
  const [alocacoes, setAlocacoes] = useState({}); // itemPedidoId -> depositoId
  const [observacoes, setObservacoes] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    setCarregando(true);
    setErro(null);
    Promise.all([
      api.get(`/api/pedidos/${pedido.id}/sugestao-depositos`),
      api.get('/api/depositos'),
    ])
      .then(([sugestaoRes, depositosRes]) => {
        setSugestao(sugestaoRes.data);
        setDepositos(depositosRes.data.filter((d) => d.ativo !== false));

        const inicial = {};
        (sugestaoRes.data.grupos || []).forEach((grupo) => {
          grupo.itens.forEach((item) => {
            inicial[item.itemPedidoId] = grupo.depositoId;
          });
        });
        (sugestaoRes.data.itensNaoAtendidos || []).forEach((item) => {
          if (item.depositoComMaisDisponibilidadeId) {
            inicial[item.itemPedidoId] = item.depositoComMaisDisponibilidadeId;
          }
        });
        setAlocacoes(inicial);
      })
      .catch(() => setErro('Não foi possível calcular a sugestão de depósito.'))
      .finally(() => setCarregando(false));
  }, [pedido.id]);

  const handleAlocacaoChange = (itemPedidoId, depositoId) => {
    setAlocacoes((prev) => ({ ...prev, [itemPedidoId]: Number(depositoId) }));
  };

  const totalItens = pedido.itens.length;
  const itensAlocados = Object.values(alocacoes).filter(Boolean).length;
  const podeConfirmar = itensAlocados === totalItens && !enviando;

  const depositosEnvolvidos = new Set(Object.values(alocacoes).filter(Boolean)).size;

  const handleConfirmar = async () => {
    setEnviando(true);
    setErro(null);
    try {
      await api.patch(`/api/pedidos/${pedido.id}/confirmar`, {
        observacoes: observacoes.trim() || null,
        alocacoes: Object.entries(alocacoes).map(([itemPedidoId, depositoId]) => ({
          itemPedidoId: Number(itemPedidoId),
          depositoId,
        })),
      });
      onConfirmed();
    } catch (err) {
      setErro(err?.response?.data?.message || 'Não foi possível confirmar o pedido.');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="checkoutBackdrop" onClick={onClose}>
      <div className="checkoutModalCard alocacaoModalCard" onClick={(e) => e.stopPropagation()}>
        <div className="checkoutModalHeader">
          <h3>Confirmar {pedido.codigo}</h3>
          <button type="button" className="checkoutCloseBtn" onClick={onClose}>✕</button>
        </div>

        {carregando ? (
          <div className="loading-container">Calculando a melhor forma de atender esse pedido...</div>
        ) : (
          <>
            {sugestao?.atendeUmDeposito && (
              <div className="alocacaoAviso alocacaoAvisoOk">
                ✓ O depósito <strong>{sugestao.depositoUnicoNome}</strong> tem tudo que esse pedido precisa.
              </div>
            )}
            {sugestao && !sugestao.atendeUmDeposito && depositosEnvolvidos > 1 && (
              <div className="alocacaoAviso alocacaoAvisoSplit">
                ⚠ Nenhum depósito sozinho atende o pedido inteiro. Sugerimos dividir entre{' '}
                <strong>{depositosEnvolvidos} depósitos</strong> — o sistema vai gerar uma expedição
                separada pra cada um.
              </div>
            )}
            {sugestao?.itensNaoAtendidos?.length > 0 && (
              <div className="alocacaoAviso alocacaoAvisoErro">
                ⚠ {sugestao.itensNaoAtendidos.length === 1 ? 'Um item não tem' : `${sugestao.itensNaoAtendidos.length} itens não têm`} estoque
                suficiente em nenhum depósito sozinho. Revise as quantidades abaixo ou recuse o pedido.
              </div>
            )}

            {erro && <div className="checkoutErro">{erro}</div>}

            <div className="alocacaoItensLista">
              {pedido.itens.map((item) => {
                const naoAtendido = sugestao?.itensNaoAtendidos?.find((n) => n.itemPedidoId === item.id);
                return (
                  <div key={item.id} className="alocacaoItemRow">
                    <div className="alocacaoItemInfo">
                      <span className="alocacaoItemNome">{item.equipamentoNome} × {item.quantidade}</span>
                      {naoAtendido && (
                        <span className="alocacaoItemAlerta">
                          Máximo encontrado: {naoAtendido.maiorDisponibilidadeEncontrada}
                          {naoAtendido.depositoComMaisDisponibilidadeNome ? ` em ${naoAtendido.depositoComMaisDisponibilidadeNome}` : ''}
                        </span>
                      )}
                    </div>
                    <select
                      value={alocacoes[item.id] || ''}
                      onChange={(e) => handleAlocacaoChange(item.id, e.target.value)}
                      className={naoAtendido ? 'alocacaoSelectAlerta' : ''}
                    >
                      <option value="">Selecione o depósito</option>
                      {depositos.map((d) => (
                        <option key={d.id} value={d.id}>{d.nome}</option>
                      ))}
                    </select>
                  </div>
                );
              })}
            </div>

            <div className="checkoutField">
              <label>Observações (opcional)</label>
              <textarea
                rows={2}
                value={observacoes}
                onChange={(e) => setObservacoes(e.target.value)}
                placeholder="Alguma observação interna sobre esse pedido?"
              />
            </div>

            <div className="checkoutModalActions">
              <button type="button" className="btnSecondary" onClick={onClose} disabled={enviando}>Cancelar</button>
              <button type="button" className="btnPrimary" disabled={!podeConfirmar} onClick={handleConfirmar}>
                {enviando ? 'Confirmando...' : 'Confirmar e enviar p/ crédito'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function AdminPedidos() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const cargo = user?.cargoFuncionario;
  const isAdminGeral = user?.tipo === 'ADMIN' || cargo === 'GERENTE_OPERACOES';

  const podeConsultor = isAdminGeral || cargo === 'CONSULTOR_LOCACAO';
  const podeCredito = isAdminGeral || cargo === 'ANALISTA_CREDENCIAMENTO';
  const podeConferente = isAdminGeral || cargo === 'CONFERENTE';

  const abasDisponiveis = [
    podeConsultor && { id: 'consultor', label: 'Solicitações' },
    podeCredito && { id: 'credito', label: 'Análise de Crédito' },
    podeConferente && { id: 'conferente', label: 'Prontos p/ Expedição' },
  ].filter(Boolean);

  const [aba, setAba] = useState(abasDisponiveis[0]?.id || 'consultor');
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processandoId, setProcessandoId] = useState(null);
  const [modalRecusa, setModalRecusa] = useState(null); // { pedidoId, tipo: 'recusar' | 'reprovar' }
  const [modalAlocacao, setModalAlocacao] = useState(null); // pedido sendo confirmado

  const ENDPOINT_POR_ABA = {
    consultor: '/api/pedidos/fila-consultor',
    credito: '/api/pedidos/fila-credito',
    conferente: '/api/pedidos/fila-conferente',
  };
  const endpoint = ENDPOINT_POR_ABA[aba];

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

  // Leva o conferente pra tela de Expedição já com o modal de "Nova
  // Expedição" aberto e pré-preenchido a partir deste pedido+depósito. Um
  // pedido desmembrado gera uma expedição por grupo — cada botão abaixo
  // cobre só os itens daquele depósito.
  const handleGerarExpedicao = (pedido, grupo) => {
    navigate('/admin/expedicao', {
      state: { pedidoOrigemId: pedido.id, depositoOrigemId: grupo.depositoId },
    });
  };

  return (
    <div className="pedidos-container admin-pedidos">
      <div className="pedidos-header">
        <h1>Pedidos</h1>
        <p className="pedidos-subtitle">Solicitações de aluguel enviadas pelos clientes pelo catálogo.</p>
      </div>

      {abasDisponiveis.length > 1 && (
        <div className="pedidos-tabs">
          {abasDisponiveis.map((a) => (
            <button
              key={a.id}
              className={`pedidos-tab-btn ${aba === a.id ? 'active' : ''}`}
              onClick={() => setAba(a.id)}
            >
              {a.label}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <div className="loading-container">Carregando pedidos...</div>
      ) : error ? (
        <div className="error-container">{error}</div>
      ) : pedidos.length === 0 ? (
        <div className="pedidos-vazio">
          <p>Nenhum pedido {aba === 'consultor' ? 'aguardando revisão' : aba === 'credito' ? 'aguardando análise de crédito' : 'pronto para expedição'} no momento.</p>
        </div>
      ) : (
        <div className="pedidos-lista">
          {pedidos.map((pedido) => (
            <PedidoCard key={pedido.id} pedido={pedido}>
              {aba === 'consultor' && (
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
                    onClick={() => setModalAlocacao(pedido)}
                  >
                    Revisar e confirmar
                  </button>
                </>
              )}
              {aba === 'credito' && (
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
              {aba === 'conferente' && (
                <div className="gruposPendentesWrapper">
                  {(pedido.gruposPendentes || []).map((grupo) => (
                    <div key={grupo.depositoId} className="grupoPendenteRow">
                      <span className="grupoPendenteInfo">
                        <strong>{grupo.depositoNome}</strong>: {grupo.itens.map((i) => `${i.equipamentoNome} ×${i.quantidade}`).join(', ')}
                      </span>
                      <button className="btnPrimary" onClick={() => handleGerarExpedicao(pedido, grupo)}>
                        Gerar expedição
                      </button>
                    </div>
                  ))}
                </div>
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

      {modalAlocacao && (
        <AlocacaoDepositoModal
          pedido={modalAlocacao}
          onClose={() => setModalAlocacao(null)}
          onConfirmed={() => {
            setModalAlocacao(null);
            carregar();
          }}
        />
      )}
    </div>
  );
}

export default AdminPedidos;
