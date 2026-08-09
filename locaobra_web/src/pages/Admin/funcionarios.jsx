import React, { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Funcionarios.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faTrash, faEdit, faList, faUserTie } from '@fortawesome/free-solid-svg-icons';
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

function FuncionariosModal({ open, onClose, editingId, form, onChange, onSubmit, onReset, submitting }) {
    if (!open) return null;

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar funcionário' : 'Cadastrar funcionário'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                <form onSubmit={onSubmit} className="equipForm">
                    <div className="formGridFuncionarios">
                        <div className="formRow full">
                            <FormField label="Nome completo">
                                <input className="equipInput" name="nome" placeholder="Nome completo" value={form.nome} onChange={onChange} required />
                            </FormField>
                        </div>

                        <div className="formRow split-2">
                            <FormField label="CPF">
                                <input className="equipInput" name="cpf" placeholder="CPF" value={form.cpf} onChange={onChange} required />
                            </FormField>
                            <FormField label="Matrícula">
                                <input className="equipInput" name="matricula" placeholder="Matrícula" value={form.matricula} onChange={onChange} required />
                            </FormField>
                        </div>

                        <div className="formRow email-phone">
                            <FormField label="E-mail">
                                <input className="equipInput" name="email" type="email" placeholder="E-mail" value={form.email} onChange={onChange} required />
                            </FormField>
                            <FormField label="Telefone">
                                <input className="equipInput" name="telefone" placeholder="Telefone" value={form.telefone} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow split-3">
                            <FormField label="Cargo">
                                <select className="equipInput" name="cargo" value={form.cargo} onChange={onChange}>
                                    <option value="">Cargo</option>
                                    <option value="ENTREGADOR">Entregador</option>
                                    <option value="CONFERENTE">Conferente</option>
                                    <option value="TECNICO_MANUTENCAO">Técnico de Manutenção</option>
                                    <option value="FAXINE">Faxineiro</option>
                                    <option value="CONSULTOR_LOCACAO">Consultor de Locação</option>
                                    <option value="ANALISTA_CREDENCIAMENTO">Analista de Credenciamento</option>
                                    <option value="ANALISTA_FINANCEIRO">Analista Financeiro</option>
                                    <option value="GERENTE_OPERACOES">Gerente de Operações</option>
                                    <option value="RH">Recursos Humanos</option>
                                </select>
                            </FormField>
                            <FormField label="Departamento">
                                <select className="equipInput" name="departamento" value={form.departamento} onChange={onChange}>
                                    <option value="">Departamento</option>
                                    <option value="LOGISTICA_E_ALMOXARIFADO">Logística e Almoxarifado</option>
                                    <option value="MANUTENCAO_E_PATIO">Manutenção e Patio</option>
                                    <option value="COMERCIAL_E_ATENDIMENTO">Comercial e Atendimento</option>
                                    <option value="FINANCEIRO">Financeiro</option>
                                    <option value="OPERACOES">Operações</option>
                                    <option value="RECURSOS_HUMANOS">Recursos Humanos</option>
                                </select>
                            </FormField>
                            <FormField label="Salário">
                                <input className="equipInput" name="salario" type="number" step="0.01" placeholder="Salário" value={form.salario} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow split-3">
                            <FormField label="Data de nascimento">
                                <input className="equipInput" name="dataNascimento" type="date" value={form.dataNascimento} onChange={onChange} />
                            </FormField>
                            <FormField label="Data de admissão">
                                <input className="equipInput" name="dataAdmissao" type="date" value={form.dataAdmissao} onChange={onChange} />
                            </FormField>
                            <FormField label="Data de demissão">
                                <input className="equipInput" name="dataDemissao" type="date" value={form.dataDemissao} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label={editingId ? 'Senha (deixe em branco para manter)' : 'Senha de acesso'}>
                                <input className="equipInput" name="senha" type="password" placeholder={editingId ? 'Nova senha' : 'Senha de acesso'} value={form.senha} onChange={onChange} />
                            </FormField>
                        </div>
                    </div>

                    <label className="checkboxRow">
                        <input type="checkbox" name="status" checked={Boolean(form.status)} onChange={onChange} />
                        Funcionário ativo
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
    cpf: '',
    matricula: '',
    email: '',
    telefone: '',
    cargo: '',
    departamento: '',
    salario: '',
    dataNascimento: '',
    dataAdmissao: '',
    dataDemissao: '',
    status: true,
    senha: ''
};

