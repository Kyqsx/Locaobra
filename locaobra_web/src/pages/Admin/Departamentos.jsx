import React, { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Departamentos.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faEdit, faList, faBuilding } from '@fortawesome/free-solid-svg-icons';
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

function DepartamentosModal({ open, onClose, editingId, form, onChange, onSubmit, onReset, submitting }) {
    if (!open) return null;

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar departamento' : 'Cadastrar departamento'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                <form onSubmit={onSubmit} className="equipForm">
                    <div className="formGridDepartamentos">
                        <div className="formRow full">
                            <FormField label="Nome do departamento">
                                <input className="equipInput" name="nome" placeholder="Ex: LOGISTICA_E_ALMOXARIFADO" value={form.nome} onChange={onChange} required />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Descrição">
                                <textarea className="equipInput textareaInput" name="descricao" placeholder="Descrição do departamento" value={form.descricao} onChange={onChange} rows={3} />
                            </FormField>
                        </div>
                    </div>

                    <label className="checkboxRow">
                        <input type="checkbox" name="ativo" checked={Boolean(form.ativo)} onChange={onChange} />
                        Departamento ativo
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
    descricao: '',
    ativo: true
};
export default function Departamentos() {
    const { user } = useAuth();
    const [departamentos, setDepartamentos] = useState([]);
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
        api.get('/api/departamentos')
            .then(response => {
                setDepartamentos(response.data || []);
            })
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/departamentos')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredDepts = useMemo(() => {
        return departamentos.filter(d => {
            const term = searchTerm.toLowerCase();
            return (
                d.nome?.toLowerCase().includes(term) ||
                d.descricao?.toLowerCase().includes(term)
            );
        });
    }, [departamentos, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setModalOpen(false);
    }

    function handleEdit(dept) {
        setEditingId(dept.id);
        setModalOpen(true);
        setForm({
            nome: dept.nome || '',
            descricao: dept.descricao || '',
            ativo: dept.ativo ?? true
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando departamento...' : 'Salvando departamento...' });

        const payload = {
            ...form,
            ativo: Boolean(form.ativo)
        };

        const request = editingId
            ? api.put(`/api/departamentos/${editingId}`, payload)
            : api.post('/api/departamentos', payload);

        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Departamento atualizado com sucesso!' : 'Departamento criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao salvar departamento: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }
return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Departamentos</h2>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de departamentos</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Configure os departamentos da empresa para organizar os setores e vincular aos cargos.
                </p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => {
                        setEditingId(null);
                        setForm(initialForm);
                        setModalOpen(true);
                    }}>
                        Novo Departamento
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Lista de Departamentos</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar departamento..."
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
                                <th>Descrição</th>
                                <th>Ativo</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredDepts.map(d => (
                                <tr key={d.id} className="tableRow">
                                    <td><strong>{d.nome}</strong></td>
                                    <td>{d.descricao || '---'}</td>
                                    <td>{d.ativo ? 'Sim' : 'Não'}</td>
                                    <td className="actionsCell">
                                        <button className="actionBtn edit" title="Editar" onClick={() => handleEdit(d)}>
                                            <FontAwesomeIcon icon={faEdit} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            <DepartamentosModal
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