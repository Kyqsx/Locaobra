import React, { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Cargos.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faEdit, faList } from '@fortawesome/free-solid-svg-icons';
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

function CargosModal({ open, onClose, editingId, form, onChange, onSubmit, onReset, submitting, departamentos }) {
    if (!open) return null;
    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar cargo' : 'Cadastrar cargo'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>Fechar</button>
                </div>
                <form onSubmit={onSubmit} className="equipForm">
                    <div className="formGridCargos">
                        <div className="formRow full">
                            <FormField label="Nome do cargo">
                                <input className="equipInput" name="nome" placeholder="Ex: ENTREGADOR" value={form.nome} onChange={onChange} required />
                            </FormField>
                        </div>
                        <div className="formRow full">
                            <FormField label="Descricao">
                                <textarea className="equipInput textareaInput" name="descricao" placeholder="Descricao das atividades" value={form.descricao} onChange={onChange} rows={3} />
                            </FormField>
                        </div>
                        <div className="formRow split-2">
                            <FormField label="Salario padrao (R$)">
                                <input className="equipInput" name="salarioPadrao" type="number" step="0.01" placeholder="0,00" value={form.salarioPadrao} onChange={onChange} />
                            </FormField>
                            <FormField label="Departamento">
                                <select className="equipInput" name="departamentoId" value={form.departamentoId} onChange={onChange}>
                                    <option value="">Selecione...</option>
                                    {departamentos.map(d => (
                                        <option key={d.id} value={d.id}>{d.nome}</option>
                                    ))}
                                </select>
                            </FormField>
                        </div>
                        <div className="formRow full">
                            <FormField label="Requisitos">
                                <textarea className="equipInput textareaInput" name="requisitos" placeholder="Requisitos necessarios" value={form.requisitos} onChange={onChange} rows={3} />
                            </FormField>
                        </div>
                    </div>
                    <label className="checkboxRow">
                        <input type="checkbox" name="ativo" checked={Boolean(form.ativo)} onChange={onChange} />
                        Cargo ativo
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
    nome: '', descricao: '', salarioPadrao: '', departamentoId: '', requisitos: '', ativo: true
};export default function Cargos() {
    const { user } = useAuth();
    const [cargos, setCargos] = useState([]);
    const [departamentos, setDepartamentos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [form, setForm] = useState(initialForm);
    const [editingId, setEditingId] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => { fetchData(); }, []);

    function fetchData() {
        setLoading(true);
        Promise.all([
            api.get('/api/cargos'),
            api.get('/api/departamentos')
        ])
            .then(([cargosResponse, deptResponse]) => {
                setCargos(cargosResponse.data || []);
                setDepartamentos(deptResponse.data || []);
            })
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/cargos')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredCargos = useMemo(() => {
        return cargos.filter(cargo => {
            const term = searchTerm.toLowerCase();
            return cargo.nome?.toLowerCase().includes(term) || cargo.descricao?.toLowerCase().includes(term);
        });
    }, [cargos, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setModalOpen(false);
        setMessage(null);
    }

    function handleEdit(cargo) {
        setEditingId(cargo.id);
        setModalOpen(true);
        setForm({
            nome: cargo.nome || '',
            descricao: cargo.descricao || '',
            salarioPadrao: cargo.salarioPadrao ?? '',
            departamentoId: cargo.departamentoId || '',
            requisitos: cargo.requisitos || '',
            ativo: cargo.ativo ?? true
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando cargo...' : 'Salvando cargo...' });
        const payload = { ...form, ativo: Boolean(form.ativo), salarioPadrao: form.salarioPadrao ? parseFloat(form.salarioPadrao) : null, departamentoId: form.departamentoId ? Number(form.departamentoId) : null };
        const request = editingId ? api.put('/api/cargos/' + editingId, payload) : api.post('/api/cargos', payload);
        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Cargo atualizado com sucesso!' : 'Cargo criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao salvar cargo: ' + (err.response?.data?.message || err.message) }))
            .finally(() => setSubmitting(false));
    }

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestao de Cargos</h2>
            </div>
            {message && (
                <div className={'messageBanner ' + (message.type === 'error' ? 'negative' : 'positive')}>
                    {message.text}
                </div>
            )}
            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de cargos</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>Configure os cargos da empresa, definindo salario padrao, departamento vinculado e requisitos necessarios.</p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => { setEditingId(null); setForm(initialForm); setModalOpen(true); }}>Novo Cargo</button>
                </div>
            </div>
            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Lista de Cargos</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input className="searchInput" placeholder="Pesquisar cargo..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
                        </div>
                    </div>
                </div>
                <div className="tableWrapper">
                    <table className="usersTable">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Descricao</th>
                                <th>Departamento</th>
                                <th>Salario Padrao</th>
                                <th>Requisitos</th>
                                <th>Ativo</th>
                                <th>Acoes</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredCargos.map(cargo => (
                                <tr key={cargo.id} className="tableRow">
                                    <td><strong>{cargo.nome}</strong></td>
                                    <td>{cargo.descricao || '---'}</td>
                                    <td>{cargo.departamentoNome || '---'}</td>
                                    <td>{cargo.salarioPadrao ? 'R$ ' + cargo.salarioPadrao.toFixed(2) : '---'}</td>
                                    <td>{cargo.requisitos || '---'}</td>
                                    <td>{cargo.ativo ? 'Sim' : 'Nao'}</td>
                                    <td className="actionsCell">
                                        <button className="actionBtn edit" title="Editar" onClick={() => handleEdit(cargo)}>
                                            <FontAwesomeIcon icon={faEdit} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
            <CargosModal
                open={modalOpen} onClose={resetForm} editingId={editingId}
                form={form} onChange={handleChange} onSubmit={handleSubmit}
                onReset={resetForm} submitting={submitting} departamentos={departamentos}
            />
        </div>
    );
}