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

function FuncionariosModal({ open, onClose, editingId, form, onChange, onEnderecoChange, onSubmit, onReset, submitting, cargos, departamentos, depositos }) {
    if (!open) return null;

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{editingId ? 'Editar funcionario' : 'Cadastrar funcionario'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>Fechar</button>
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
                            <FormField label="Matricula">
                                <input className="equipInput" name="matricula" placeholder="Matricula" value={form.matricula} onChange={onChange} required />
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
                                <select className="equipInput" name="cargoId" value={form.cargoId} onChange={onChange}>
                                    <option value="">Selecione o cargo</option>
                                    {cargos.map(c => (
                                        <option key={c.id} value={c.id}>{c.nome}</option>
                                    ))}
                                </select>
                            </FormField>
                            <FormField label="Departamento">
                                <select className="equipInput" name="departamentoId" value={form.departamentoId} onChange={onChange}>
                                    <option value="">Selecione o departamento</option>
                                    {departamentos.map(d => (
                                        <option key={d.id} value={d.id}>{d.nome}</option>
                                    ))}
                                </select>
                            </FormField>
                            <FormField label="Depósito">
                                <select className="equipInput" name="depositoId" value={form.depositoId} onChange={onChange}>
                                    <option value="">Sem depósito</option>
                                    {depositos.map(dep => (
                                        <option key={dep.id} value={dep.id}>{dep.nome}</option>
                                    ))}
                                </select>
                            </FormField>
                            <FormField label="Salario (R$)">
                                <input
                                    className="equipInput"
                                    name="salario"
                                    placeholder="0,00"
                                    value={form.salario}
                                    onChange={onChange}
                                    readOnly={Boolean(form.cargoId && cargos.find(c => String(c.id) === String(form.cargoId))?.salarioPadrao != null)}
                                    title={form.cargoId ? "Definido automaticamente pelo cargo selecionado" : "Defina o salario"}
                                />
                                {form.cargoId && (
                                    <small style={{ color: '#888', fontSize: '0.75rem' }}>
                                        Definido pelo cargo selecionado
                                    </small>
                                )}
                            </FormField>
                        </div>
<div className="formRow split-2">
                            <FormField label="Data de Nascimento">
                                <input className="equipInput" name="dataNascimento" type="date" value={form.dataNascimento} onChange={onChange} />
                            </FormField>
                            <FormField label="Data de Admissao">
                                <input className="equipInput" name="dataAdmissao" type="date" value={form.dataAdmissao} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow split-2">
                            <FormField label="Data de Demissao">
                                <input className="equipInput" name="dataDemissao" type="date" value={form.dataDemissao} onChange={onChange} />
                            </FormField>
                            <FormField label="Senha (inicial)">
                                <input className="equipInput" name="senha" type="password" placeholder="Deixe em branco para 123456" value={form.senha} onChange={onChange} />
                            </FormField>
                        </div>

                        <div className="formRow full">
                            <FormField label="Endereço (opcional)">
                                <div className="enderecoGrid">
                                    <input className="equipInput" name="cep" placeholder="CEP" value={form.endereco?.cep || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="rua" placeholder="Rua / Logradouro" value={form.endereco?.rua || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="numero" placeholder="Número" value={form.endereco?.numero || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="complemento" placeholder="Complemento" value={form.endereco?.complemento || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="bairro" placeholder="Bairro" value={form.endereco?.bairro || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="cidade" placeholder="Cidade" value={form.endereco?.cidade || ''} onChange={onEnderecoChange} />
                                    <input className="equipInput" name="estado" placeholder="UF (2 letras)" maxLength={2} value={form.endereco?.estado || ''} onChange={onEnderecoChange} />
                                </div>
                            </FormField>
                        </div>
                    </div>

                    <label className="checkboxRow">
                        <input type="checkbox" name="status" checked={Boolean(form.status)} onChange={onChange} />
                        Funcionario ativo
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

const enderecoVazio = { cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '' };

const initialForm = {
    nome: '',
    cpf: '',
    matricula: '',
    email: '',
    telefone: '',
    cargoId: '',
    departamentoId: '',
    salario: '',
    dataNascimento: '',
    dataAdmissao: '',
    dataDemissao: '',
    status: true,
    senha: '',
    depositoId: '',
    endereco: { ...enderecoVazio }
};
export default function Funcionarios() {
    const { user } = useAuth();
    const [funcionarios, setFuncionarios] = useState([]);
    const [usuarios, setUsuarios] = useState([]);
    const [cargos, setCargos] = useState([]);
    const [departamentos, setDepartamentos] = useState([]);
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
        Promise.all([
            api.get('/api/funcionarios'),
            api.get('/api/usuarios'),
            api.get('/api/cargos'),
            api.get('/api/departamentos'),
            api.get('/api/depositos')
        ])
            .then(([funcResp, userResp, cargosResp, deptResp, depResp]) => {
                setFuncionarios(funcResp.data || []);
                setUsuarios(userResp.data || []);
                setCargos(cargosResp.data || []);
                setDepartamentos(deptResp.data || []);
                setDepositos(depResp.data || []);
            })
            .catch(err => {
                console.error(err);
                setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data?.message || err.message) });
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
                funcionario.matricula?.toLowerCase().includes(term) ||
                funcionario.cpf?.toLowerCase().includes(term)
            );
        });
    }, [funcionarios, searchTerm]);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        const newValue = type === 'checkbox' ? checked : value;

        setForm(prev => {
            const next = { ...prev, [name]: newValue };

            // Quando selecionar um cargo, preencher automaticamente
            // o salário padrão e o departamento vinculado ao cargo.
            if (name === 'cargoId' && value) {
                const cargoSelecionado = cargos.find(c => String(c.id) === String(value));
                if (cargoSelecionado) {
                    if (cargoSelecionado.salarioPadrao != null) {
                        next.salario = String(cargoSelecionado.salarioPadrao);
                    }
                    if (cargoSelecionado.departamentoId) {
                        next.departamentoId = String(cargoSelecionado.departamentoId);
                    }
                }
            }

            return next;
        });
    }

    // Edita os campos aninhados de form.endereco (vira linha própria na tabela
    // enderecos, referenciada por FK pelo backend).
    function handleEnderecoChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, endereco: { ...(prev.endereco || {}), [name]: value } }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingId(null);
        setModalOpen(false);
        setMessage(null);
    }

    function handleEdit(funcionario) {
        const end = funcionario.endereco || {};
        setEditingId(funcionario.id);
        setModalOpen(true);
        setForm({
            nome: funcionario.nome || '',
            cpf: funcionario.cpf || '',
            matricula: funcionario.matricula || '',
            email: '',
            telefone: funcionario.telefone || '',
            cargoId: funcionario.cargoId || '',
            departamentoId: funcionario.departamentoId || '',
            salario: funcionario.salario ?? '',
            dataNascimento: funcionario.dataNascimento ? funcionario.dataNascimento.slice(0, 10) : '',
            dataAdmissao: funcionario.dataAdmissao ? funcionario.dataAdmissao.slice(0, 10) : '',
            dataDemissao: funcionario.dataDemissao ? funcionario.dataDemissao.slice(0, 10) : '',
            status: funcionario.status ?? true,
            senha: '',
            depositoId: funcionario.depositoId || '',
            endereco: {
                cep: end.cep || '',
                rua: end.rua || '',
                numero: end.numero || '',
                complemento: end.complemento || '',
                bairro: end.bairro || '',
                cidade: end.cidade || '',
                estado: end.estado || '',
            }
        });
    }
