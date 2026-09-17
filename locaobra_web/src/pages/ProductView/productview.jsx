import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import { useCart } from '../../context/CartContext';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faShoppingCart, faBox, faShield, faGear, faCreditCard, faInfo } from '@fortawesome/free-solid-svg-icons';
import './ProductPage.css';

const ProductPageLocaObra = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isCliente } = useAuth();
  const { adicionarItem } = useCart();

  const [equipamento, setEquipamento] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeImage, setActiveImage] = useState(0);
  const [selectedOption, setSelectedOption] = useState('daily');
  const [quantidade, setQuantidade] = useState(1);
  const [mensagemCarrinho, setMensagemCarrinho] = useState(null);

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

  const disponivel = equipamento?.quantidadeDisponivel ?? 0;

  const garantirPodeComprar = () => {
    if (!user) {
      navigate('/login');
      return false;
    }
    if (!isCliente) {
      // Funcionário/admin logado navegando no catálogo — pedido é uma ação de cliente.
      alert('Apenas clientes podem solicitar um pedido de aluguel pelo catálogo.');
      return false;
    }
    return true;
  };

  const handleAdicionarAoCarrinho = () => {
    if (!garantirPodeComprar()) return;
    adicionarItem(equipamento, quantidade);
    setMensagemCarrinho('added');
  };

  const handleComprarAgora = () => {
    if (!garantirPodeComprar()) return;
    adicionarItem(equipamento, quantidade);
    navigate('/carrinho');
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

  useEffect(() => {
    setQuantidade(1);
    setMensagemCarrinho(null);
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

  // Multiplicador baseado na opção selecionada (apenas para estimativa de valor nesta página)
  const multiplicadores = {
    daily: 1,
    weekly: 7,
    monthly: 30,
  };

  const multiplicador = multiplicadores[selectedOption] || 1;
  const valorTotal = valorDiaria * multiplicador * quantidade;

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
                <span className="info-icon"><FontAwesomeIcon icon={faInfo} /></span>
                Informações de Aluguel
              </div>
              <div className="info-list">
                <div className="info-item">
                  <span className="info-icon"><FontAwesomeIcon icon={faBox} /></span>
                  Entrega e retirada gratuitas em São Paulo
                </div>
                <div className="info-item">
                  <span className="info-icon"><FontAwesomeIcon icon={faShield} /></span>
                  Produto com seguro incluído
                </div>
                <div className="info-item">
                  <span className="info-icon"><FontAwesomeIcon icon={faGear} /></span>
                  Suporte técnico 24/7
                </div>
                <div className="info-item">
                  <span className="info-icon"><FontAwesomeIcon icon={faCreditCard} /></span>
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
                <p><strong>Disponíveis:</strong> {disponivel} unidades</p>
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

              <div className="option-group">
                <label className="option-label">Quantidade ({disponivel} disponível{disponivel === 1 ? '' : 'is'})</label>
                <div className="quantity-selector">
                  <button
                    type="button"
                    className="quantity-btn"
                    onClick={() => setQuantidade(q => Math.max(1, q - 1))}
                    disabled={quantidade <= 1}
                  >
                    −
                  </button>
                  <input
                    type="number"
                    className="quantity-input"
                    min={1}
                    max={disponivel || 1}
                    value={quantidade}
                    onChange={(e) => {
                      const val = Number(e.target.value) || 1;
                      setQuantidade(Math.min(Math.max(1, val), disponivel || 1));
                    }}
                  />
                  <button
                    type="button"
                    className="quantity-btn"
                    onClick={() => setQuantidade(q => Math.min(disponivel || 1, q + 1))}
                    disabled={quantidade >= disponivel}
                  >
                    +
                  </button>
                </div>
              </div>

              <p className="option-price">Valor estimado: R$ {valorTotal.toFixed(2)}</p>
            </div>

            {/* Ações */}
            <div className="actions-section">
              {mensagemCarrinho === 'added' && (
                <div className="carrinho-confirmacao">
                  ✅ Adicionado ao carrinho.{' '}
                  <Link to="/carrinho">Ver carrinho</Link>
                </div>
              )}
              <button
                className="btn-main"
                onClick={handleComprarAgora}
                disabled={disponivel < 1}
              >
                <span><FontAwesomeIcon icon={faShoppingCart} /></span> {disponivel < 1 ? 'Indisponível no momento' : 'Comprar agora'}
              </button>
              <button
                className="btn-secondary"
                onClick={handleAdicionarAoCarrinho}
                disabled={disponivel < 1}
              >
                <span><FontAwesomeIcon icon={faPlus} /></span> Adicionar ao carrinho
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductPageLocaObra;
