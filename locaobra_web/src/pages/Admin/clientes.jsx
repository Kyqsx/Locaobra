import React, { useEffect, useState, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Clientes.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faTrash, faTools, faFileImport, faList } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

export default function Clientes() {
    const { user } = useAuth();
    const [clientes, setClientes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [form, setForm] = useState({
        nome: '',
        telefone: '',
        cpf_cnpj: '',
    });
    const [message, setMessage] = useState(null);

    // Excluir cliente apaga histórico — restrito, espelhando o backend
    // (DELETE /api/clientes/** só ADMIN/RH/GERENTE_OPERACOES).
    const canDeleteCliente = user?.tipo === 'ADMIN' ||
        ['RH', 'GERENTE_OPERACOES'].includes(user?.cargoFuncionario);

    useEffect(() => {
        fetchList();
    }, []);

    function fetchList() {
        setLoading(true);
        console.log("📥 Buscando clientes...");
        api.get('/api/clientes')
            .then(response => {
                console.log("✅ Clientes recebidos:", response.data);
                setClientes(response.data);
            })
            .catch(err => {
                console.error("❌ Erro ao buscar clientes:", err.response?.status, err.response?.data);
                setMessage({ type: 'error', text: 'Erro ao buscar: ' + (err.response?.data || err.message) });
            })
            .finally(() => setLoading(false));
    }

    // Função de Deleção
    function handleDelete(id) {
        if (window.confirm('Tem certeza que deseja excluir este cliente?')) {
            api.delete(`/api/clientes/${id}`)
                .then(() => {
                    setMessage({ type: 'success', text: 'Cliente excluído com sucesso!' });
                    fetchList(); // Atualiza a planilha
                })
                .catch(err => {
                    setMessage({ type: 'error', text: 'Erro ao excluir: ' + (err.response?.data || err.message) });
                });
        }
    }

    if (!canAccessAdminRoute(user, '/admin/clientes')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredClientes = useMemo(() => {
        return clientes.filter(cli =>
            cli.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (cli.cpfCnpj && cli.cpfCnpj.toLowerCase().includes(searchTerm.toLowerCase()))
        );
    }, [clientes, searchTerm]);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setMessage({ type: 'info', text: 'Enviando...' });

        const data = {
            nome: form.nome,
            telefone: form.telefone,
            cpfCnpj: form.cpf_cnpj,
        };

        console.log("📤 Enviando dados:", data);
        console.log("📋 Token no localStorage:", localStorage.getItem('token'));
        console.log("📋 Headers da requisição:", api.defaults.headers);

        api.post('/api/clientes', data)
            .then(() => {
                setMessage({ type: 'success', text: 'Cliente criado com sucesso!' });
                setForm({ nome: '', telefone: '', cpf_cnpj: '' });
                e.target.reset();
                fetchList();
            })
            .catch(err => {
                console.error("❌ Erro completo:", err);
                console.error("❌ Response:", err.response?.data);
                setMessage({ type: 'error', text: 'Erro ao criar: ' + (err.response?.data?.message || err.response?.data || err.message) });
            });
    }

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Clientes</h2>
                <div className="headerRight">

                </div>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastrar Cliente</h3>
                <form onSubmit={handleSubmit} className="equipForm">
                    <div className="formGrid">
                        <input className="equipInput" name="nome" placeholder="Nome do Cliente" value={form.nome} onChange={handleChange} required />
                        <input className="equipInput" name="telefone" placeholder="Telefone" value={form.telefone} onChange={handleChange} required />
                        <input className="equipInput" name="cpf_cnpj" placeholder="CPF/CNPJ" value={form.cpf_cnpj} onChange={handleChange} required />
                    </div>


                    <div className="formFooter">
                        <button type="submit" className="addBtn">Salvar</button>
                    </div>
                </form>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Planilha de Cadastrados</h3>
                    <div className="headerRight">
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar cliente..."
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
                                <th>Telefone</th>
                                <th>CPF / CNPJ</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredClientes.map(cli => (
                                <tr key={cli.id} className="tableRow">
                                    <td className="nameCell">
                                        <div className="userCell">
                                            <div className="userCellAvatar">{cli.nome.charAt(0)}</div>
                                            <div>
                                                <div className="userName">{cli.nome}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td><span className="">{cli.telefone}</span></td>
                                    <td className="dateCell">{cli.cpfCnpj || '---'}</td>
                                    <td className="actionsCell">
                                        {/* Botão de Delete Vinculado */}
                                        {canDeleteCliente && (
                                            <button
                                                className="actionBtn delete"
                                                onClick={() => handleDelete(cli.id)}
                                                title="Excluir"
                                            >
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}