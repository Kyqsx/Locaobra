import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import './ProductPage.css';

const hojeISO = () => new Date().toISOString().split('T')[0];

const somarDias = (dataISO, dias) => {
  const d = new Date(`${dataISO}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return d.toISOString().split('T')[0];
};

const ProductPageLocaObra = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isCliente } = useAuth();

  const [equipamento, setEquipamento] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeImage, setActiveImage] = useState(0);
  const [selectedOption, setSelectedOption] = useState('daily');

  const [showCheckout, setShowCheckout] = useState(false);

  const images = equipamento?.imagens?.length > 0 ? equipamento.imagens : [null, null, null, null];

  const imageUrl = (path) => {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
  };

  const defaultSpecs = [
    { label: 'Categoria', value: equipamento?.categoria || '—' },
    { label: 'Disponibilidade', value: equipamento ? `${equipamento.quantidadeDisponivel ?? 0} de ${equipamento.quantidadeTotal ?? 0} unidades` : '—' },
    { label: 'Status', value: equipamento?.status || '—' },
    { label: 'Criado em', value: equipamento?.criadoEm ? new Date(equipamento.criadoEm).toLocaleDateString('pt-BR') : '—' },
  ];

  const specs = equipamento?.especificacoes
    ? Object.entries(equipamento.especificacoes).map(([label, value]) => ({ label, value }))
    : defaultSpecs;

  const handleRentClick = () => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (!isCliente) {
      // Funcionário/admin logado navegando no catálogo — pedido é uma ação de cliente.
      alert('Apenas clientes podem solicitar um pedido de aluguel pelo catálogo.');
      return;
    }
    setShowCheckout(true);
  };

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    api.get(`/api/equipamentos/${id}`)
      .then(response => setEquipamento(response.data))
      .catch(() => setError('Não foi possível carregar o equipamento.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <div className="loading-container">Carregando equipamento...</div>;
  }

  if (error) {
    return <div className="error-container">{error}</div>;
  }

  if (!equipamento) {
    return <div className="error-container">Equipamento não encontrado.</div>;
  }

  const valorDiaria = equipamento.valorDiaria ? Number(equipamento.valorDiaria) : 0;

  // Multiplicador baseado na opção selecionada
  const multiplicadores = {
    daily: 1,
    weekly: 7,
    monthly: 30,
  };

  const multiplicador = multiplicadores[selectedOption] || 1;
  const valorTotal = valorDiaria * multiplicador;

  return (
    <div>
      {/* Produto */}
      <div className="produto-wrapper">
        <div className="produto-container">
          {/* Seção de Imagens com Título e Descrição */}
          <div className="product-image-section">
            <div className="main-image-container">
              {images[activeImage] ? (
                <img className="product-main-image" src={imageUrl(images[activeImage])} alt={`Imagem ${activeImage + 1}`} />
              ) : (
                <div className="main-image-placeholder">📐</div>
              )}
            </div>
            <div className="thumbnail-carousel">
              {images.map((image, index) => (
                <div
                  key={index}
                  className={`thumbnail ${activeImage === index ? 'active' : ''}`}
                  onClick={() => setActiveImage(index)}
                >
                  {image ? (
                    <img className="thumbnail-image" src={imageUrl(image)} alt={`Preview ${index + 1}`} />
                  ) : (
                    <span style={{ fontSize: '28px' }}>📐</span>
                  )}
                </div>
              ))}
            </div>

            {/* Informações Adicionais */}
            <div className="additional-info">
              <div className="info-title">
                <span className="info-icon">ℹ</span>
                Informações de Aluguel
              </div>
              <div className="info-list">
                <div className="info-item">
                  <span className="info-icon">📦</span>
                  Entrega e retirada gratuitas em São Paulo
                </div>
                <div className="info-item">
                  <span className="info-icon">🛡️</span>
                  Produto com seguro incluído
                </div>
                <div className="info-item">
                  <span className="info-icon">⚙️</span>
                  Suporte técnico 24/7
                </div>
                <div className="info-item">
                  <span className="info-icon">💳</span>
                  Pagamento seguro com parcelamento
                </div>
              </div>
            </div>
          </div>

          {/* Seção de Informações - Lado Direito */}
          <div className="product-info-section">
            {/* Título, Descrição e Avaliações embaixo das imagens */}
            <div className="product-text-section">
              <h1 className="product-title">{equipamento.nome}</h1>
              <p className="product-description">
                {equipamento.descricao || 'Sem descrição disponível.'}
              </p>
              <div className="rating-section">
                <span className="stars">★★★★★</span>
                <span className="rating-count">{equipamento.status || 'Disponível'}</span>
              </div>
              <div className="product-details">
                <p><strong>Categoria:</strong> {equipamento.categoria || '—'}</p>
                <p><strong>Disponíveis:</strong> {equipamento.quantidadeDisponivel ?? 0} unidades</p>
                <p><strong>Valor diária:</strong> R$ {valorDiaria.toFixed(2)}</p>
                
              </div>
            </div>

            {/* Especificações */}
            <div className="specs-section">
              <div className="specs-title">Especificações Técnicas</div>
              <div className="specs-grid">
                {specs.map((spec, index) => (
                  <div key={index} className="spec-item">
                    <div className="spec-content">
                      <div className="spec-label">{spec.label}: <span className='spec-value'>{spec.value}</span></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Tipo de Locação */}
            <div className="options-section">
              <div className="option-group">
                <label className="option-label">Tipo de locação</label>
                <select
                  className="option-select"
                  value={selectedOption}
                  onChange={(e) => setSelectedOption(e.target.value)}
                >
                  <option value="daily">Diária (1 dia)</option>
                  <option value="weekly">Semanal (7 dias)</option>
                  <option value="monthly">Mensal (30 dias)</option>
                </select>
              </div>
              <p className="option-price">Valor estimado: R$ {valorTotal.toFixed(2)}</p>
            </div>

            {/* Ações */}
            <div className="actions-section">
              <button
                className="btn-main"
                onClick={handleRentClick}
                disabled={(equipamento.quantidadeDisponivel ?? 0) < 1}
              >
                <span>🛒</span> {(equipamento.quantidadeDisponivel ?? 0) < 1 ? 'Indisponível no momento' : 'Alugar Agora'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {showCheckout && (
        <CheckoutModal
          equipamento={equipamento}
          diasIniciais={multiplicador}
          user={user}
          onClose={() => setShowCheckout(false)}
        />
      )}
    </div>
  );
};

function CheckoutModal({ equipamento, diasIniciais, user, onClose }) {
  const navigate = useNavigate();
  const valorDiaria = equipamento.valorDiaria ? Number(equipamento.valorDiaria) : 0;
  const disponivel = equipamento.quantidadeDisponivel ?? 0;

  const [dataInicio, setDataInicio] = useState(hojeISO());
  const [dataFim, setDataFim] = useState(somarDias(hojeISO(), diasIniciais || 1));
  const [quantidade, setQuantidade] = useState(1);
  const [enderecoEntrega, setEnderecoEntrega] = useState(user?.enderecoFormatado || '');
  const [observacoesCliente, setObservacoesCliente] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [pedidoCriado, setPedidoCriado] = useState(null);

  const dias = Math.max(1, Math.round((new Date(`${dataFim}T00:00:00`) - new Date(`${dataInicio}T00:00:00`)) / 86400000));
  const valorTotal = valorDiaria * quantidade * dias;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro(null);

    if (!dataInicio || !dataFim) {
      setErro('Informe as datas de início e fim da locação.');
      return;
    }
    if (dataFim < dataInicio) {
      setErro('A data de fim não pode ser anterior à data de início.');
      return;
    }
    if (!enderecoEntrega.trim()) {
      setErro('Informe o endereço de entrega.');
      return;
    }
    if (quantidade < 1 || quantidade > disponivel) {
      setErro(`Escolha uma quantidade entre 1 e ${disponivel} (disponível).`);
      return;
    }

    setEnviando(true);
    try {
      const response = await api.post('/api/pedidos', {
        dataInicio,
        dataFim,
        enderecoEntrega: enderecoEntrega.trim(),
        observacoesCliente: observacoesCliente.trim() || null,
        itens: [
          { equipamentoId: equipamento.id, quantidade, observacaoItem: null },
        ],
      });
      setPedidoCriado(response.data);
    } catch (err) {
      setErro(err?.response?.data?.message || 'Não foi possível enviar o pedido. Tente novamente.');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="checkoutBackdrop" onClick={onClose}>
      <div className="checkoutModalCard" onClick={(e) => e.stopPropagation()}>
        {pedidoCriado ? (
          <div className="checkoutSuccess">
            <div className="checkoutSuccessIcon">✅</div>
            <h3>Pedido enviado!</h3>
            <p>
              Seu orçamento <strong>{pedidoCriado.codigo}</strong> foi enviado e está aguardando
              revisão da nossa equipe. Você pode acompanhar o status a qualquer momento em
              "Meus Pedidos".
            </p>
            <div className="checkoutModalActions">
              <button type="button" className="btnSecondary" onClick={onClose}>Fechar</button>
              <button type="button" className="btnPrimary" onClick={() => navigate('/meus-pedidos')}>
                Ver Meus Pedidos
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="checkoutModalHeader">
              <h3>Solicitar aluguel</h3>
              <button type="button" className="checkoutCloseBtn" onClick={onClose}>✕</button>
            </div>

            <p className="checkoutProdutoNome">{equipamento.nome}</p>

            {erro && <div className="checkoutErro">{erro}</div>}

            <form onSubmit={handleSubmit} className="checkoutForm">
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
                <label>Quantidade ({disponivel} disponível{disponivel === 1 ? '' : 'is'})</label>
                <input
                  type="number"
                  min={1}
                  max={disponivel}
                  value={quantidade}
                  onChange={(e) => setQuantidade(Number(e.target.value))}
                  required
                />
              </div>

              <div className="checkoutField">
                <label>Endereço de entrega</label>
                <textarea
                  rows={2}
                  value={enderecoEntrega}
                  onChange={(e) => setEnderecoEntrega(e.target.value)}
                  placeholder="Rua, número, bairro, cidade/UF"
                  required
                />
              </div>

              <div className="checkoutField">
                <label>Observações (opcional)</label>
                <textarea
                  rows={2}
                  value={observacoesCliente}
                  onChange={(e) => setObservacoesCliente(e.target.value)}
                  placeholder="Alguma informação adicional para o consultor?"
                />
              </div>

              <div className="checkoutResumo">
                <span>{dias} dia{dias > 1 ? 's' : ''} × {quantidade}x R$ {valorDiaria.toFixed(2)}</span>
                <strong>R$ {valorTotal.toFixed(2)}</strong>
              </div>

              <div className="checkoutModalActions">
                <button type="button" className="btnSecondary" onClick={onClose} disabled={enviando}>
                  Cancelar
                </button>
                <button type="submit" className="btnPrimary" disabled={enviando}>
                  {enviando ? 'Enviando...' : 'Enviar pedido'}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export default ProductPageLocaObra;
