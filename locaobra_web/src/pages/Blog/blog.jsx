import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import './Blog.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faNewspaper, faCalendar, faUser } from '@fortawesome/free-solid-svg-icons';
import { imageUrl } from '../../utils/imagem';

function Blog() {
    const [artigos, setArtigos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        setError(null);
        api.get('/api/artigos', { params: { apenasPublicados: true } })
            .then(response => setArtigos(response.data || []))
            .catch(() => setError('Não foi possível carregar os artigos.'))
            .finally(() => setLoading(false));
    }, []);

    return (
        <div className="container-blog">
            <div className="header-blog">
                <nav className="breadcrumb">
                    <Link to="/">Início</Link>
                    <span className="separador">›</span>
                    <span className="pagina-atual">Dicas LocaObra</span>
                </nav>
                <h1 className="titulo-pagina">Dicas LocaObra</h1>
                <p className="subtitulo-blog">Conteúdo para ajudar você a planejar sua obra e aproveitar melhor os equipamentos alugados.</p>
            </div>

            <div className="grid-blog">
                {loading ? (
                    <div className="loading-container">Carregando artigos...</div>
                ) : error ? (
                    <div className="error-container">{error}</div>
                ) : artigos.length > 0 ? (
                    artigos.map(artigo => (
                        <Link to={`/blog/${artigo.slug}`} key={artigo.id} className="card-blog">
                            <div className="image-container-blog">
                                {artigo.imagemCapa ? (
                                    <img src={imageUrl(artigo.imagemCapa)} alt={artigo.titulo} className="img-blog" />
                                ) : (
                                    <div className="placeholder-img"><FontAwesomeIcon icon={faNewspaper} /></div>
                                )}
                            </div>
                            <div className="blog-card-info">
                                <h3>{artigo.titulo}</h3>
                                <p className="resumo-blog">{artigo.resumo || 'Sem resumo disponível.'}</p>
                                <div className="meta-blog">
                                    {artigo.autor && (
                                        <span><FontAwesomeIcon icon={faUser} /> {artigo.autor}</span>
                                    )}
                                    {artigo.criadoEm && (
                                        <span><FontAwesomeIcon icon={faCalendar} /> {new Date(artigo.criadoEm).toLocaleDateString('pt-BR')}</span>
                                    )}
                                </div>
                            </div>
                        </Link>
                    ))
                ) : (
                    <div className="vazio">
                        <p>Nenhum artigo publicado no momento.</p>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Blog;