export default function Funcionarios() {
    const { user } = useAuth();
    const [funcionarios, setFuncionarios] = useState([]);
    const [usuarios, setUsuarios] = useState([]);
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
        Promise.all([
            api.get('/api/funcionarios?apenasAtivos=false'),
            api.get('/api/usuarios')
        ])
            .then(([funcionariosResponse, usuariosResponse]) => {
                setFuncionarios(funcionariosResponse.data || []);
                setUsuarios(usuariosResponse.data || []);
            })
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar funcionários: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/funcionarios')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredFuncionarios = useMemo(() => {
        return funcionarios.filter(funcionario => {
            const term = searchTerm.toLowerCase();
            return (
                funcionario.nome?.toLowerCase().includes(term) ||
                funcionario.cpf?.toLowerCase().includes(term) ||
                funcionario.matricula?.toLowerCase().includes(term)
            );
        });
    }, [funcionarios, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setModalOpen(false);
    }

    function handleEdit(funcionario) {
        setEditingId(funcionario.id);
        setModalOpen(true);
        setForm({
            nome: funcionario.nome || '',
            cpf: funcionario.cpf || '',
            matricula: funcionario.matricula || '',
            email: funcionario.email || '',
            telefone: funcionario.telefone || '',
            cargo: funcionario.cargo || '',
            departamento: funcionario.departamento || '',
            salario: funcionario.salario ?? '',
            dataNascimento: funcionario.dataNascimento ? funcionario.dataNascimento.slice(0, 10) : '',
            dataAdmissao: funcionario.dataAdmissao ? funcionario.dataAdmissao.slice(0, 10) : '',
            dataDemissao: funcionario.dataDemissao ? funcionario.dataDemissao.slice(0, 10) : '',
            status: funcionario.status ?? true,
            senha: ''
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando funcionário...' : 'Salvando funcionário...' });

        const payload = {
            ...form,
            status: Boolean(form.status),
            salario: form.salario ? String(form.salario) : ''
        };

        const request = editingId
            ? api.put(`/api/funcionarios/${editingId}`, payload)
            : api.post('/api/funcionarios', payload);

        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Funcionário atualizado com sucesso!' : 'Funcionário criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao salvar funcionário: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }

    function handleDelete(id) {
        if (!window.confirm('Deseja excluir este funcionário?')) {
            return;
        }

        api.delete(`/api/funcionarios/${id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Funcionário excluído com sucesso!' });
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao excluir funcionário: ' + (err.response?.data?.message || err.message) });
            });
    }

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Funcionários</h2>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de funcionários</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>Cadastre ou edite funcionários em uma janela dedicada com campos identificados. O usuário de acesso é criado automaticamente.</p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => {
                        setEditingId(null);
                        setForm(initialForm);
                        setModalOpen(true);
                    }}>
                        Novo Funcionário
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Planilha de Funcionários</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar funcionário..."
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
                                <th>Matrícula</th>
                                <th>CPF</th>
                                <th>Usuário</th>
                                <th>Status</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredFuncionarios.map(funcionario => {
                                const usuarioAssociado = usuarios.find(usuario => usuario.idFuncionario === funcionario.id);
                                return (
                                    <tr key={funcionario.id} className="tableRow">
                                        <td className="nameCell">
                                            <div className="userCell">
                                                <div className="userCellAvatar">{funcionario.nome?.charAt(0) || 'F'}</div>
                                                <div>
                                                    <div className="userName">{funcionario.nome}</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>{funcionario.matricula || '---'}</td>
                                        <td>{funcionario.cpf || '---'}</td>
                                        <td>{usuarioAssociado ? usuarioAssociado.nome : 'Sem vínculo'}</td>
                                        <td>{funcionario.status ? 'Ativo' : 'Inativo'}</td>
                                        <td className="actionsCell">
                                            <button className="actionBtn edit" title="Editar" onClick={() => handleEdit(funcionario)}>
                                                <FontAwesomeIcon icon={faEdit} />
                                            </button>
                                            <button className="actionBtn delete" title="Excluir" onClick={() => handleDelete(funcionario.id)}>
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>

            <FuncionariosModal
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