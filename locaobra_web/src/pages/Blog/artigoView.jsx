import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../service/api';
import './ArtigoPage.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCalendar, faUser, faNewspaper, faArrowLeft } from '@fortawesome/free-solid-svg-icons';

function imageUrl(path) {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
}

const ArtigoView = () => {
    const { slug } = useParams();
    const [artigo, setArtigo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!slug) return;
        setLoading(true);
        setError(null);
        api.get(`/api/artigos/slug/${slug}`)
            .then(response => setArtigo(response.data))
            .catch(() => setError('Não foi possível carregar o artigo.'))
            .finally(() => setLoading(false));
    }, [slug]);

    if (loading) {
        return <div className="loading-container">Carregando artigo...</div>;
    }

    if (error) {
        return <div className="error-container">{error}</div>;
    }

    if (!artigo) {
        return <div className="error-container">Artigo não encontrado.</div>;
    }

    return (
        <div className="artigo-wrapper">
            <div className="artigo-container">
                <nav className="breadcrumb">
                    <Link to="/">Início</Link>
                    <span className="separador">›</span>
                    <Link to="/blog">Dicas LocaObra</Link>
                    <span className="separador">›</span>
                    <span className="pagina-atual">{artigo.titulo}</span>
                </nav>

                <article className="artigo-conteudo">
                    <header className="artigo-header">
                        <h1 className="artigo-titulo">{artigo.titulo}</h1>
                        <div className="artigo-meta">
                            {artigo.autor && (
                                <span><FontAwesomeIcon icon={faUser} /> {artigo.autor}</span>
                            )}
                            {artigo.criadoEm && (
                                <span><FontAwesomeIcon icon={faCalendar} /> {new Date(artigo.criadoEm).toLocaleDateString('pt-BR')}</span>
                            )}
                        </div>
                    </header>

                    <div className="artigo-imagem-capa">
                        {artigo.imagemCapa ? (
                            <img src={imageUrl(artigo.imagemCapa)} alt={artigo.titulo} />
                        ) : (
                            <div className="artigo-imagem-placeholder"><FontAwesomeIcon icon={faNewspaper} /></div>
                        )}
                    </div>

                    {artigo.resumo && (
                        <p className="artigo-resumo">{artigo.resumo}</p>
                    )}

                    <div className="artigo-texto">
                        {artigo.conteudo.split(/\n{2,}/).map((paragrafo, i) => (
                            <p key={i}>{paragrafo}</p>
                        ))}
                    </div>
                </article>

                <Link to="/blog" className="voltar-blog">
                    <FontAwesomeIcon icon={faArrowLeft} /> Voltar para Dicas LocaObra
                </Link>
            </div>
        </div>
    );
};

export default ArtigoView;
