import React, { useEffect, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faHammer, faTruckLoading, faScrewdriverWrench, faLayerGroup, faChevronRight, faQuestionCircle, faChevronDown, faNewspaper } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import './home.css';
import { imageUrl } from '../../utils/imagem';

const Home = () => {
    // Estado para controlar qual FAQ está aberto (armazena o ID ou null)
    const [openFaq, setOpenFaq] = useState(null);
    const [blogPosts, setBlogPosts] = useState([]);

    const toggleFaq = (index) => {
        setOpenFaq(openFaq === index ? null : index);
    };

    useEffect(() => {
        api.get('/api/artigos', { params: { apenasPublicados: true } })
            .then(response => setBlogPosts((response.data || []).slice(0, 2)))
            .catch(() => setBlogPosts([]));
    }, []);

    const categorias = [
        { id: 1, nome: 'Ferramentas Elétricas', icon: faScrewdriverWrench, slug: 'ferramentas-eletricas' },
        { id: 2, nome: 'Andaimes e Escadas', icon: faLayerGroup, slug: 'andaimes' },
        { id: 3, nome: 'Acesso e Elevação', icon: faTruckLoading, slug: 'acesso-elevacao' },
        { id: 4, nome: 'Equipamentos Pesados', icon: faHammer, slug: 'equipamentos-pesados' },
    ];

    const faqs = [
        {
            pergunta: 'Como funciona o aluguel?',
            resposta: 'Você escolhe o equipamento pelo site, define o período de locação e nós entregamos diretamente no seu canteiro de obras ou você retira em uma de nossas unidades.'
        },
        {
            pergunta: 'Preciso pagar caução?',
            resposta: 'Sim, para equipamentos de alto valor solicitamos uma garantia (caução) que é estornada integralmente após a devolução do item em boas condições.'
        },
        {
            pergunta: 'E se o equipamento quebrar?',
            resposta: 'Oferecemos suporte técnico especializado. Caso ocorra uma falha por desgaste natural, realizamos a substituição do equipamento em até 24 horas.'
        }
    ];

    return (
        <div className="home-container">
            {/* 1. SEÇÃO BANNER PRINCIPAL */}
            <section className="hero-banner">
                <div className="hero-content">
                    <h1>Equipamento certo,<br />na hora certa.</h1>
                    <p>Alugue o que você precisa para construir o que você imagina.</p>
                    <Link to="/catalogo" className="cta-button">Ver Catálogo Completo</Link>
                </div>
            </section>

            {/* 2. CATEGORIAS RÁPIDAS */}
            <section className="section-padding">
                <h2 className="section-title">Navegue por Categorias</h2>
                <div className="categories-grid">
                    {categorias.map(cat => (
                        /* Envolva o card com o Link apontando para a rota dinâmica */
                        <Link
                            to={`/catalogo/${cat.slug}`}
                            key={cat.id}
                            className="category-card"
                            style={{ textDecoration: 'none', color: 'inherit' }} // Garante que o link não mude a cor do texto
                        >
                            <FontAwesomeIcon icon={cat.icon} className="cat-icon" />
                            <span>{cat.nome}</span>
                        </Link>
                    ))}
                </div>
            </section>

            {/* 3. RESUMO DO BLOG */}
            {blogPosts.length > 0 && (
                <section className="section-padding bg-light">
                    <div className="section-header">
                        <h2 className="section-title">Dicas LocaObra</h2>
                        <Link to="/blog" className="view-more">Ver tudo <FontAwesomeIcon icon={faChevronRight} /></Link>
                    </div>
                    <div className="blog-summary">
                        {blogPosts.map(post => (
                            <Link to={`/blog/${post.slug}`} key={post.id} className="blog-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                                {post.imagemCapa ? (
                                    <img src={imageUrl(post.imagemCapa)} alt={post.titulo} />
                                ) : (
                                    <div className="blog-img-placeholder"><FontAwesomeIcon icon={faNewspaper} /></div>
                                )}
                                <div className="blog-info">
                                    <h3>{post.titulo}</h3>
                                    <p>{post.resumo || 'Sem resumo disponível.'}</p>
                                    <span className="read-more">Ler Artigo</span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </section>
            )}

            {/* 4. PERGUNTAS FREQUENTES (FAQ) COM DROPDOWN */}
            <section className="section-padding faq-section">
                <h2 className="section-title">Dúvidas Frequentes</h2>
                <div className="faq-container">
                    {faqs.map((faq, index) => (
                        <div key={index} className={`faq-item ${openFaq === index ? 'active' : ''}`}>
                            <button className="faq-question" onClick={() => toggleFaq(index)}>
                                <span>
                                    <FontAwesomeIcon icon={faQuestionCircle} className="faq-icon-q" />
                                    {faq.pergunta}
                                </span>
                                <FontAwesomeIcon
                                    icon={faChevronDown}
                                    className={`faq-chevron ${openFaq === index ? 'rotate' : ''}`}
                                />
                            </button>
                            <div className="faq-answer">
                                <p>{faq.resposta}</p>
                            </div>
                        </div>
                    ))}
                </div>
            </section>
        </div>
    );
};

export default Home;