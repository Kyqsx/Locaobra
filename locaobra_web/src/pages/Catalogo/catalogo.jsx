import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../service/api'; // Importe sua instância do axios
import './Catalogo.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFilter } from '@fortawesome/free-solid-svg-icons';

function Catalogo() {
  const { slug } = useParams();
  const [equipamentos, setEquipamentos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isCatalogoCompleto = !slug;
  const nomeFormatado = slug ? slug.replace(/-/g, ' ') : "Catálogo Completo";

  useEffect(() => {
    fetchEquipamentos();
  }, [slug]); // Recarrega sempre que mudar a categoria na URL

  const imageUrl = (path) => {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
  };

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

  return (
    <div className="container-catalogo">

      <div className="header-catalogo">
      <nav className="breadcrumb">
        <Link to="/">Início</Link>
        <span className="separador">›</span>
        <span className="pagina-atual">{nomeFormatado}</span>
      </nav>
        <button className="btn-filtrar">
          <FontAwesomeIcon icon={faFilter} /> Filtrar
        </button>
      </div>

      <div className="grid-produtos">
        {loading ? (
          <div className="loading-container">Carregando equipamentos...</div>
        ) : error ? (
          <div className="error-container">{error}</div>
        ) : equipamentos.length > 0 ? (
          equipamentos.map(item => (
            <div key={item.id} className="card-produto">
              <Link to={`/productview/${item.id}`} className="btn-card">
                <div className="image-container">
                  {item.imagens && item.imagens.length > 0 ? (
                    <img
                      src={imageUrl(item.imagens[0])}
                      alt={item.nome}
                      className="img-produto-cat"
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
            {isCatalogoCompleto ? (
              <p>Nenhum equipamento disponível no momento.</p>
            ) : (
              <p>Nenhum equipamento disponível em <strong>{nomeFormatado}</strong> no momento.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default Catalogo;