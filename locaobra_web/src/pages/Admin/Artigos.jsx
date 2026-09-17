import React, { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Artigos.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faEdit, faTrash, faList, faNewspaper, faImage } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

function imageUrl(path) {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
}

function FormField({ label, children }) {
    return (
        <div className="formField">
            {label && <label className="fieldLabel">{label}</label>}
            {children}
        </div>
    );
}

const initialForm = {
    titulo: '',
    slug: '',
    resumo: '',
    conteudo: '',
    autor: '',
    publicado: true,
};

function ArtigoModal({ open, onClose, editingId, form, onChange, onSubmit, capaAtual, novaCapa, onCapaChange, submitting }) {
    if (!open) return null;

    const previewUrl = novaCapa ? URL.createObjectURL(novaCapa) : imageUrl(capaAtual);

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar artigo' : 'Novo artigo'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                <form onSubmit={onSubmit} className="equipForm">
                    <div className="formGridArtigos">
                        <div className="formRow full">
                            <FormField label="Título">
                                <input className="equipInput" name="titulo" placeholder="Ex: 5 dicas para economizar no aluguel de equipamentos" value={form.titulo} onChange={onChange} required />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Slug (URL amigável — opcional, gerado a partir do título se vazio)">
                                <input className="equipInput" name="slug" placeholder="Ex: dicas-economizar-aluguel" value={form.slug} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Resumo (aparece nos cards da home e da listagem)">
                                <textarea className="equipInput textareaInput" name="resumo" placeholder="Resumo curto do artigo" value={form.resumo} onChange={onChange} rows={2} maxLength={500} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Conteúdo">
                                <textarea className="equipInput textareaInput" name="conteudo" placeholder="Texto completo do artigo" value={form.conteudo} onChange={onChange} rows={10} required />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Autor">
                                <input className="equipInput" name="autor" placeholder="Nome do autor" value={form.autor} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Imagem de capa">
                                <div className="capaPickerWrapper">
                                    {previewUrl ? (
                                        <img className="capaPreview" src={previewUrl} alt="Prévia da capa" />
                                    ) : (
                                        <div className="capaPlaceholder"><FontAwesomeIcon icon={faImage} /></div>
                                    )}
                                    <input type="file" accept="image/*" onChange={(e) => onCapaChange(e.target.files?.[0] || null)} />
                                </div>
                            </FormField>
                        </div>
                    </div>

                    <label className="checkboxRow">
                        <input type="checkbox" name="publicado" checked={Boolean(form.publicado)} onChange={onChange} />
                        Publicado (visível no site)
                    </label>

                    <div className="formFooter">
                        <button type="submit" className="addBtn" disabled={submitting}>
                            {submitting ? 'Salvando...' : 'Salvar'}
                        </button>
                        <button type="button" className="secondaryBtn" onClick={onClose}>Cancelar</button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default function Artigos() {
    const { user } = useAuth();
    const [artigos, setArtigos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [form, setForm] = useState(initialForm);
    const [editingId, setEditingId] = useState(null);
    const [capaAtual, setCapaAtual] = useState(null);
    const [novaCapa, setNovaCapa] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        fetchData();
    }, []);

    function fetchData() {
        setLoading(true);
        api.get('/api/artigos')
            .then(response => setArtigos(response.data || []))
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar artigos: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/artigos')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredArtigos = useMemo(() => {
        const term = searchTerm.toLowerCase();
        return artigos.filter(a =>
            a.titulo?.toLowerCase().includes(term) ||
            a.autor?.toLowerCase().includes(term)
        );
    }, [artigos, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setCapaAtual(null);
        setNovaCapa(null);
        setModalOpen(false);
    }

    function handleEdit(artigo) {
        setEditingId(artigo.id);
        setForm({
            titulo: artigo.titulo || '',
            slug: artigo.slug || '',
            resumo: artigo.resumo || '',
            conteudo: artigo.conteudo || '',
            autor: artigo.autor || '',
            publicado: artigo.publicado ?? true,
        });
        setCapaAtual(artigo.imagemCapa || null);
        setNovaCapa(null);
        setModalOpen(true);
    }

    function handleDelete(id) {
        if (!window.confirm('Tem certeza que deseja excluir este artigo?')) return;
        api.delete(`/api/artigos/${id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Artigo excluído com sucesso!' });
                if (editingId === id) resetForm();
                fetchData();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao excluir: ' + (err.response?.data?.message || err.message) }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando artigo...' : 'Salvando artigo...' });

        const payload = {
            titulo: form.titulo,
            slug: form.slug || null,
            resumo: form.resumo,
            conteudo: form.conteudo,
            autor: form.autor,
            publicado: Boolean(form.publicado),
        };

        const formData = new FormData();
        formData.append('artigo', JSON.stringify(payload));
        if (novaCapa) formData.append('imagemCapa', novaCapa);

        const request = editingId
            ? api.put(`/api/artigos/${editingId}`, formData)
            : api.post('/api/artigos', formData);

        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Artigo atualizado com sucesso!' : 'Artigo criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao salvar artigo: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Artigos</h2>
                <div className="headerRight"></div>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de artigos</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Crie e gerencie os artigos do blog exibidos na home e na página "Dicas LocaObra".
                </p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => {
                        setEditingId(null);
                        setForm(initialForm);
                        setCapaAtual(null);
                        setNovaCapa(null);
                        setModalOpen(true);
                    }}>
                        Novo Artigo
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Lista de Artigos</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar artigo..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>
                    </div>
                </div>

                <div className="tableWrapper">
                    <table className="usersTable">
                        <thead>
                            <tr>
                                <th>Artigo</th>
                                <th>Autor</th>
                                <th>Status</th>
                                <th>Criado em</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredArtigos.map(a => (
                                <tr key={a.id} className="tableRow">
                                    <td className="nameCell">
                                        <div className="userCell">
                                            {a.imagemCapa ? (
                                                <img className="artigoThumb" src={imageUrl(a.imagemCapa)} alt={a.titulo} />
                                            ) : (
                                                <div className="userCellAvatar"><FontAwesomeIcon icon={faNewspaper} /></div>
                                            )}
                                            <div>
                                                <div className="userName">{a.titulo}</div>
                                                <div className="userRole">/blog/{a.slug}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>{a.autor || '—'}</td>
                                    <td>
                                        <span className={`typeTag ${a.publicado ? 'cliente' : 'fornecedor'}`}>
                                            {a.publicado ? 'Publicado' : 'Rascunho'}
                                        </span>
                                    </td>
                                    <td>{a.criadoEm ? new Date(a.criadoEm).toLocaleDateString('pt-BR') : '—'}</td>
                                    <td className="actionsCell">
                                        <button className="actionBtn edit" onClick={() => handleEdit(a)} title="Editar">
                                            <FontAwesomeIcon icon={faEdit} />
                                        </button>
                                        <button className="actionBtn delete" onClick={() => handleDelete(a.id)} title="Excluir">
                                            <FontAwesomeIcon icon={faTrash} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!loading && filteredArtigos.length === 0 && (
                                <tr className="tableRow">
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#999' }}>Nenhum artigo cadastrado ainda</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <ArtigoModal
                open={modalOpen}
                onClose={resetForm}
                editingId={editingId}
                form={form}
                onChange={handleChange}
                onSubmit={handleSubmit}
                capaAtual={capaAtual}
                novaCapa={novaCapa}
                onCapaChange={setNovaCapa}
                submitting={submitting}
            />
        </div>
    );
}
