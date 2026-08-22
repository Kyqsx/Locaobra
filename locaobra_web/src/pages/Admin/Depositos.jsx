import React, { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Depositos.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faEdit, faTrash, faList, faWarehouse } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

function FormField({ label, children }) {
    return (
        <div className="formField">
            {label && <label className="fieldLabel">{label}</label>}
            {children}
        </div>
    );
}

function DepositosModal({ open, onClose, editingId, form, onChange, onSubmit, onReset, submitting }) {
    if (!open) return null;

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar depósito' : 'Cadastrar depósito'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                <form onSubmit={onSubmit} className="equipForm">
                    <div className="formGridDepositos">
                        <div className="formRow full">
                            <FormField label="Nome do depósito">
                                <input className="equipInput" name="nome" placeholder="Ex: Galpão Central - Osasco" value={form.nome} onChange={onChange} required />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Endereço">
                                <input className="equipInput" name="endereco" placeholder="Endereço completo" value={form.endereco} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Descrição">
                                <textarea className="equipInput textareaInput" name="descricao" placeholder="Observações sobre o depósito" value={form.descricao} onChange={onChange} rows={3} />
                            </FormField>
                        </div>
                    </div>

                    <label className="checkboxRow">
                        <input type="checkbox" name="ativo" checked={Boolean(form.ativo)} onChange={onChange} />
                        Depósito ativo
                    </label>

                    <div className="formFooter">
                        <button type="submit" className="addBtn" disabled={submitting}>
                            {submitting ? 'Salvando...' : 'Salvar'}
                        </button>
                        {editingId && (
                            <button type="button" className="secondaryBtn" onClick={onReset}>Cancelar</button>
                        )}
                    </div>
                </form>
            </div>
        </div>
    );
}

const initialForm = {
    nome: '',
    endereco: '',
    descricao: '',
    ativo: true
};

export default function Depositos() {
    const { user } = useAuth();
    const [depositos, setDepositos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [form, setForm] = useState(initialForm);
    const [editingId, setEditingId] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        fetchData();
    }, []);

    function fetchData() {
        setLoading(true);
        api.get('/api/depositos')
            .then(response => {
                setDepositos(response.data || []);
            })
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/depositos')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredDepositos = useMemo(() => {
        return depositos.filter(d => {
            const term = searchTerm.toLowerCase();
            return (
                d.nome?.toLowerCase().includes(term) ||
                d.endereco?.toLowerCase().includes(term) ||
                d.descricao?.toLowerCase().includes(term)
            );
        });
    }, [depositos, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setModalOpen(false);
    }

    function handleEdit(deposito) {
        setEditingId(deposito.id);
        setModalOpen(true);
        setForm({
            nome: deposito.nome || '',
            endereco: deposito.endereco || '',
            descricao: deposito.descricao || '',
            ativo: deposito.ativo ?? true
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando depósito...' : 'Salvando depósito...' });

        const payload = {
            ...form,
            ativo: Boolean(form.ativo)
        };

        const request = editingId
            ? api.put(`/api/depositos/${editingId}`, payload)
            : api.post('/api/depositos', payload);

        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Depósito atualizado com sucesso!' : 'Depósito criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao salvar depósito: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }

    function handleDelete(deposito) {
        if (!window.confirm(`Excluir o depósito "${deposito.nome}"?`)) return;
        setMessage(null);
        api.delete(`/api/depositos/${deposito.id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Depósito excluído com sucesso!' });
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao excluir: ' + (err.response?.data?.message || err.message) });
            });
    }

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Depósitos</h2>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de depósitos</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Cadastre os galpões/pátios da empresa. Unidades de equipamento e funcionários de
                    logística são vinculados a um depósito específico — nunca ao modelo do equipamento.
                </p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => {
                        setEditingId(null);
                        setForm(initialForm);
                        setModalOpen(true);
                    }}>
                        Novo Depósito
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Lista de Depósitos</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar depósito..."
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
                                <th>Nome</th>
                                <th>Endereço</th>
                                <th>Unidades</th>
                                <th>Funcionários</th>
                                <th>Ativo</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredDepositos.map(d => (
                                <tr key={d.id} className="tableRow">
                                    <td><strong><FontAwesomeIcon icon={faWarehouse} /> {d.nome}</strong></td>
                                    <td>{d.endereco || '---'}</td>
                                    <td>{d.quantidadeUnidades ?? 0}</td>
                                    <td>{d.quantidadeFuncionarios ?? 0}</td>
                                    <td>{d.ativo ? 'Sim' : 'Não'}</td>
                                    <td className="actionsCell">
                                        <button className="actionBtn edit" title="Editar" onClick={() => handleEdit(d)}>
                                            <FontAwesomeIcon icon={faEdit} />
                                        </button>
                                        <button className="actionBtn delete" title="Excluir" onClick={() => handleDelete(d)}>
                                            <FontAwesomeIcon icon={faTrash} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!loading && filteredDepositos.length === 0 && (
                                <tr className="tableRow">
                                    <td colSpan="6" style={{ textAlign: 'center', color: '#999' }}>Nenhum depósito cadastrado ainda</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <DepositosModal
                open={modalOpen}
                onClose={resetForm}
                editingId={editingId}
                form={form}
                onChange={handleChange}
                onSubmit={handleSubmit}
                onReset={resetForm}
                submitting={submitting}
            />
        </div>
    );
}