function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        setMessage({ type: 'info', text: editingId ? 'Atualizando funcionario...' : 'Salvando funcionario...' });

        const end = form.endereco || {};
        const payload = {
            ...form,
            status: Boolean(form.status),
            cargoId: form.cargoId ? Number(form.cargoId) : null,
            departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
            depositoId: form.depositoId ? Number(form.depositoId) : 0,
            salario: form.salario ? form.salario.toString() : '',
            endereco: {
                cep: (end.cep || '').trim() || null,
                rua: (end.rua || '').trim() || null,
                numero: (end.numero || '').trim() || null,
                complemento: (end.complemento || '').trim() || null,
                bairro: (end.bairro || '').trim() || null,
                cidade: (end.cidade || '').trim() || null,
                estado: (end.estado || '').trim() || null,
            },
        };

        const request = editingId
            ? api.put(`/api/funcionarios/${editingId}`, payload)
            : api.post('/api/funcionarios', payload);

        request
            .then(() => {
                setMessage({ type: 'success', text: editingId ? 'Funcionario atualizado com sucesso!' : 'Funcionario criado com sucesso!' });
                resetForm();
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao salvar funcionario: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }

    function handleDelete(id) {
        if (!window.confirm('Tem certeza que deseja excluir este funcionario?')) return;
        setSubmitting(true);
        api.delete(`/api/funcionarios/${id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Funcionario excluido com sucesso!' });
                fetchData();
            })
            .catch(err => {
                setMessage({ type: 'error', text: 'Erro ao excluir: ' + (err.response?.data?.message || err.message) });
            })
            .finally(() => setSubmitting(false));
    }
return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestao de Funcionarios</h2>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastro de funcionarios</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Cadastre, edite ou gerencie os funcionarios da empresa.
                </p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => {
                        setEditingId(null);
                        setForm(initialForm);
                        setModalOpen(true);
                    }}>
                        Novo Funcionario
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Planilha de Funcionarios</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar funcionario..."
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
                                <th>Matricula</th>
                                <th>CPF</th>
                                <th>Cargo</th>
                                <th>Departamento</th>
                                <th>Depósito</th>
                                <th>Endereço</th>
                                <th>Usuario</th>
                                <th>Status</th>
                                <th>Acoes</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredFuncionarios.map(funcionario => {
                                const usuarioAssociado = usuarios.find(u => u.idFuncionario === funcionario.id);
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
                                        <td>{funcionario.cargoNome || '---'}</td>
                                        <td>{funcionario.departamentoNome || '---'}</td>
                                        <td>{funcionario.depositoNome || '---'}</td>
                                        <td>{funcionario.endereco?.formatado || '---'}</td>
                                        <td>{usuarioAssociado ? usuarioAssociado.nome : 'Sem vinculo'}</td>
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
                onEnderecoChange={handleEnderecoChange}
                onSubmit={handleSubmit}
                onReset={resetForm}
                submitting={submitting}
                cargos={cargos}
                departamentos={departamentos}
                depositos={depositos}
            />
        </div>
    );
}