import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import { useCart } from '../../context/CartContext';
import EnderecoFields from '../../components/EnderecoFields';
import './Cart.css';

const hojeISO = () => new Date().toISOString().split('T')[0];

const somarDias = (dataISO, dias) => {
  const d = new Date(`${dataISO}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return d.toISOString().split('T')[0];
};

const imageUrl = (path) => {
  if (!path) return null;
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${api.defaults.baseURL}${path}`;
};

const enderecoNovoVazio = { cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '' };

function Carrinho() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { itens, atualizarQuantidade, atualizarObservacao, removerItem, limparCarrinho } = useCart();

  const [dataInicio, setDataInicio] = useState(hojeISO());
  const [dataFim, setDataFim] = useState(somarDias(hojeISO(), 1));

  const enderecosSalvos = user?.enderecos || [];
  const principalSalvo = enderecosSalvos.find((e) => e.principal) || enderecosSalvos[0] || null;
  const [enderecoSelecionadoId, setEnderecoSelecionadoId] = useState(principalSalvo ? principalSalvo.id : 'novo');
  const [enderecoNovo, setEnderecoNovo] = useState(enderecoNovoVazio);

  const [observacoesCliente, setObservacoesCliente] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [pedidoCriado, setPedidoCriado] = useState(null);

  const dias = Math.max(1, Math.round((new Date(`${dataFim}T00:00:00`) - new Date(`${dataInicio}T00:00:00`)) / 86400000));
  const valorTotal = itens.reduce((soma, i) => soma + i.valorDiaria * i.quantidade * dias, 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro(null);

    if (itens.length === 0) {
      setErro('Seu carrinho está vazio. Adicione ao menos um equipamento.');
      return;
    }
    if (!dataInicio || !dataFim) {
      setErro('Informe as datas de início e fim da locação.');
      return;
    }
    if (dataFim < dataInicio) {
      setErro('A data de fim não pode ser anterior à data de início.');
      return;
    }
    let enderecoPayload = {};
    if (enderecoSelecionadoId === 'novo') {
      if (!enderecoNovo.rua.trim() || !enderecoNovo.cidade.trim() || !enderecoNovo.estado.trim()) {
        setErro('Preencha ao menos rua, cidade e UF do endereço de entrega.');
        return;
      }
      enderecoPayload = { enderecoEntrega: enderecoNovo };
    } else {
      enderecoPayload = { enderecoId: Number(enderecoSelecionadoId) };
    }

    setEnviando(true);
    try {
      const response = await api.post('/api/pedidos', {
        dataInicio,
        dataFim,
        ...enderecoPayload,
        observacoesCliente: observacoesCliente.trim() || null,
        itens: itens.map((i) => ({
          equipamentoId: i.equipamentoId,
          quantidade: i.quantidade,
          observacaoItem: i.observacaoItem?.trim() || null,
        })),
      });
      setPedidoCriado(response.data);
      limparCarrinho();
    } catch (err) {
      setErro(err?.response?.data?.message || 'Não foi possível enviar o pedido. Tente novamente.');
    } finally {
      setEnviando(false);
    }
  };

  if (pedidoCriado) {
    return (
      <div className="carrinho-container">
        <div className="carrinho-sucesso">
          <div className="carrinho-sucesso-icon">✅</div>
          <h3>Pedido enviado!</h3>
          <p>
            Seu orçamento <strong>{pedidoCriado.codigo}</strong> foi enviado e está aguardando
            revisão da nossa equipe. Você pode acompanhar o status a qualquer momento em
            "Meus Pedidos".
          </p>
          <div className="carrinho-sucesso-acoes">
            <Link to="/" className="btnSecondary">Continuar comprando</Link>
            <button type="button" className="btnPrimary" onClick={() => navigate('/meus-pedidos')}>
              Ver Meus Pedidos
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (itens.length === 0) {
    return (
      <div className="carrinho-container">
        <div className="carrinho-header">
          <h1>Seu carrinho</h1>
        </div>
        <div className="carrinho-vazio">
          <p>Seu carrinho está vazio no momento.</p>
          <Link to="/" className="btnPrimary">Ver catálogo</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="carrinho-container">
      <div className="carrinho-header">
        <h1>Seu carrinho</h1>
        <p className="carrinho-subtitle">Revise os itens e envie um único pedido de orçamento.</p>
      </div>

      <div className="carrinho-layout">
        <div className="carrinho-itens">
          {itens.map((item) => (
            <div key={item.equipamentoId} className="carrinho-item">
              <div className="carrinho-item-imagem">
                {item.imagem ? (
                  <img src={imageUrl(item.imagem)} alt={item.nome} />
                ) : (
                  <span className="carrinho-item-placeholder">📐</span>
                )}
              </div>

              <div className="carrinho-item-info">
                <h3>{item.nome}</h3>
                <p className="carrinho-item-preco">R$ {item.valorDiaria.toFixed(2)} / diária</p>

                <div className="carrinho-item-linha">
                  <label>Quantidade</label>
                  <div className="quantity-selector">
                    <button
                      type="button"
                      className="quantity-btn"
                      onClick={() => atualizarQuantidade(item.equipamentoId, item.quantidade - 1)}
                      disabled={item.quantidade <= 1}
                    >
                      −
                    </button>
                    <input
                      type="number"
                      className="quantity-input"
                      min={1}
                      max={item.quantidadeDisponivel || 1}
                      value={item.quantidade}
                      onChange={(e) => atualizarQuantidade(item.equipamentoId, Number(e.target.value))}
                    />
                    <button
                      type="button"
                      className="quantity-btn"
                      onClick={() => atualizarQuantidade(item.equipamentoId, item.quantidade + 1)}
                      disabled={item.quantidade >= (item.quantidadeDisponivel || 1)}
                    >
                      +
                    </button>
                  </div>
                  <span className="carrinho-item-disponivel">
                    {item.quantidadeDisponivel ?? 0} disponíve{item.quantidadeDisponivel === 1 ? 'l' : 'is'}
                  </span>
                </div>

                <input
                  type="text"
                  className="carrinho-item-obs"
                  placeholder="Observação para esse item (opcional)"
                  value={item.observacaoItem || ''}
                  onChange={(e) => atualizarObservacao(item.equipamentoId, e.target.value)}
                />
              </div>

              <div className="carrinho-item-acoes">
                <span className="carrinho-item-subtotal">
                  R$ {(item.valorDiaria * item.quantidade * dias).toFixed(2)}
                </span>
                <button
                  type="button"
                  className="carrinho-item-remover"
                  onClick={() => removerItem(item.equipamentoId)}
                >
                  Remover
                </button>
              </div>
            </div>
          ))}
        </div>

        <form className="carrinho-resumo" onSubmit={handleSubmit}>
          <h2>Dados do pedido</h2>

          {erro && <div className="checkoutErro">{erro}</div>}

          <div className="checkoutFieldRow">
            <div className="checkoutField">
              <label>Data de início</label>
              <input
                type="date"
                min={hojeISO()}
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                required
              />
            </div>
            <div className="checkoutField">
              <label>Data de fim</label>
              <input
                type="date"
                min={dataInicio}
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="checkoutField">
            <label>Endereço de entrega</label>
            {enderecosSalvos.length > 0 && (
              <div className="carrinho-enderecos-salvos">
                {enderecosSalvos.map((endereco) => (
                  <label key={endereco.id} className="carrinho-endereco-opcao">
                    <input
                      type="radio"
                      name="enderecoSalvo"
                      checked={String(enderecoSelecionadoId) === String(endereco.id)}
                      onChange={() => setEnderecoSelecionadoId(endereco.id)}
                    />
                    <span>
                      <strong>{endereco.apelido || 'Endereço'}</strong>
                      {endereco.principal && <span className="carrinho-endereco-tag">Principal</span>}
                      <br />
                      <span className="carrinho-endereco-detalhe">{endereco.formatado}</span>
                    </span>
                  </label>
                ))}
                <label className="carrinho-endereco-opcao">
                  <input
                    type="radio"
                    name="enderecoSalvo"
                    checked={enderecoSelecionadoId === 'novo'}
                    onChange={() => setEnderecoSelecionadoId('novo')}
                  />
                  <span><strong>Usar outro endereço</strong></span>
                </label>
              </div>
            )}

            {enderecoSelecionadoId === 'novo' && (
              <div className="carrinho-endereco-novo">
                <EnderecoFields value={enderecoNovo} onChange={setEnderecoNovo} prefixo="carrinho" />
                <Link to="/meus-enderecos" className="carrinho-link-salvar-endereco">
                  Prefere salvar endereços pra usar depois? Gerencie em "Meus Endereços".
                </Link>
              </div>
            )}
          </div>

          <div className="checkoutField">
            <label>Observações gerais (opcional)</label>
            <textarea
              rows={2}
              value={observacoesCliente}
              onChange={(e) => setObservacoesCliente(e.target.value)}
              placeholder="Alguma informação adicional para o consultor?"
            />
          </div>

          <div className="carrinho-resumo-linha">
            <span>{itens.reduce((n, i) => n + i.quantidade, 0)} ite{itens.reduce((n, i) => n + i.quantidade, 0) > 1 ? 'ns' : 'm'} × {dias} dia{dias > 1 ? 's' : ''}</span>
            <strong>R$ {valorTotal.toFixed(2)}</strong>
          </div>

          <button type="submit" className="btnPrimary carrinho-finalizar" disabled={enviando}>
            {enviando ? 'Enviando...' : 'Finalizar pedido'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Carrinho;
