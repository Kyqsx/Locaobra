import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faHammer, faTruckLoading, faScrewdriverWrench, faLayerGroup, faChevronRight, faQuestionCircle, faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import './home.css';

const Home = () => {
    // Estado para controlar qual FAQ está aberto (armazena o ID ou null)
    const [openFaq, setOpenFaq] = useState(null);

    const toggleFaq = (index) => {
        setOpenFaq(openFaq === index ? null : index);
    };

    const categorias = [
        { id: 1, nome: 'Ferramentas Elétricas', icon: faScrewdriverWrench, slug: 'ferramentas-eletricas' },
        { id: 2, nome: 'Andaimes e Escadas', icon: faLayerGroup, slug: 'andaimes' },
        { id: 3, nome: 'Acesso e Elevação', icon: faTruckLoading, slug: 'acesso-elevacao' },
        { id: 4, nome: 'Equipamentos Pesados', icon: faHammer, slug: 'equipamentos-pesados' },
    ];

    const blogPosts = [
        {
            id: 1,
            title: '5 Ferramentas essenciais para começar sua obra',
            excerpt: 'Descubra quais itens não podem faltar no seu canteiro para evitar atrasos...',
            image: 'https://images.unsplash.com/photo-1581094288338-2314dddb79a5?auto=format&fit=crop&q=80&w=400'
        },
        {
            id: 2,
            title: 'Como economizar no aluguel de andaimes',
            excerpt: 'Planejar o tempo de uso pode reduzir custos em até 30% no seu projeto final...',
            image: 'https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=400'
        }
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
            <section className="section-padding bg-light">
                <div className="section-header">
                    <h2 className="section-title">Dicas LocaObra</h2>
                    <a href="/blog" className="view-more">Ver tudo <FontAwesomeIcon icon={faChevronRight} /></a>
                </div>
                <div className="blog-summary">
                    {blogPosts.map(post => (
                        <div key={post.id} className="blog-card">
                            <img src={post.image} alt={post.title} />
                            <div className="blog-info">
                                <h3>{post.title}</h3>
                                <p>{post.excerpt}</p>
                                <button className="read-more">Ler Artigo</button>
                            </div>
                        </div>
                    ))}
                </div>
            </section>

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