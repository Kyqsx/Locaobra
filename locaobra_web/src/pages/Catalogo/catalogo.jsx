import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../service/api'; // Importe sua instância do axios
import { Estrelas } from '../../components/Estrelas';
import { formatarMedia } from '../../utils/avaliacoes';
import { objectPositionDe, imageUrl } from '../../utils/imagem';
import './Catalogo.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFilter, faSearch } from '@fortawesome/free-solid-svg-icons';
import Lightbox from '../../components/Lightbox';

// Opções de ordenação do painel de filtros.
const OPCOES_ORDENACAO = [
  { valor: 'relevancia', rotulo: 'Relevância' },
  { valor: 'menor-preco', rotulo: 'Menor preço' },
  { valor: 'maior-preco', rotulo: 'Maior preço' },
  { valor: 'nome', rotulo: 'Nome (A-Z)' },
  { valor: 'avaliacao', rotulo: 'Melhor avaliados' },
];

// Devolve uma cópia ordenada da lista conforme a ordenação escolhida.
function ordenarEquipamentos(lista, ordenacao) {
  const ordenada = [...lista];
  switch (ordenacao) {
    case 'menor-preco':
      return ordenada.sort((a, b) => (a.valorDiaria ?? 0) - (b.valorDiaria ?? 0));
    case 'maior-preco':
      return ordenada.sort((a, b) => (b.valorDiaria ?? 0) - (a.valorDiaria ?? 0));
    case 'nome':
      return ordenada.sort((a, b) => (a.nome || '').localeCompare(b.nome || '', 'pt-BR'));
    case 'avaliacao':
      return ordenada.sort((a, b) =>
        (b.mediaAvaliacoes ?? 0) - (a.mediaAvaliacoes ?? 0) ||
        (b.totalAvaliacoes ?? 0) - (a.totalAvaliacoes ?? 0)
      );
    default:
      return ordenada;
  }
}

