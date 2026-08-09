import React, { useEffect, useState, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './OrdemServico.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faWrench, faPlus, faTrash, faList, faClipboardList,
    faSearch, faCheckCircle, faTimesCircle, faGaugeHigh,
    faBoxesStacked, faTriangleExclamation, faPlay, faBoxOpen,
} from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

const STATUS_OS_LABEL = {
    ABERTA: 'Aberta',
    EM_ANDAMENTO: 'Em andamento',
    CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada',
};

function formatDate(dateStr) {
    if (!dateStr) return '---';
    const d = new Date(dateStr);
    return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

/* ============================================================
   MODAL — ABRIR NOVA OS
   ============================================================ */
function AbrirOSModal({ unidadesDisponiveis, tecnicos, tecnicoPadraoId, onClose, onCreated }) {
    const [form, setForm] = useState({
        unidadeId: '',
        tecnicoId: tecnicoPadraoId || '',
        observacoes: '',
    });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    function handleChange(e) {
        setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!form.unidadeId) {
            setError('Selecione a unidade que vai entrar em manutenção.');
            return;
        }
        setSaving(true);
        setError(null);
        api.post('/api/ordens-servico', {
            unidadeId: Number(form.unidadeId),
            tecnicoId: form.tecnicoId ? Number(form.tecnicoId) : null,
            observacoes: form.observacoes || null,
        })
            .then(() => {
                onCreated();
                onClose();
            })
            .catch(err => setError(err.response?.data?.message || err.response?.data || 'Erro ao abrir Ordem de Serviço'))
            .finally(() => setSaving(false));
    }

    return (
        <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard osModalCard">
                <div className="modalHeader">
                    <h3><FontAwesomeIcon icon={faWrench} /> Abrir Ordem de Serviço</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {error && <div className="messageBanner negative">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="formField">
                        <label>Unidade com defeito / falha</label>
                        <select className="equipInput" name="unidadeId" value={form.unidadeId} onChange={handleChange} required>
                            <option value="">Selecione a unidade...</option>
                            {unidadesDisponiveis.map(u => (
                                <option key={u.id} value={u.id}>
                                    {u.equipamentoNome} — {u.codigoPatrimonio || u.numeroDeSerie || `Unidade #${u.id}`}
                                </option>
                            ))}
                        </select>
                        {unidadesDisponiveis.length === 0 && (
                            <p className="fieldHint">Nenhuma unidade em manutenção sem OS aberta no momento.</p>
                        )}
                    </div>

                    <div className="formField">
                        <label>Técnico responsável (opcional)</label>
                        <select className="equipInput" name="tecnicoId" value={form.tecnicoId} onChange={handleChange}>
                            <option value="">A definir depois</option>
                            {tecnicos.map(t => (
                                <option key={t.id} value={t.id}>{t.nome}</option>
                            ))}
                        </select>
                    </div>

                    <div className="formField">
                        <label>Observações iniciais</label>
                        <textarea
                            className="equipTextarea"
                            name="observacoes"
                            rows={3}
                            placeholder="Ex: Máquina retornou da obra fazendo barulho estranho..."
                            value={form.observacoes}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="formFooter" style={{ justifyContent: 'flex-end' }}>
                        <button type="button" className="smallBtn" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="addBtn" disabled={saving}>
                            {saving ? 'Abrindo...' : 'Abrir OS'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ============================================================
   MODAL — DETALHE / TRABALHO NA OS
   ============================================================ */
function OSDetalheModal({ osId, tecnicos, pecas, onClose, onChanged }) {
    const [os, setOs] = useState(null);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState(null);
    const [saving, setSaving] = useState(false);

    const [diagForm, setDiagForm] = useState({ diagnostico: '', observacoes: '', horimetroRegistrado: '', tecnicoId: '' });
    const [novoItem, setNovoItem] = useState({ pecaId: '', quantidade: 1 });

    useEffect(() => {
        carregar();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [osId]);

    function carregar() {
        setLoading(true);
        api.get(`/api/ordens-servico/${osId}`)
            .then(res => {
                setOs(res.data);
                setDiagForm({
                    diagnostico: res.data.diagnostico || '',
                    observacoes: res.data.observacoes || '',
                    horimetroRegistrado: res.data.horimetroRegistrado ?? '',
                    tecnicoId: res.data.tecnicoId || '',
                });
            })
            .catch(() => setMessage({ type: 'error', text: 'Erro ao carregar Ordem de Serviço' }))
            .finally(() => setLoading(false));
    }

    function handleDiagChange(e) {
        setDiagForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
    }

    function salvarDiagnostico() {
        setSaving(true);
        setMessage(null);
        api.patch(`/api/ordens-servico/${osId}/diagnostico`, {
            diagnostico: diagForm.diagnostico,
            observacoes: diagForm.observacoes,
            horimetroRegistrado: diagForm.horimetroRegistrado !== '' ? Number(diagForm.horimetroRegistrado) : null,
            tecnicoId: diagForm.tecnicoId ? Number(diagForm.tecnicoId) : null,
        })
            .then(res => {
                setOs(res.data);
                setMessage({ type: 'success', text: 'Diagnóstico salvo.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao salvar diagnóstico' }))
            .finally(() => setSaving(false));
    }

    function adicionarPeca() {
        if (!novoItem.pecaId || !novoItem.quantidade || Number(novoItem.quantidade) <= 0) {
            setMessage({ type: 'error', text: 'Selecione a peça e uma quantidade válida.' });
            return;
        }
        setSaving(true);
        setMessage(null);
        api.post(`/api/ordens-servico/${osId}/itens`, {
            pecaId: Number(novoItem.pecaId),
            quantidade: Number(novoItem.quantidade),
        })
            .then(res => {
                setOs(res.data);
                setNovoItem({ pecaId: '', quantidade: 1 });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao lançar peça' }))
            .finally(() => setSaving(false));
    }

    function removerPeca(itemId) {
        setSaving(true);
        setMessage(null);
        api.delete(`/api/ordens-servico/${osId}/itens/${itemId}`)
            .then(res => {
                setOs(res.data);
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao remover peça' }))
            .finally(() => setSaving(false));
    }

    function concluirOS() {
        if (!window.confirm('Concluir esta OS e liberar a unidade para locação?')) return;
        setSaving(true);
        setMessage(null);
        api.patch(`/api/ordens-servico/${osId}/concluir`)
            .then(res => {
                setOs(res.data);
                setMessage({ type: 'success', text: 'OS concluída! Unidade liberada para locação.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao concluir OS' }))
            .finally(() => setSaving(false));
    }

    function cancelarOS() {
        const motivo = window.prompt('Motivo do cancelamento (opcional):', '');
        if (motivo === null) return;
        setSaving(true);
        setMessage(null);
        api.patch(`/api/ordens-servico/${osId}/cancelar`, { motivo })
            .then(res => {
                setOs(res.data);
                setMessage({ type: 'success', text: 'OS cancelada.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao cancelar OS' }))
            .finally(() => setSaving(false));
    }

    if (loading || !os) {
        return (
            <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
                <div className="modalCard osModalCard"><p style={{ padding: 20 }}>Carregando...</p></div>
            </div>
        );
    }

    const finalizada = os.status === 'CONCLUIDA' || os.status === 'CANCELADA';

    return (
        <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard osModalCard osDetalheModalCard">
                <div className="modalHeader">
                    <h3>
                        <FontAwesomeIcon icon={faWrench} /> OS #{os.id} — {os.equipamentoNome}
                        <span className={`osStatusTag ${os.status?.toLowerCase()}`}>
                            {STATUS_OS_LABEL[os.status] || os.status}
                        </span>
                    </h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                <div className="detalheInfoGrid">
                    <div className="detalheInfoItem"><strong>Unidade:</strong> {os.unidadeCodigoPatrimonio || `#${os.unidadeId}`}</div>
                    <div className="detalheInfoItem"><strong>Aberta em:</strong> {formatDate(os.abertaEm)}</div>
                    <div className="detalheInfoItem"><strong>Iniciada em:</strong> {formatDate(os.iniciadaEm)}</div>
                    <div className="detalheInfoItem"><strong>Concluída em:</strong> {formatDate(os.concluidaEm)}</div>
                </div>

                {/* DIAGNÓSTICO */}
                <div className="specsContainer">
                    <div className="specsHeader"><h4><FontAwesomeIcon icon={faClipboardList} /> Diagnóstico técnico</h4></div>

                    <div className="formField">
                        <label>Técnico responsável</label>
                        <select className="equipInput" name="tecnicoId" value={diagForm.tecnicoId} onChange={handleDiagChange} disabled={finalizada}>
                            <option value="">A definir</option>
                            {tecnicos.map(t => (
                                <option key={t.id} value={t.id}>{t.nome}</option>
                            ))}
                        </select>
                    </div>

                    <div className="formField">
                        <label>Diagnóstico (ex: "Troca do induzido e rolamento do martelete de 15kg")</label>
                        <textarea
                            className="equipTextarea"
                            name="diagnostico"
                            rows={3}
                            value={diagForm.diagnostico}
                            onChange={handleDiagChange}
                            disabled={finalizada}
                        />
                    </div>

                    <div className="formGrid">
                        <div className="formField">
                            <label><FontAwesomeIcon icon={faGaugeHigh} /> Horímetro registrado (h)</label>
                            <input
                                className="equipInput"
                                type="number"
                                step="0.1"
                                name="horimetroRegistrado"
                                value={diagForm.horimetroRegistrado}
                                onChange={handleDiagChange}
                                disabled={finalizada}
                            />
                        </div>
                        <div className="formField">
                            <label>Observações</label>
                            <input
                                className="equipInput"
                                name="observacoes"
                                value={diagForm.observacoes}
                                onChange={handleDiagChange}
                                disabled={finalizada}
                            />
                        </div>
                    </div>

                    {!finalizada && (
                        <div className="formFooter" style={{ justifyContent: 'flex-end' }}>
                            <button type="button" className="smallBtn success" disabled={saving} onClick={salvarDiagnostico}>
                                Salvar diagnóstico
                            </button>
                        </div>
                    )}
                </div>

                {/* PEÇAS UTILIZADAS */}
                <div className="specsContainer">
                    <div className="specsHeader"><h4><FontAwesomeIcon icon={faBoxesStacked} /> Peças utilizadas (baixa no estoque)</h4></div>

                    {!finalizada && (
                        <div className="unidadeFormRow">
                            <select
                                className="equipInput"
                                value={novoItem.pecaId}
                                onChange={(e) => setNovoItem(prev => ({ ...prev, pecaId: e.target.value }))}
                            >
                                <option value="">Selecione a peça...</option>
                                {pecas.map(p => (
                                    <option key={p.id} value={p.id}>
                                        {p.nome} {p.codigo ? `(${p.codigo})` : ''} — estoque: {p.quantidadeEmEstoque}
                                    </option>
                                ))}
                            </select>
                            <input
                                className="equipInput"
                                type="number"
                                min="1"
                                placeholder="Qtd"
                                value={novoItem.quantidade}
                                onChange={(e) => setNovoItem(prev => ({ ...prev, quantidade: e.target.value }))}
                            />
                            <button type="button" className="addBtn" disabled={saving} onClick={adicionarPeca}>
                                <FontAwesomeIcon icon={faPlus} /> Lançar
                            </button>
                        </div>
                    )}

                    <div className="tableWrapper">
                        <table className="usersTable">
                            <thead>
                                <tr>
                                    <th>Peça</th>
                                    <th>Qtd. usada</th>
                                    {!finalizada && <th>Ações</th>}
                                </tr>
                            </thead>
                            <tbody>
                                {(os.itens || []).length > 0 ? (
                                    os.itens.map(item => (
                                        <tr key={item.id} className="tableRow">
                                            <td>{item.pecaNome} {item.pecaCodigo ? `(${item.pecaCodigo})` : ''}</td>
                                            <td>{item.quantidade}</td>
                                            {!finalizada && (
                                                <td className="actionsCell">
                                                    <button type="button" className="actionBtn delete" onClick={() => removerPeca(item.id)} title="Remover / devolver ao estoque">
                                                        <FontAwesomeIcon icon={faTrash} />
                                                    </button>
                                                </td>
                                            )}
                                        </tr>
                                    ))
                                ) : (
                                    <tr className="tableRow">
                                        <td colSpan={finalizada ? 2 : 3} style={{ textAlign: 'center', color: '#999' }}>Nenhuma peça lançada</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* AÇÕES FINAIS */}
                {!finalizada && (
                    <div className="formFooter" style={{ justifyContent: 'flex-end', marginTop: 16 }}>
                        <button type="button" className="smallBtn danger" disabled={saving} onClick={cancelarOS}>
                            <FontAwesomeIcon icon={faTimesCircle} /> Cancelar OS
                        </button>
                        <button type="button" className="addBtn success" disabled={saving} onClick={concluirOS}>
                            <FontAwesomeIcon icon={faCheckCircle} /> Concluir e liberar para locação
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

/* ============================================================
   ABA — CADASTRO DE PEÇAS / ESTOQUE
   ============================================================ */
function PecasEstoqueSection({ pecas, onChanged }) {
    const [form, setForm] = useState({ codigo: '', nome: '', quantidadeEmEstoque: '', unidadeMedida: 'un', estoqueMinimo: '' });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);
    const [entradaQtd, setEntradaQtd] = useState({});

    function handleChange(e) {
        setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!form.nome.trim()) {
            setMessage({ type: 'error', text: 'Informe o nome da peça.' });
            return;
        }
        setSaving(true);
        setMessage(null);
        api.post('/api/pecas', {
            codigo: form.codigo || null,
            nome: form.nome,
            quantidadeEmEstoque: form.quantidadeEmEstoque !== '' ? Number(form.quantidadeEmEstoque) : 0,
            unidadeMedida: form.unidadeMedida || null,
            estoqueMinimo: form.estoqueMinimo !== '' ? Number(form.estoqueMinimo) : null,
        })
            .then(() => {
                setForm({ codigo: '', nome: '', quantidadeEmEstoque: '', unidadeMedida: 'un', estoqueMinimo: '' });
                setMessage({ type: 'success', text: 'Peça cadastrada.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao cadastrar peça' }))
            .finally(() => setSaving(false));
    }

    function handleEntrada(pecaId) {
        const qtd = Number(entradaQtd[pecaId]);
        if (!qtd || qtd <= 0) return;
        api.patch(`/api/pecas/${pecaId}/entrada`, { quantidade: qtd })
            .then(() => {
                setEntradaQtd(prev => ({ ...prev, [pecaId]: '' }));
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao registrar entrada' }));
    }

    function handleDelete(pecaId) {
        if (!window.confirm('Remover esta peça do cadastro de estoque?')) return;
        api.delete(`/api/pecas/${pecaId}`)
            .then(onChanged)
            .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || err.response?.data || 'Erro ao remover peça' }));
    }

    return (
        <div className="recentUsersSection">
            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastrar peça no estoque</h3>
                {message && <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="formGrid">
                        <div className="formField">
                            <label>Nome</label>
                            <input className="equipInput" name="nome" value={form.nome} onChange={handleChange} required />
                        </div>
                        <div className="formField">
                            <label>Código</label>
                            <input className="equipInput" name="codigo" value={form.codigo} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="formGrid">
                        <div className="formField">
                            <label>Quantidade inicial</label>
                            <input className="equipInput" type="number" min="0" name="quantidadeEmEstoque" value={form.quantidadeEmEstoque} onChange={handleChange} />
                        </div>
                        <div className="formField">
                            <label>Unidade de medida</label>
                            <input className="equipInput" name="unidadeMedida" placeholder="un, L, kg..." value={form.unidadeMedida} onChange={handleChange} />
                        </div>
                        <div className="formField">
                            <label>Estoque mínimo (alerta)</label>
                            <input className="equipInput" type="number" min="0" name="estoqueMinimo" value={form.estoqueMinimo} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="formFooter">
                        <button type="submit" className="addBtn" disabled={saving}>Cadastrar peça</button>
                    </div>
                </form>
            </div>

            <div className="sectionHeader">
                <h3><FontAwesomeIcon icon={faBoxesStacked} /> Estoque de peças</h3>
            </div>
            <div className="tableWrapper">
                <table className="usersTable">
                    <thead>
                        <tr>
                            <th>Peça</th>
                            <th>Código</th>
                            <th>Estoque</th>
                            <th>Entrada</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {pecas.length > 0 ? pecas.map(p => (
                            <tr key={p.id} className="tableRow">
                                <td>{p.nome}</td>
                                <td>{p.codigo || '---'}</td>
                                <td>
                                    <span className={p.estoqueBaixo ? 'osStatusTag cancelada' : 'osStatusTag concluida'}>
                                        {p.quantidadeEmEstoque} {p.unidadeMedida || ''}
                                    </span>
                                </td>
                                <td>
                                    <div className="unidadeFormRow" style={{ margin: 0 }}>
                                        <input
                                            className="equipInput"
                                            type="number"
                                            min="1"
                                            style={{ width: 90 }}
                                            placeholder="Qtd"
                                            value={entradaQtd[p.id] || ''}
                                            onChange={(e) => setEntradaQtd(prev => ({ ...prev, [p.id]: e.target.value }))}
                                        />
                                        <button type="button" className="actionBtn edit" title="Registrar entrada" onClick={() => handleEntrada(p.id)}>
                                            <FontAwesomeIcon icon={faBoxOpen} />
                                        </button>
                                    </div>
                                </td>
                                <td className="actionsCell">
                                    <button type="button" className="actionBtn delete" onClick={() => handleDelete(p.id)} title="Remover">
                                        <FontAwesomeIcon icon={faTrash} />
                                    </button>
                                </td>
                            </tr>
                        )) : (
                            <tr className="tableRow">
                                <td colSpan="5" style={{ textAlign: 'center', color: '#999' }}>Nenhuma peça cadastrada ainda</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

/* ============================================================
   PÁGINA PRINCIPAL
   ============================================================ */
export default function OrdensServico() {
    const { user } = useAuth();
    const [equipamentos, setEquipamentos] = useState([]);
    const [ordens, setOrdens] = useState([]);
    const [tecnicos, setTecnicos] = useState([]);
    const [pecas, setPecas] = useState([]);
    const [alertas, setAlertas] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [abrirModalOpen, setAbrirModalOpen] = useState(false);
    const [detalheOsId, setDetalheOsId] = useState(null);
    const [aba, setAba] = useState('fila'); // fila | historico | pecas | alertas

    useEffect(() => {
        fetchTudo();
    }, []);

    function fetchTudo() {
        setLoading(true);
        Promise.all([
            api.get('/api/equipamentos'),
            api.get('/api/ordens-servico'),
            api.get('/api/funcionarios', { params: { apenasAtivos: true } }),
            api.get('/api/pecas'),
            api.get('/api/unidades/alertas-manutencao'),
        ])
            .then(([eqRes, osRes, funcRes, pecasRes, alertasRes]) => {
                setEquipamentos(eqRes.data);
                setOrdens(osRes.data);
                setTecnicos((funcRes.data || []).filter(f => f.cargo === 'TECNICO_MANUTENCAO'));
                setPecas(pecasRes.data);
                setAlertas(alertasRes.data);
            })
            .catch(() => { /* feedback já é mostrado nas seções, mantemos a página de pé */ })
            .finally(() => setLoading(false));
    }

    if (!canAccessAdminRoute(user, '/admin/ordens-servico')) {
        return <Navigate to="/admin" replace />;
    }

    // Todas as unidades (achatadas), com nome do equipamento
    const todasUnidades = useMemo(() => {
        const lista = [];
        equipamentos.forEach(eq => {
            (eq.unidades || []).forEach(u => lista.push({ ...u, equipamentoNome: eq.nome }));
        });
        return lista;
    }, [equipamentos]);

    // Unidades em manutenção que ainda não têm OS aberta/em andamento
    const unidadesSemOSAberta = useMemo(() => {
        const idsComOSAberta = new Set(
            ordens.filter(o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO').map(o => o.unidadeId)
        );
        return todasUnidades.filter(u => (u.status === 'EM_MANUTENCAO' || u.status === 'AGUARDANDO_MANUTENCAO') && !idsComOSAberta.has(u.id));
    }, [todasUnidades, ordens]);

    const ordensFila = useMemo(() => {
        return ordens
            .filter(o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO')
            .filter(o =>
                (o.equipamentoNome || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                (o.unidadeCodigoPatrimonio || '').toLowerCase().includes(searchTerm.toLowerCase())
            );
    }, [ordens, searchTerm]);

    const ordensHistorico = useMemo(() => {
        return ordens
            .filter(o => o.status === 'CONCLUIDA' || o.status === 'CANCELADA')
            .filter(o =>
                (o.equipamentoNome || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                (o.unidadeCodigoPatrimonio || '').toLowerCase().includes(searchTerm.toLowerCase())
            );
    }, [ordens, searchTerm]);

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Ordens de Serviço</h2>
                <div className="headerRight">
                    <button type="button" className="addBtn" onClick={() => setAbrirModalOpen(true)}>
                        <FontAwesomeIcon icon={faPlus} /> Abrir OS
                    </button>
                </div>
            </div>

            <div className="statsGrid">
                <div className="statCard">
                    <div className="statCardTop">
                        <div className="statInfo">
                            <p className="statTitle">Aguardando manutenção</p>
                            <p className="statValue">{unidadesSemOSAberta.length}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#d63031' }}>
                            <FontAwesomeIcon icon={faTriangleExclamation} />
                        </div>
                    </div>
                </div>
                <div className="statCard">
                    <div className="statCardTop">
                        <div className="statInfo">
                            <p className="statTitle">OS em andamento</p>
                            <p className="statValue">{ordensFila.length}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#ff8c00' }}>
                            <FontAwesomeIcon icon={faWrench} />
                        </div>
                    </div>
                </div>
                <div className="statCard">
                    <div className="statCardTop">
                        <div className="statInfo">
                            <p className="statTitle">Alertas de horímetro</p>
                            <p className="statValue">{alertas.length}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#9C27B0' }}>
                            <FontAwesomeIcon icon={faGaugeHigh} />
                        </div>
                    </div>
                </div>
                <div className="statCard">
                    <div className="statCardTop">
                        <div className="statInfo">
                            <p className="statTitle">Peças em estoque</p>
                            <p className="statValue">{pecas.length}</p>
                        </div>
                        <div className="statIcon" style={{ '--icon-color': '#4CAF50' }}>
                            <FontAwesomeIcon icon={faBoxesStacked} />
                        </div>
                    </div>
                </div>
            </div>

            <div className="osTabs">
                <button type="button" className={`osTabBtn ${aba === 'fila' ? 'active' : ''}`} onClick={() => setAba('fila')}>
                    <FontAwesomeIcon icon={faClipboardList} /> Aguardando Manutenção
                </button>
                <button type="button" className={`osTabBtn ${aba === 'historico' ? 'active' : ''}`} onClick={() => setAba('historico')}>
                    <FontAwesomeIcon icon={faList} /> Histórico de OS
                </button>
                <button type="button" className={`osTabBtn ${aba === 'pecas' ? 'active' : ''}`} onClick={() => setAba('pecas')}>
                    <FontAwesomeIcon icon={faBoxesStacked} /> Peças / Estoque
                </button>
                <button type="button" className={`osTabBtn ${aba === 'alertas' ? 'active' : ''}`} onClick={() => setAba('alertas')}>
                    <FontAwesomeIcon icon={faGaugeHigh} /> Manutenção Preventiva
                </button>
            </div>

            {(aba === 'fila' || aba === 'historico') && (
                <div className="recentUsersSection">
                    <div className="sectionHeader">
                        <h3>
                            <FontAwesomeIcon icon={faClipboardList} />
                            {aba === 'fila' ? ' Ordens de Serviço abertas' : ' Ordens de Serviço finalizadas'}
                        </h3>
                        <div className="headerRight">
                            <div className="searchBox">
                                <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                                <input
                                    className="searchInput"
                                    placeholder="Pesquisar por equipamento ou patrimônio..."
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
                                    <th>Equipamento</th>
                                    <th>Unidade</th>
                                    <th>Técnico</th>
                                    <th>Status</th>
                                    <th>Aberta em</th>
                                    <th>Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {!loading && (aba === 'fila' ? ordensFila : ordensHistorico).map(os => (
                                    <tr key={os.id} className="tableRow">
                                        <td className="nameCell">
                                            <div className="userCell">
                                                <div className="userCellAvatar">{(os.equipamentoNome || '?').charAt(0)}</div>
                                                <div>
                                                    <div className="userName">{os.equipamentoNome}</div>
                                                    <div className="userRole">OS #{os.id}</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>{os.unidadeCodigoPatrimonio || `#${os.unidadeId}`}</td>
                                        <td>{os.tecnicoNome || '---'}</td>
                                        <td>
                                            <span className={`osStatusTag ${os.status?.toLowerCase()}`}>
                                                {STATUS_OS_LABEL[os.status] || os.status}
                                            </span>
                                        </td>
                                        <td>{formatDate(os.abertaEm)}</td>
                                        <td className="actionsCell">
                                            <button className="actionBtn edit" onClick={() => setDetalheOsId(os.id)} title="Abrir OS">
                                                <FontAwesomeIcon icon={faPlay} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                {!loading && (aba === 'fila' ? ordensFila : ordensHistorico).length === 0 && (
                                    <tr className="tableRow">
                                        <td colSpan="6" style={{ textAlign: 'center', color: '#999' }}>
                                            {aba === 'fila' ? 'Nenhuma OS aberta no momento' : 'Nenhuma OS finalizada ainda'}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {aba === 'pecas' && <PecasEstoqueSection pecas={pecas} onChanged={fetchTudo} />}

            {aba === 'alertas' && (
                <div className="recentUsersSection">
                    <div className="sectionHeader">
                        <h3><FontAwesomeIcon icon={faTriangleExclamation} /> Unidades no limite do horímetro</h3>
                    </div>
                    <div className="tableWrapper">
                        <table className="usersTable">
                            <thead>
                                <tr>
                                    <th>Unidade</th>
                                    <th>Horímetro atual</th>
                                    <th>Limite p/ manutenção</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {alertas.length > 0 ? alertas.map(u => (
                                    <tr key={u.id} className="tableRow">
                                        <td>{u.codigoPatrimonio || u.numeroDeSerie || `Unidade #${u.id}`}</td>
                                        <td>{u.horimetroAtual}h</td>
                                        <td>{u.horimetroLimiteManutencao}h</td>
                                        <td><span className="osStatusTag cancelada">Precisa de manutenção</span></td>
                                    </tr>
                                )) : (
                                    <tr className="tableRow">
                                        <td colSpan="4" style={{ textAlign: 'center', color: '#999' }}>Nenhuma unidade atingiu o limite de horímetro</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {abrirModalOpen && (
                <AbrirOSModal
                    unidadesDisponiveis={unidadesSemOSAberta}
                    tecnicos={tecnicos}
                    tecnicoPadraoId={user?.idFuncionario}
                    onClose={() => setAbrirModalOpen(false)}
                    onCreated={fetchTudo}
                />
            )}

            {detalheOsId && (
                <OSDetalheModal
                    key={detalheOsId}
                    osId={detalheOsId}
                    tecnicos={tecnicos}
                    pecas={pecas}
                    onClose={() => setDetalheOsId(null)}
                    onChanged={fetchTudo}
                />
            )}
        </div>
    );
}
