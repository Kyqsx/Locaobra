import React, { useEffect, useState, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Clientes.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faPlus, faTrash, faTools, faFileImport, faList, faLocationDot, faPen, faCoins } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';
import EnderecoFields from '../../components/EnderecoFields';

const enderecoVazio = { apelido: '', cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '', principal: false };

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
    const [enderecosForm, setEnderecosForm] = useState([{ ...enderecoVazio }]);
    const [message, setMessage] = useState(null);
    const [clienteEnderecos, setClienteEnderecos] = useState(null); // cliente sendo editado no modal de endereços
    const [clienteCredito, setClienteCredito] = useState(null); // cliente sendo visto/editado no modal de crédito

    // Excluir cliente apaga histórico — restrito, espelhando o backend
    // (DELETE /api/clientes/** só ADMIN/RH/GERENTE_OPERACOES).
    const canDeleteCliente = user?.tipo === 'ADMIN' ||
        ['RH', 'GERENTE_OPERACOES'].includes(user?.cargoFuncionario);

    // Só quem avalia o cliente edita o crédito — espelha o backend
    // (PATCH /api/clientes/{id}/credito), ver SecurityConfig.
    const canEditCredito = user?.tipo === 'ADMIN' ||
        ['ANALISTA_CREDENCIAMENTO', 'ANALISTA_FINANCEIRO'].includes(user?.cargoFuncionario);

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

    function handleEnderecoFormChange(index, novoValor) {
        setEnderecosForm(prev => prev.map((end, i) => (i === index ? novoValor : end)));
    }

    function adicionarLinhaEndereco() {
        setEnderecosForm(prev => [...prev, { ...enderecoVazio }]);
    }

    function removerLinhaEndereco(index) {
        setEnderecosForm(prev => prev.filter((_, i) => i !== index));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setMessage({ type: 'info', text: 'Enviando...' });

        const data = {
            nome: form.nome,
            telefone: form.telefone,
            cpfCnpj: form.cpf_cnpj,
            // Só manda endereços que o funcionário realmente preencheu (pelo
            // menos a rua) — linhas em branco deixadas ali são ignoradas.
            enderecos: enderecosForm.filter(end => end.rua && end.rua.trim()),
        };

        console.log("📤 Enviando dados:", data);

        api.post('/api/clientes', data)
            .then(() => {
                setMessage({ type: 'success', text: 'Cliente criado com sucesso!' });
                setForm({ nome: '', telefone: '', cpf_cnpj: '' });
                setEnderecosForm([{ ...enderecoVazio }]);
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

                    <div className="clienteEnderecosFormSection">
                        <div className="specsHeader">
                            <label><FontAwesomeIcon icon={faLocationDot} /> Endereço(s)</label>
                        </div>
                        {enderecosForm.map((endereco, index) => (
                            <div key={index} className="clienteEnderecoFormRow">
                                <EnderecoFields
                                    value={endereco}
                                    onChange={(novo) => handleEnderecoFormChange(index, novo)}
                                    showApelido
                                    showPrincipal={enderecosForm.length > 1}
                                    prefixo={`cliente-novo-${index}`}
                                />
                                {enderecosForm.length > 1 && (
                                    <button type="button" className="btnSmallDelete" onClick={() => removerLinhaEndereco(index)}>
                                        <FontAwesomeIcon icon={faTrash} /> Remover endereço
                                    </button>
                                )}
                            </div>
                        ))}
                        <button type="button" className="btnLinkAdd" onClick={adicionarLinhaEndereco}>
                            + Adicionar outro endereço
                        </button>
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
                                <th>Crédito</th>
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
                                    <td>
                                        <span className={`creditoBadge creditoBadge-${cli.situacaoCredito || 'EM_ANALISE'}`}>
                                            {formatarSituacaoCredito(cli.situacaoCredito)}
                                        </span>
                                    </td>
                                    <td className="actionsCell">
                                        <button
                                            className="actionBtn"
                                            onClick={() => setClienteCredito(cli)}
                                            title="Crédito"
                                        >
                                            <FontAwesomeIcon icon={faCoins} />
                                        </button>
                                        <button
                                            className="actionBtn"
                                            onClick={() => setClienteEnderecos(cli)}
                                            title="Endereços"
                                        >
                                            <FontAwesomeIcon icon={faLocationDot} />
                                        </button>
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

            {clienteEnderecos && (
                <ClienteEnderecosModal
                    cliente={clienteEnderecos}
                    onClose={() => setClienteEnderecos(null)}
                />
            )}

            {clienteCredito && (
                <ClienteCreditoModal
                    cliente={clienteCredito}
                    podeEditar={canEditCredito}
                    onClose={() => setClienteCredito(null)}
                    onSalvo={(atualizado) => {
                        setClientes(prev => prev.map(c => (c.id === atualizado.id ? atualizado : c)));
                        setClienteCredito(atualizado);
                    }}
                />
            )}
        </div>
    );
}

function formatarSituacaoCredito(situacao) {
    switch (situacao) {
        case 'LIBERADO': return 'Liberado';
        case 'BLOQUEADO': return 'Bloqueado';
        case 'EM_ANALISE':
        default: return 'Em análise';
    }
}

function formatarMoeda(valor) {
    if (valor === null || valor === undefined) return '—';
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

// Modal de gestão de endereços de um cliente já cadastrado — mesma lógica de
// "Meus Endereços" do cliente, só que operada pela equipe via
// /api/clientes/{id}/enderecos.
function ClienteEnderecosModal({ cliente, onClose }) {
    const [enderecos, setEnderecos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState(null);
    const [formAberto, setFormAberto] = useState(false);
    const [editandoId, setEditandoId] = useState(null);
    const [form, setForm] = useState({ ...enderecoVazio });
    const [salvando, setSalvando] = useState(false);

    function carregar() {
        setLoading(true);
        setErro(null);
        api.get(`/api/clientes/${cliente.id}/enderecos`)
            .then(res => setEnderecos(res.data))
            .catch(() => setErro('Não foi possível carregar os endereços.'))
            .finally(() => setLoading(false));
    }

    useEffect(() => { carregar(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

    function abrirNovo() {
        setEditandoId(null);
        setForm({ ...enderecoVazio, principal: enderecos.length === 0 });
        setFormAberto(true);
    }

    function abrirEdicao(endereco) {
        setEditandoId(endereco.id);
        setForm({
            apelido: endereco.apelido || '',
            cep: endereco.cep || '',
            rua: endereco.rua || '',
            numero: endereco.numero || '',
            complemento: endereco.complemento || '',
            bairro: endereco.bairro || '',
            cidade: endereco.cidade || '',
            estado: endereco.estado || '',
            principal: Boolean(endereco.principal),
        });
        setFormAberto(true);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setSalvando(true);
        try {
            if (editandoId) {
                await api.put(`/api/clientes/${cliente.id}/enderecos/${editandoId}`, form);
            } else {
                await api.post(`/api/clientes/${cliente.id}/enderecos`, form);
            }
            setFormAberto(false);
            carregar();
        } catch (err) {
            alert(err?.response?.data?.message || 'Não foi possível salvar o endereço.');
        } finally {
            setSalvando(false);
        }
    }

    async function handleRemover(endereco) {
        if (!window.confirm(`Remover o endereço "${endereco.apelido || endereco.formatado}"?`)) return;
        try {
            await api.delete(`/api/clientes/${cliente.id}/enderecos/${endereco.id}`);
            carregar();
        } catch (err) {
            alert(err?.response?.data?.message || 'Não foi possível remover o endereço.');
        }
    }

    async function handleDefinirPrincipal(endereco) {
        try {
            await api.patch(`/api/clientes/${cliente.id}/enderecos/${endereco.id}/principal`);
            carregar();
        } catch (err) {
            alert(err?.response?.data?.message || 'Não foi possível definir como principal.');
        }
    }

    return (
        <div className="modalBackdrop" onClick={onClose}>
            <div className="modalCard clienteEnderecosModalCard" onClick={(e) => e.stopPropagation()}>
                <div className="specsHeader">
                    <label><FontAwesomeIcon icon={faLocationDot} /> Endereços de {cliente.nome}</label>
                    <button type="button" className="btnSmallDelete" onClick={onClose}>Fechar</button>
                </div>

                {loading ? (
                    <p>Carregando...</p>
                ) : erro ? (
                    <p className="messageBanner negative">{erro}</p>
                ) : (
                    <div className="clienteEnderecosLista">
                        {enderecos.length === 0 && !formAberto && <p>Nenhum endereço cadastrado ainda.</p>}
                        {enderecos.map(endereco => (
                            <div key={endereco.id} className="clienteEnderecoItem">
                                <div>
                                    <strong>{endereco.apelido || 'Endereço'}</strong>
                                    {endereco.principal && <span className="enderecoBadgePrincipal"> Principal</span>}
                                    <p>{endereco.formatado}</p>
                                </div>
                                <div className="clienteEnderecoItemAcoes">
                                    {!endereco.principal && (
                                        <button type="button" className="btnLinkAdd" onClick={() => handleDefinirPrincipal(endereco)}>Tornar principal</button>
                                    )}
                                    <button type="button" className="btnLinkAdd" onClick={() => abrirEdicao(endereco)}><FontAwesomeIcon icon={faPen} /></button>
                                    <button type="button" className="btnSmallDelete" onClick={() => handleRemover(endereco)}><FontAwesomeIcon icon={faTrash} /></button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {!formAberto && (
                    <button type="button" className="btnLinkAdd" onClick={abrirNovo}>+ Adicionar endereço</button>
                )}

                {formAberto && (
                    <form onSubmit={handleSubmit} className="clienteEnderecoFormRow">
                        <EnderecoFields value={form} onChange={setForm} showApelido showPrincipal prefixo="cliente-modal" />
                        <div className="formFooter">
                            <button type="button" className="btnSmallDelete" onClick={() => setFormAberto(false)} disabled={salvando}>Cancelar</button>
                            <button type="submit" className="addBtn" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}

// Modal de crédito de um cliente — leitura pra qualquer um que já vê o
// cliente (RH, consultor etc.), edição só pra quem avalia (ver
// canEditCredito acima e PATCH /api/clientes/{id}/credito no backend).
function ClienteCreditoModal({ cliente, podeEditar, onClose, onSalvo }) {
    const [editando, setEditando] = useState(false);
    const [salvando, setSalvando] = useState(false);
    const [erro, setErro] = useState(null);
    const [form, setForm] = useState({
        situacaoCredito: cliente.situacaoCredito || 'EM_ANALISE',
        limiteCredito: cliente.limiteCredito ?? '',
        observacoesCredito: cliente.observacoesCredito || '',
    });

    async function handleSalvar(e) {
        e.preventDefault();
        setSalvando(true);
        setErro(null);
        try {
            const payload = {
                situacaoCredito: form.situacaoCredito,
                limiteCredito: form.limiteCredito === '' ? null : Number(form.limiteCredito),
                observacoesCredito: form.observacoesCredito.trim() || null,
            };
            const res = await api.patch(`/api/clientes/${cliente.id}/credito`, payload);
            onSalvo(res.data);
            setEditando(false);
        } catch (err) {
            setErro(err?.response?.data?.message || err?.response?.data || 'Não foi possível salvar o crédito.');
        } finally {
            setSalvando(false);
        }
    }

    return (
        <div className="modalBackdrop" onClick={onClose}>
            <div className="modalCard clienteEnderecosModalCard" onClick={(e) => e.stopPropagation()}>
                <div className="specsHeader">
                    <label><FontAwesomeIcon icon={faCoins} /> Crédito de {cliente.nome}</label>
                    <button type="button" className="btnSmallDelete" onClick={onClose}>Fechar</button>
                </div>

                {erro && <div className="messageBanner negative">{erro}</div>}

                {!editando ? (
                    <>
                        <div className="creditoResumo">
                            <div className="creditoResumoItem">
                                <span>Situação</span>
                                <span className={`creditoBadge creditoBadge-${cliente.situacaoCredito || 'EM_ANALISE'}`}>
                                    {formatarSituacaoCredito(cliente.situacaoCredito)}
                                </span>
                            </div>
                            <div className="creditoResumoItem">
                                <span>Limite</span>
                                <span>{formatarMoeda(cliente.limiteCredito)}</span>
                            </div>
                            <div className="creditoResumoItem">
                                <span>Utilizado</span>
                                <span>{formatarMoeda(cliente.creditoUtilizado)}</span>
                            </div>
                            <div className={`creditoResumoItem ${cliente.creditoDisponivel < 0 ? 'negativo' : ''}`}>
                                <span>Disponível</span>
                                <span>{formatarMoeda(cliente.creditoDisponivel)}</span>
                            </div>
                        </div>

                        {cliente.observacoesCredito && (
                            <p className="creditoObservacoesTexto">{cliente.observacoesCredito}</p>
                        )}

                        {podeEditar && (
                            <button type="button" className="btnLinkAdd" onClick={() => setEditando(true)}>
                                <FontAwesomeIcon icon={faPen} /> Editar crédito
                            </button>
                        )}
                    </>
                ) : (
                    <form onSubmit={handleSalvar}>
                        <div className="creditoFormGrid">
                            <div className="creditoFormField">
                                <label htmlFor="credito-situacao">Situação</label>
                                <select
                                    id="credito-situacao"
                                    className="equipInput"
                                    value={form.situacaoCredito}
                                    onChange={(e) => setForm(prev => ({ ...prev, situacaoCredito: e.target.value }))}
                                >
                                    <option value="EM_ANALISE">Em análise</option>
                                    <option value="LIBERADO">Liberado</option>
                                    <option value="BLOQUEADO">Bloqueado</option>
                                </select>
                            </div>
                            <div className="creditoFormField">
                                <label htmlFor="credito-limite">Limite de crédito (R$)</label>
                                <input
                                    id="credito-limite"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    className="equipInput"
                                    placeholder="Ex: 5000.00"
                                    value={form.limiteCredito}
                                    onChange={(e) => setForm(prev => ({ ...prev, limiteCredito: e.target.value }))}
                                />
                            </div>
                            <div className="creditoFormField creditoFormFull">
                                <label htmlFor="credito-obs">Observações internas</label>
                                <textarea
                                    id="credito-obs"
                                    className="equipTextarea"
                                    rows={3}
                                    placeholder="Ex: histórico de pagamento, motivo do bloqueio..."
                                    value={form.observacoesCredito}
                                    onChange={(e) => setForm(prev => ({ ...prev, observacoesCredito: e.target.value }))}
                                />
                            </div>
                        </div>
                        <div className="formFooter">
                            <button type="button" className="btnSmallDelete" onClick={() => setEditando(false)} disabled={salvando}>Cancelar</button>
                            <button type="submit" className="addBtn" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