function Catalogo() {
  const { slug } = useParams();
  const [equipamentos, setEquipamentos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lightboxSrc, setLightboxSrc] = useState(null);
  const [busca, setBusca] = useState('');
  const [filtrosAbertos, setFiltrosAbertos] = useState(false);
  const [ordenacao, setOrdenacao] = useState('relevancia');
  const [apenasDisponiveis, setApenasDisponiveis] = useState(false);

  const isCatalogoCompleto = !slug;
  const nomeFormatado = slug ? slug.replace(/-/g, ' ') : "Catálogo Completo";

  // Busca local (nome ou descrição) + filtros do painel, sobre os
  // equipamentos já carregados — sem nova chamada de API.
  const termoBusca = busca.trim().toLowerCase();
  let listaBase = termoBusca
    ? equipamentos.filter(eq =>
      (eq.nome || '').toLowerCase().includes(termoBusca) ||
      (eq.descricao || '').toLowerCase().includes(termoBusca)
    )
    : equipamentos;
  if (apenasDisponiveis) {
    listaBase = listaBase.filter(eq => (eq.quantidadeDisponivel ?? 0) > 0);
  }
  const equipamentosFiltrados = ordenarEquipamentos(listaBase, ordenacao);
  const filtrosAtivos =
    (ordenacao !== 'relevancia' ? 1 : 0) + (apenasDisponiveis ? 1 : 0);

  function fetchEquipamentos() {
    setLoading(true);
    setError(null);

    // Faz a chamada para o seu endpoint de listagem
    api.get('/api/equipamentos')
      .then(response => {
        // Se houver categoria (slug) na URL, filtra; caso contrário, lista tudo
        const lista = slug
          ? response.data.filter(eq =>
            eq.categoria.toLowerCase() === slug.toLowerCase()
          )
          : response.data;
        setEquipamentos(lista);
      })
      .catch(err => {
        console.error(err);
        setError("Não foi possível carregar os equipamentos.");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    fetchEquipamentos();
    setBusca('');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]); // Recarrega sempre que mudar a categoria na URL

  return (
    <div className="container-catalogo">

      <div className="header-catalogo">
        <nav className="breadcrumb">
          <Link to="/">Início</Link>
          <span className="separador">›</span>
          <span className="pagina-atual">{nomeFormatado}</span>
        </nav>
        <div className="acoes-catalogo">
          <div className="campo-busca">
            <FontAwesomeIcon icon={faSearch} className="icone-busca" />
            <input
              type="text"
              className="input-busca"
              placeholder="Buscar equipamento..."
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
            />
          </div>
          <div className="filtros-wrapper">
            <button
              className={`btn-filtrar${filtrosAtivos > 0 ? ' com-filtro' : ''}`}
              onClick={() => setFiltrosAbertos(aberto => !aberto)}
            >
              <FontAwesomeIcon icon={faFilter} /> Filtrar{filtrosAtivos > 0 ? ` (${filtrosAtivos})` : ''}
            </button>
            {filtrosAbertos && (
              <>
                <div className="overlay-filtros" onClick={() => setFiltrosAbertos(false)} />
                <div className="painel-filtros">
                  <div className="painel-filtros-header">
                    <h4>Filtros</h4>
                    {filtrosAtivos > 0 && (
                      <button
                        type="button"
                        className="limpar-filtros"
                        onClick={() => {
                          setOrdenacao('relevancia');
                          setApenasDisponiveis(false);
                        }}
                      >
                        Limpar
                      </button>
                    )}
                  </div>
                  <p className="filtros-label">Ordenar por</p>
                  {OPCOES_ORDENACAO.map(op => (
                    <label key={op.valor} className="opcao-filtro">
                      <input
                        type="radio"
                        name="ordenacao-catalogo"
                        checked={ordenacao === op.valor}
                        onChange={() => setOrdenacao(op.valor)}
                      />
                      <span>{op.rotulo}</span>
                    </label>
                  ))}
                  <label className="opcao-filtro">
                    <input
                      type="checkbox"
                      checked={apenasDisponiveis}
                      onChange={(e) => setApenasDisponiveis(e.target.checked)}
                    />
                    <span>Somente equipamentos disponíveis</span>
                  </label>
                  <button
                    type="button"
                    className="aplicar-filtros"
                    onClick={() => setFiltrosAbertos(false)}
                  >
                    Aplicar
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="grid-produtos">
        {loading ? (
          <div className="loading-container">Carregando equipamentos...</div>
        ) : error ? (
          <div className="error-container">{error}</div>
        ) : equipamentosFiltrados.length > 0 ? (
          equipamentosFiltrados.map(item => (
            <div key={item.id} className="card-produto">
              <Link to={`/productview/${item.id}`} className="btn-card">
                <div className="image-container">
                  {item.imagens && item.imagens.length > 0 && item.imagens[0]?.url ? (
                    <img
                      src={imageUrl(item.imagens[0].url)}
                      alt={item.nome}
                      className="img-produto-cat"
                      style={{ objectPosition: objectPositionDe(item.imagens[0]) }}
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.style.display = 'none';
                        e.target.parentElement.innerHTML = '<div class="placeholder-img">🏗️</div>';
                      }}
                    />
                  ) : (
                    <div className="placeholder-img">🏗️</div>
                  )}
                </div>

                <h3>{item.nome}</h3>

                {item.totalAvaliacoes > 0 && (
                  <div className="avaliacao-card">
                    <Estrelas valor={item.mediaAvaliacoes} tamanho={12} />
                    <span>{formatarMedia(item.mediaAvaliacoes)} ({item.totalAvaliacoes})</span>
                  </div>
                )}

                <p className="descricao-produto">
                  {item.descricao || "Sem descrição disponível."}
                </p>

                <p className="preco-diaria">
                  <span className='valor'>R$ {item.valorDiaria?.toFixed(2)}</span>
                </p>

              </Link>
            </div>
          ))
        ) : (
          <div className="vazio">
            {termoBusca ? (
              <p>Nenhum equipamento encontrado para <strong>"{busca}"</strong>.</p>
            ) : apenasDisponiveis ? (
              <p>Nenhum equipamento disponível com os filtros aplicados.</p>
            ) : isCatalogoCompleto ? (
              <p>Nenhum equipamento disponível no momento.</p>
            ) : (
              <p>Nenhum equipamento disponível em <strong>{nomeFormatado}</strong> no momento.</p>
            )}
          </div>
        )}
      </div>

      <Lightbox src={lightboxSrc} onClose={() => setLightboxSrc(null)} />
    </div>
  );
}

export default Catalogo;