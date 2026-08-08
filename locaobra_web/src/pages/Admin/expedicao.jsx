import React, { useEffect, useState, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Expedicao.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faPlus, faTrash, faList, faTruck, faClipboardCheck,
    faClipboardList, faSearch, faTruckLoading, faCheckCircle,
    faTimesCircle, faCamera, faPen, faTimes, faSave, faSignature,
} from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

const STATUS_EXPEDICAO_LABEL = {
    AGENDADO: 'Agendado',
    EM_TRANSITO: 'Em trânsito',
    CONCLUIDO: 'Concluído',
    CANCELADO: 'Cancelado',
};

const TIPO_EXPEDICAO_LABEL = {
    ENTREGA: 'Entrega',
    COLETA: 'Coleta',
};

const TIPO_VISTORIA_LABEL = {
    ENTREGA: 'Vistoria de Entrega',
    DEVOLUCAO: 'Vistoria de Devolução',
};

function imageUrl(path) {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
}

function formatDate(dateStr) {
    if (!dateStr) return '---';
    const d = new Date(dateStr);
    return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function formatDateOnly(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('pt-BR');
}

/* ============================================================
   VISTORIA MODAL — Termo de vistoria de entrega/devolução
   com upload de fotos do estado do equipamento
   ============================================================ */
function VistoriaModal({ expedicao, onClose, onChanged }) {
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [form, setForm] = useState({
        unidadeId: '',
        tipo: 'ENTREGA',
        condicaoGeral: 'BOM',
        avariasExistentes: '',
        danosCausados: '',
        observacoes: '',
        nomeResponsavel: '',
        assinaturaResponsavel: '',
    });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);
    const [previewUrls, setPreviewUrls] = useState([]);

    const unidades = useMemo(() => {
        const itens = expedicao?.itens || [];
        const unique = [];
        const seen = new Set();
        itens.forEach(item => {
            if (item.unidadeId && !seen.has(item.unidadeId)) {
                seen.add(item.unidadeId);
                unique.push({
                    id: item.unidadeId,
                    codigoPatrimonio: item.codigoPatrimonio || 'Sem patrimônio',
                    equipamentoNome: item.equipamentoNome || 'Equipamento',
                });
            }
        });
        return unique;
    }, [expedicao]);

    useEffect(() => {
        if (unidades.length === 1) {
            setForm(prev => ({ ...prev, unidadeId: String(unidades[0].id) }));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [unidades.length]);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function handleFiles(e) {
        const files = Array.from(e.target.files || []);
        if (files.length === 0) return;
        setSelectedFiles(prev => [...prev, ...files]);
        const previews = files.map(f => URL.createObjectURL(f));
        setPreviewUrls(prev => [...prev, ...previews]);
        e.target.value = '';
    }

    function removeFile(idx) {
        URL.revokeObjectURL(previewUrls[idx]);
        setSelectedFiles(prev => prev.filter((_, i) => i !== idx));
        setPreviewUrls(prev => prev.filter((_, i) => i !== idx));
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!form.unidadeId) {
            setMessage({ type: 'error', text: 'Selecione a unidade para vistoriar.' });
            return;
        }
        setSaving(true);
        setMessage({ type: 'info', text: 'Salvando vistoria...' });

        const vistoriaData = {
            unidadeId: parseInt(form.unidadeId, 10),
            tipo: form.tipo,
            condicaoGeral: form.condicaoGeral,
            avariasExistentes: form.avariasExistentes,
            danosCausados: form.danosCausados,
            observacoes: form.observacoes,
            nomeResponsavel: form.nomeResponsavel,
            assinaturaResponsavel: form.assinaturaResponsavel,
        };

        const formData = new FormData();
        formData.append('vistoria', JSON.stringify(vistoriaData));
        selectedFiles.forEach(file => formData.append('fotos', file));

        api.post(`/api/expedicoes/${expedicao.id}/vistorias`, formData)
            .then(() => {
                setMessage({ type: 'success', text: 'Vistoria registrada com sucesso!' });
                setTimeout(() => {
                    onChanged();
                    onClose();
                }, 1200);
            })
            .catch(err => {
                const msg = typeof err.response?.data === 'string'
                    ? err.response.data
                    : (err.response?.data?.message || err.response?.data || err.message);
                setMessage({ type: 'error', text: 'Erro ao salvar vistoria: ' + msg });
            })
            .finally(() => setSaving(false));
    }

    return (
        <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard vistoriaModalCard">
                <div className="modalHeader">
                    <h3>
                        <FontAwesomeIcon icon={faClipboardCheck} />{' '}
                        {form.tipo === 'ENTREGA' ? 'Termo de Vistoria — Entrega' : 'Termo de Vistoria — Devolução'}
                    </h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="formGrid">
                        <div className="formField">
                            <label>Expedição</label>
                            <input className="equipInput" value={expedicao.codigo || ''} readOnly disabled />
                        </div>
                        <div className="formField">
                            <label>Unidade / Equipamento</label>
                            <select className="equipInput" name="unidadeId" value={form.unidadeId} onChange={handleChange} required>
                                <option value="">Selecione...</option>
                                {unidades.map(u => (
                                    <option key={u.id} value={u.id}>{u.codigoPatrimonio} — {u.equipamentoNome}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="formGrid">
                        <div className="formField">
                            <label>Tipo de Vistoria</label>
                            <select className="equipInput" name="tipo" value={form.tipo} onChange={handleChange}>
                                <option value="ENTREGA">Vistoria de Entrega</option>
                                <option value="DEVOLUCAO">Vistoria de Devolução</option>
                            </select>
                        </div>
                        <div className="formField">
                            <label>Condição Geral</label>
                            <select className="equipInput" name="condicaoGeral" value={form.condicaoGeral} onChange={handleChange}>
                                <option value="BOM">Bom</option>
                                <option value="REGULAR">Regular</option>
                                <option value="RUIM">Ruim</option>
                            </select>
                        </div>
                    </div>

                    <div className="formField">
                        <label>Avarias pré-existentes</label>
                        <textarea
                            className="equipTextarea"
                            name="avariasExistentes"
                            rows={2}
                            placeholder="Registrar avarias que já existiam antes (vistoria de entrega)..."
                            value={form.avariasExistentes}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="formField">
                        <label>Danos causados pelo cliente</label>
                        <textarea
                            className="equipTextarea"
                            name="danosCausados"
                            rows={2}
                            placeholder="Registrar danos causados durante o período de locação (vistoria de devolução)..."
                            value={form.danosCausados}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="formField">
                        <label>Observações</label>
                        <textarea
                            className="equipTextarea"
                            name="observacoes"
                            rows={2}
                            placeholder="Observações adicionais..."
                            value={form.observacoes}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="formGrid">
                        <div className="formField">
                            <label>Nome do responsável (cliente)</label>
                            <input
                                className="equipInput"
                                name="nomeResponsavel"
                                placeholder="Nome de quem assina"
                                value={form.nomeResponsavel}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="formField">
                            <label>Assinatura (base64 ou texto)</label>
                            <input
                                className="equipInput"
                                name="assinaturaResponsavel"
                                placeholder="Assinatura do responsável"
                                value={form.assinaturaResponsavel}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="formField">
                        <label>
                            <FontAwesomeIcon icon={faCamera} /> Fotos do estado do equipamento
                        </label>
                        <div className="fileInputWrapper photoUpload">
                            <FontAwesomeIcon icon={faCamera} />
                            <input type="file" multiple accept="image/*" onChange={handleFiles} />
                            <span>Adicionar fotos</span>
                        </div>

                        {previewUrls.length > 0 && (
                            <div className="photoPreviewGrid">
                                {previewUrls.map((url, idx) => (
                                    <div key={idx} className="photoPreviewItem">
                                        <img src={url} alt={`Foto ${idx + 1}`} />
                                        <button type="button" onClick={() => removeFile(idx)} title="Remover">
                                            <FontAwesomeIcon icon={faTimes} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="formFooter" style={{ justifyContent: 'flex-end' }}>
                        <button type="button" className="smallBtn" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="addBtn" disabled={saving}>
                            {saving ? 'Salvando...' : 'Registrar Vistoria'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ============================================================
   MODAL DE CRIAÇÃO DE EXPEDIÇÃO
   ============================================================ */
function NovaExpedicaoModal({ onClose, onChanged }) {
    const [clientes, setClientes] = useState([]);
    const [motoristas, setMotoristas] = useState([]);
    const [equipamentos, setEquipamentos] = useState([]);
    const [form, setForm] = useState({
        tipo: 'ENTREGA',
        clienteId: '',
        motoristaId: '',
        placaVeiculo: '',
        dataProgramada: new Date().toISOString().split('T')[0],
        horarioProgramado: '08:00',
        enderecoEntrega: '',
        observacoes: '',
    });
    const [itens, setItens] = useState([]);
    const [novoItem, setNovoItem] = useState({ unidadeId: '', quantidade: 1, observacaoItem: '' });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        Promise.all([
            api.get('/api/clientes?apenasAtivos=true'),
            api.get('/api/funcionarios?apenasAtivos=true'),
            api.get('/api/equipamentos?apenasAtivos=true'),
        ])
            .then(([cliRes, funcRes, eqRes]) => {
                setClientes(cliRes.data || []);
                setMotoristas((funcRes.data || []).filter(f => f.cargo === 'ENTREGADOR'));
                setEquipamentos(eqRes.data || []);
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data || err.message) }));
    }, []);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function handleItemChange(e) {
        const { name, value } = e.target;
        setNovoItem(prev => ({ ...prev, [name]: value }));
    }

    function handleSelectUnidade(e) {
        const unidadeId = e.target.value;
        const unidade = equipamentos
            .flatMap(eq => (eq.unidades || []).map(u => ({ ...u, equipamentoNome: eq.nome })))
            .find(u => String(u.id) === String(unidadeId));
        setNovoItem(prev => ({ ...prev, unidadeId }));
        if (unidade) {
            setNovoItem(prev => ({ ...prev, unidadeId: String(unidade.id) }));
        }
    }

    function unidadesDisponiveis() {
        return equipamentos.flatMap(eq =>
            (eq.unidades || [])
                .filter(u => u.status === 'DISPONIVEL')
                .map(u => ({ ...u, equipamentoNome: eq.nome }))
        );
    }

    function adicionarItem() {
        if (!novoItem.unidadeId) {
            setMessage({ type: 'error', text: 'Selecione uma unidade disponível.' });
            return;
        }
        const unidade = unidadesDisponiveis().find(u => String(u.id) === String(novoItem.unidadeId));
        if (!unidade) {
            setMessage({ type: 'error', text: 'Unidade não encontrada ou indisponível.' });
            return;
        }
        if (itens.some(i => String(i.unidadeId) === String(unidade.id))) {
            setMessage({ type: 'error', text: 'Esta unidade já está na lista.' });
            return;
        }
        setItens(prev => [...prev, {
            unidadeId: unidade.id,
            codigoPatrimonio: unidade.codigoPatrimonio || '---',
            equipamentoNome: unidade.equipamentoNome,
            quantidade: parseInt(novoItem.quantidade, 10) || 1,
            observacaoItem: novoItem.observacaoItem,
        }]);
        setNovoItem({ unidadeId: '', quantidade: 1, observacaoItem: '' });
        setMessage(null);
    }

    function removerItem(idx) {
        setItens(prev => prev.filter((_, i) => i !== idx));
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (itens.length === 0) {
            setMessage({ type: 'error', text: 'Adicione pelo menos um item à expedição.' });
            return;
        }
        setSaving(true);
        setMessage({ type: 'info', text: 'Criando expedição...' });

        const data = {
            tipo: form.tipo,
            clienteId: form.clienteId ? parseInt(form.clienteId, 10) : null,
            motoristaId: form.motoristaId ? parseInt(form.motoristaId, 10) : null,
            placaVeiculo: form.placaVeiculo,
            dataProgramada: form.dataProgramada,
            horarioProgramado: form.horarioProgramado,
            enderecoEntrega: form.enderecoEntrega,
            observacoes: form.observacoes,
            itens: itens.map(item => ({
                unidadeId: item.unidadeId,
                quantidade: item.quantidade,
                observacaoItem: item.observacaoItem,
            })),
        };

        api.post('/api/expedicoes', data)
            .then(() => {
                setMessage({ type: 'success', text: 'Expedição criada com sucesso!' });
                setTimeout(() => {
                    onChanged();
                    onClose();
                }, 1200);
            })
            .catch(err => {
                const msg = typeof err.response?.data === 'string'
                    ? err.response.data
                    : (err.response?.data?.message || err.response?.data || err.message);
                setMessage({ type: 'error', text: 'Erro ao criar: ' + msg });
            })
            .finally(() => setSaving(false));
    }

    const unidades = unidadesDisponiveis();

    return (
        <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard novaExpedicaoModalCard">
                <div className="modalHeader">
                    <h3><FontAwesomeIcon icon={faTruck} /> Nova Expedição</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="formGrid">
                        <div className="formField">
                            <label>Tipo</label>
                            <select className="equipInput" name="tipo" value={form.tipo} onChange={handleChange}>
                                <option value="ENTREGA">Entrega</option>
                                <option value="COLETA">Coleta / Devolução</option>
                            </select>
                        </div>
                        <div className="formField">
                            <label>Data Programada</label>
                            <input className="equipInput" type="date" name="dataProgramada" value={form.dataProgramada} onChange={handleChange} required />
                        </div>
                        <div className="formField">
                            <label>Horário</label>
                            <input className="equipInput" type="time" name="horarioProgramado" value={form.horarioProgramado} onChange={handleChange} />
                        </div>
                    </div>

                    <div className="formGrid">
                        <div className="formField">
                            <label>Cliente</label>
                            <select className="equipInput" name="clienteId" value={form.clienteId} onChange={handleChange}>
                                <option value="">Selecione...</option>
                                {clientes.map(c => (
                                    <option key={c.id} value={c.id}>{c.nome}</option>
                                ))}
                            </select>
                        </div>
                        <div className="formField">
                            <label>Motorista</label>
                            <select className="equipInput" name="motoristaId" value={form.motoristaId} onChange={handleChange}>
                                <option value="">Selecione...</option>
                                {motoristas.map(m => (
                                    <option key={m.id} value={m.id}>{m.nome}</option>
                                ))}
                            </select>
                        </div>
                        <div className="formField">
                            <label>Placa do Veículo</label>
                            <input className="equipInput" name="placaVeiculo" placeholder="ABC-1234" value={form.placaVeiculo} onChange={handleChange} />
                        </div>
                    </div>

                    <div className="formField">
                        <label>Endereço de Entrega/Coleta</label>
                        <input className="equipInput" name="enderecoEntrega" placeholder="Endereço completo..." value={form.enderecoEntrega} onChange={handleChange} />
                    </div>

                    <div className="formField">
                        <label>Observações</label>
                        <textarea className="equipTextarea" name="observacoes" rows={2} placeholder="Observações da expedição..." value={form.observacoes} onChange={handleChange} />
                    </div>

                    {/* ===== ITENS ===== */}
                    <div className="specsContainer">
                        <div className="specsHeader">
                            <label><FontAwesomeIcon icon={faTruckLoading} /> Itens da Expedição</label>
                        </div>

                        <div className="unidadeFormRow">
                            <select className="equipInput" value={novoItem.unidadeId} onChange={handleSelectUnidade}>
                                <option value="">Selecione unidade disponível...</option>
                                {unidades.map(u => (
                                    <option key={u.id} value={u.id}>
                                        {u.codigoPatrimonio || '---'} — {u.equipamentoNome}
                                    </option>
                                ))}
                            </select>
                            <input
                                className="equipInput"
                                type="number"
                                min="1"
                                placeholder="Qtd"
                                value={novoItem.quantidade}
                                onChange={e => setNovoItem(prev => ({ ...prev, quantidade: e.target.value }))}
                            />
                            <input
                                className="equipInput"
                                placeholder="Obs. do item"
                                value={novoItem.observacaoItem}
                                onChange={e => setNovoItem(prev => ({ ...prev, observacaoItem: e.target.value }))}
                            />
                            <button type="button" className="addBtn" onClick={adicionarItem}>
                                <FontAwesomeIcon icon={faPlus} /> Adicionar
                            </button>
                        </div>

                        {itens.length > 0 && (
                            <div className="tableWrapper">
                                <table className="usersTable">
                                    <thead>
                                        <tr>
                                            <th>Patrimônio</th>
                                            <th>Equipamento</th>
                                            <th>Qtd</th>
                                            <th>Obs.</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {itens.map((item, idx) => (
                                            <tr key={idx} className="tableRow">
                                                <td>{item.codigoPatrimonio}</td>
                                                <td>{item.equipamentoNome}</td>
                                                <td>{item.quantidade}</td>
                                                <td>{item.observacaoItem || '---'}</td>
                                                <td>
                                                    <button type="button" className="actionBtn delete" onClick={() => removerItem(idx)}>
                                                        <FontAwesomeIcon icon={faTrash} />
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>

                    <div className="formFooter" style={{ justifyContent: 'flex-end' }}>
                        <button type="button" className="smallBtn" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="addBtn" disabled={saving}>
                            {saving ? 'Criando...' : 'Criar Expedição'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ============================================================
   MODAL DE DETALHES DA EXPEDIÇÃO — Check-in/Check-out,
   Vistorias e assinatura
   ============================================================ */
function ExpedicaoDetalheModal({ expedicao, onClose, onChanged }) {
    const [assinatura, setAssinatura] = useState('');
    const [savingAssinatura, setSavingAssinatura] = useState(false);
    const [changingStatus, setChangingStatus] = useState(false);
    const [showVistoriaModal, setShowVistoriaModal] = useState(false);
    const [message, setMessage] = useState(null);

    function handleStatusChange(novoStatus) {
        setChangingStatus(true);
        setMessage(null);
        api.patch(`/api/expedicoes/${expedicao.id}/status`, { status: novoStatus })
            .then(() => {
                setMessage({ type: 'success', text: 'Status atualizado!' });
                onChanged();
            })
            .catch(err => {
                const msg = typeof err.response?.data === 'string'
                    ? err.response.data
                    : (err.response?.data?.message || err.response?.data || err.message);
                setMessage({ type: 'error', text: 'Erro: ' + msg });
            })
            .finally(() => setChangingStatus(false));
    }

    function handleSaveAssinatura() {
        if (!assinatura) {
            setMessage({ type: 'error', text: 'Digite a assinatura do cliente.' });
            return;
        }
        setSavingAssinatura(true);
        api.patch(`/api/expedicoes/${expedicao.id}/assinatura`, { assinatura })
            .then(() => {
                setMessage({ type: 'success', text: 'Assinatura registrada!' });
                setAssinatura('');
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }))
            .finally(() => setSavingAssinatura(false));
    }

    const vistoriasEntrega = (expedicao.vistorias || []).filter(v => v.tipo === 'ENTREGA');
    const vistoriasDevolucao = (expedicao.vistorias || []).filter(v => v.tipo === 'DEVOLUCAO');

    return (
        <div className="modalBackdrop" style={{ inset: 0, position: 'fixed', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard detalheModalCard">
                <div className="modalHeader">
                    <h3>
                        <FontAwesomeIcon icon={faTruck} /> {expedicao.codigo}
                        <span className={`expeStatusTag ${expedicao.status?.toLowerCase()}`}>
                            {STATUS_EXPEDICAO_LABEL[expedicao.status] || expedicao.status}
                        </span>
                    </h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                {/* INFO GERAL */}
                <div className="detalheInfoGrid">
                    <div className="detalheInfoItem">
                        <strong>Tipo:</strong> {TIPO_EXPEDICAO_LABEL[expedicao.tipo] || expedicao.tipo}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Cliente:</strong> {expedicao.clienteNome || '---'}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Motorista:</strong> {expedicao.motoristaNome || '---'}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Placa:</strong> {expedicao.placaVeiculo || '---'}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Data:</strong> {formatDateOnly(expedicao.dataProgramada)}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Horário:</strong> {expedicao.horarioProgramado || '---'}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Endereço:</strong> {expedicao.enderecoEntrega || '---'}
                    </div>
                    <div className="detalheInfoItem">
                        <strong>Observações:</strong> {expedicao.observacoes || '---'}
                    </div>
                    {expedicao.checkoutEm && (
                        <div className="detalheInfoItem">
                            <strong>Check-out:</strong> {formatDate(expedicao.checkoutEm)}
                        </div>
                    )}
                    {expedicao.checkinEm && (
                        <div className="detalheInfoItem">
                            <strong>Check-in:</strong> {formatDate(expedicao.checkinEm)}
                        </div>
                    )}
                </div>

                {/* STATUS / CHECK-IN / CHECK-OUT */}
                <div className="checkinSection">
                    <h4><FontAwesomeIcon icon={faClipboardList} /> Check-in / Check-out</h4>
                    <div className="checkinActions">
                        {expedicao.status === 'AGENDADO' && (
                            <button
                                className="smallBtn success"
                                disabled={changingStatus}
                                onClick={() => handleStatusChange('EM_TRANSITO')}
                            >
                                <FontAwesomeIcon icon={faTruckLoading} /> Iniciar (Check-out)
                            </button>
                        )}
                        {expedicao.status === 'EM_TRANSITO' && (
                            <button
                                className="smallBtn success"
                                disabled={changingStatus}
                                onClick={() => handleStatusChange('CONCLUIDO')}
                            >
                                <FontAwesomeIcon icon={faCheckCircle} /> Concluir (Check-in)
                            </button>
                        )}
                        {(expedicao.status === 'AGENDADO' || expedicao.status === 'EM_TRANSITO') && (
                            <button
                                className="smallBtn delete"
                                disabled={changingStatus}
                                onClick={() => {
                                    if (window.confirm('Cancelar esta expedição?')) handleStatusChange('CANCELADO');
                                }}
                            >
                                <FontAwesomeIcon icon={faTimesCircle} /> Cancelar
                            </button>
                        )}
                    </div>
                </div>

                {/* ITENS */}
                <div className="checkinSection">
                    <h4><FontAwesomeIcon icon={faTruckLoading} /> Itens ({expedicao.itens?.length || 0})</h4>
                    <div className="tableWrapper">
                        <table className="usersTable">
                            <thead>
                                <tr>
                                    <th>Patrimônio</th>
                                    <th>Equipamento</th>
                                    <th>Qtd</th>
                                    <th>Obs.</th>
                                </tr>
                            </thead>
                            <tbody>
                                {(expedicao.itens || []).map(item => (
                                    <tr key={item.id} className="tableRow">
                                        <td>{item.codigoPatrimonio || '---'}</td>
                                        <td>{item.equipamentoNome || '---'}</td>
                                        <td>{item.quantidade}</td>
                                        <td>{item.observacaoItem || '---'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* VISTORIAS DE ENTREGA */}
                <div className="checkinSection">
                    <h4>
                        <FontAwesomeIcon icon={faClipboardCheck} /> Vistorias de Entrega ({vistoriasEntrega.length})
                        <button className="smallBtn success" onClick={() => setShowVistoriaModal(true)}>
                            <FontAwesomeIcon icon={faPen} /> Nova
                        </button>
                    </h4>
                    {vistoriasEntrega.length === 0 ? (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>Nenhuma vistoria de entrega registrada.</p>
                    ) : (
                        <div className="vistoriaList">
                            {vistoriasEntrega.map(v => (
                                <VistoriaCard key={v.id} vistoria={v} />
                            ))}
                        </div>
                    )}
                </div>

                {/* VISTORIAS DE DEVOLUÇÃO */}
                <div className="checkinSection">
                    <h4>
                        <FontAwesomeIcon icon={faClipboardCheck} /> Vistorias de Devolução ({vistoriasDevolucao.length})
                        <button className="smallBtn success" onClick={() => setShowVistoriaModal(true)}>
                            <FontAwesomeIcon icon={faPen} /> Nova
                        </button>
                    </h4>
                    {vistoriasDevolucao.length === 0 ? (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>Nenhuma vistoria de devolução registrada.</p>
                    ) : (
                        <div className="vistoriaList">
                            {vistoriasDevolucao.map(v => (
                                <VistoriaCard key={v.id} vistoria={v} />
                            ))}
                        </div>
                    )}
                </div>

                {/* ASSINATURA */}
                <div className="checkinSection">
                    <h4><FontAwesomeIcon icon={faSignature} /> Assinatura do Cliente</h4>
                    {expedicao.assinaturaCliente ? (
                        <div className="assinaturaRegistrada">
                            <p><strong>Assinatura registrada:</strong></p>
                            <p className="assinaturaText">{expedicao.assinaturaCliente}</p>
                        </div>
                    ) : (
                        <>
                            <input
                                className="equipInput"
                                placeholder="Nome/assinatura do cliente"
                                value={assinatura}
                                onChange={e => setAssinatura(e.target.value)}
                                style={{ marginBottom: '10px' }}
                            />
                            <button className="addBtn" onClick={handleSaveAssinatura} disabled={savingAssinatura}>
                                <FontAwesomeIcon icon={faSave} /> {savingAssinatura ? 'Salvando...' : 'Registrar Assinatura'}
                            </button>
                        </>
                    )}
                </div>
            </div>

            {showVistoriaModal && (
                <VistoriaModal
                    expedicao={expedicao}
                    onClose={() => setShowVistoriaModal(false)}
                    onChanged={onChanged}
                />
            )}
        </div>
    );
}

/* ============================================================
   CARD DE VISTORIA (resumo)
   ============================================================ */
function VistoriaCard({ vistoria }) {
    return (
        <div className="vistoriaCard">
            <div className="vistoriaCardHeader">
                <span className={`vistoriaTipoTag ${vistoria.tipo?.toLowerCase()}`}>
                    {vistoria.tipo === 'ENTREGA' ? 'Entrega' : 'Devolução'}
                </span>
                <span className="vistoriaCondicao">{vistoria.condicaoGeral || '---'}</span>
                <span className="vistoriaData">{formatDate(vistoria.realizadaEm)}</span>
            </div>
            {vistoria.codigoPatrimonio && (
                <p><strong>Patrimônio:</strong> {vistoria.codigoPatrimonio}</p>
            )}
            {vistoria.avariasExistentes && (
                <p><strong>Avarias pré-existentes:</strong> {vistoria.avariasExistentes}</p>
            )}
            {vistoria.danosCausados && (
                <p><strong>Danos causados:</strong> {vistoria.danosCausados}</p>
            )}
            {vistoria.observacoes && (
                <p><strong>Obs.:</strong> {vistoria.observacoes}</p>
            )}
            {vistoria.nomeResponsavel && (
                <p><strong>Responsável:</strong> {vistoria.nomeResponsavel}</p>
            )}
            {vistoria.fotos && vistoria.fotos.length > 0 && (
                <div className="vistoriaFotos">
                    {vistoria.fotos.map(foto => (
                        <img key={foto.id} src={imageUrl(foto.url)} alt={foto.legenda || 'Foto'} />
                    ))}
                </div>
            )}
        </div>
    );
}

/* ============================================================
   PÁGINA PRINCIPAL — Painel de Expedição
   ============================================================ */
export default function Expedicao() {
    const { user } = useAuth();
    const [expedicoes, setExpedicoes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [filtroData, setFiltroData] = useState(new Date().toISOString().split('T')[0]);
    const [filtroStatus, setFiltroStatus] = useState('');
    const [filtroTipo, setFiltroTipo] = useState('');
    const [showNova, setShowNova] = useState(false);
    const [detalheId, setDetalheId] = useState(null);
    const [message, setMessage] = useState(null);

    function fetchList(data = filtroData, status = filtroStatus) {
        setLoading(true);
        const params = {};
        if (data) params.data = data;
        if (status) params.status = status;
        api.get('/api/expedicoes', { params })
            .then(response => setExpedicoes(response.data || []))
            .catch(err => setMessage({ type: 'error', text: 'Erro ao buscar: ' + (err.response?.data || err.message) }))
            .finally(() => setLoading(false));
    }

    useEffect(() => {
        fetchList();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    if (!canAccessAdminRoute(user, '/admin/expedicao')) {
        return <Navigate to="/admin" replace />;
    }

    function applyFilters() {
        fetchList(filtroData, filtroStatus);
    }

    function handleDelete(id) {
        if (!window.confirm('Excluir esta expedição?')) return;
        api.delete(`/api/expedicoes/${id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Expedição excluída!' });
                fetchList();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    const filtered = useMemo(() => {
        let list = expedicoes;
        if (filtroTipo) list = list.filter(e => e.tipo === filtroTipo);
        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            list = list.filter(e =>
                (e.codigo || '').toLowerCase().includes(term) ||
                (e.clienteNome || '').toLowerCase().includes(term) ||
                (e.motoristaNome || '').toLowerCase().includes(term) ||
                (e.placaVeiculo || '').toLowerCase().includes(term)
            );
        }
        return list;
    }, [expedicoes, filtroTipo, searchTerm]);

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Painel de Expedição</h2>
                <div className="headerRight">
                    <button className="addBtn" onClick={() => setShowNova(true)}>
                        <FontAwesomeIcon icon={faPlus} /> Nova Expedição
                    </button>
                </div>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
            )}

            {/* FILTROS */}
            <div className="settingsCard">
                <div className="expedicaoFiltros">
                    <div className="searchBox">
                        <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                        <input
                            className="searchInput"
                            placeholder="Buscar por código, cliente, motorista..."
                            value={searchTerm}
                            onChange={e => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <input
                        className="equipInput"
                        type="date"
                        value={filtroData}
                        onChange={e => setFiltroData(e.target.value)}
                        style={{ maxWidth: '180px' }}
                    />
                    <select
                        className="equipInput"
                        value={filtroTipo}
                        onChange={e => setFiltroTipo(e.target.value)}
                        style={{ maxWidth: '160px' }}
                    >
                        <option value="">Todos os tipos</option>
                        <option value="ENTREGA">Entrega</option>
                        <option value="COLETA">Coleta</option>
                    </select>
                    <select
                        className="equipInput"
                        value={filtroStatus}
                        onChange={e => setFiltroStatus(e.target.value)}
                        style={{ maxWidth: '180px' }}
                    >
                        <option value="">Todos os status</option>
                        <option value="AGENDADO">Agendado</option>
                        <option value="EM_TRANSITO">Em trânsito</option>
                        <option value="CONCLUIDO">Concluído</option>
                        <option value="CANCELADO">Cancelado</option>
                    </select>
                    <button className="smallBtn" onClick={applyFilters}>
                        <FontAwesomeIcon icon={faSearch} /> Aplicar
                    </button>
                </div>
            </div>

            {/* TABELA */}
            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Programação de {formatDateOnly(filtroData)}</h3>
                </div>

                <div className="tableWrapper">
                    <table className="usersTable">
                        <thead>
                            <tr>
                                <th>Código</th>
                                <th>Tipo</th>
                                <th>Cliente</th>
                                <th>Motorista</th>
                                <th>Data</th>
                                <th>Status</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filtered.map(e => (
                                <tr key={e.id} className="tableRow">
                                    <td className="nameCell">
                                        <div className="userCell">
                                            <div className="userCellAvatar expeAvatar">
                                                <FontAwesomeIcon icon={faTruck} />
                                            </div>
                                            <div>
                                                <div className="userName">{e.codigo}</div>
                                                <div className="userRole">
                                                    {e.placaVeiculo ? `Placa: ${e.placaVeiculo}` : 'Sem veículo'}
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span className={`typeTag ${e.tipo?.toLowerCase()}`}>
                                            {TIPO_EXPEDICAO_LABEL[e.tipo] || e.tipo}
                                        </span>
                                    </td>
                                    <td>{e.clienteNome || '---'}</td>
                                    <td>{e.motoristaNome || '---'}</td>
                                    <td className="dateCell">{formatDateOnly(e.dataProgramada)} {e.horarioProgramado || ''}</td>
                                    <td>
                                        <span className={`expeStatusTag ${e.status?.toLowerCase()}`}>
                                            {STATUS_EXPEDICAO_LABEL[e.status] || e.status}
                                        </span>
                                    </td>
                                    <td className="actionsCell">
                                        <button className="actionBtn view" onClick={() => setDetalheId(e.id)} title="Ver detalhes / Vistoria">
                                            <FontAwesomeIcon icon={faClipboardCheck} />
                                        </button>
                                        <button className="actionBtn delete" onClick={() => handleDelete(e.id)} title="Excluir">
                                            <FontAwesomeIcon icon={faTrash} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!loading && filtered.length === 0 && (
                                <tr className="tableRow">
                                    <td colSpan="7" style={{ textAlign: 'center', color: '#999' }}>
                                        Nenhuma expedição encontrada para os filtros selecionados.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {showNova && (
                <NovaExpedicaoModal
                    onClose={() => setShowNova(false)}
                    onChanged={() => fetchList()}
                />
            )}

            {detalheId && (
                <ExpedicaoDetalheModal
                    expedicao={filtered.find(e => e.id === detalheId)}
                    onClose={() => setDetalheId(null)}
                    onChanged={() => fetchList()}
                />
            )}
        </div>
    );
}