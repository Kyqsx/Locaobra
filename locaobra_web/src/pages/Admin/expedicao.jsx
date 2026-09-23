import React, { useEffect, useState, useMemo, useRef } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import api from '../../service/api';
import './Expedicao.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faPlus, faTrash, faList, faTruck, faClipboardCheck,
    faSearch, faTruckLoading, faCheckCircle,
    faTimesCircle, faCamera, faPen, faSignature,
    faXmark,
} from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';
import EnderecoFields from '../../components/EnderecoFields';
import AssinaturaPad from '../../components/AssinaturaPad';
import { comprimirImagem, formatarTamanho, TAMANHO_MAX_UPLOAD, imageUrl } from '../../utils/imagem';
import { formatDate } from '../../utils/formatters';
import FileDropzone from '../../components/FileDropzone';
import ImagePreviewGrid from '../../components/ImagePreviewGrid';

const STATUS_EXPEDICAO_LABEL = {
    AGENDADO: 'Agendado',
    EM_TRANSITO: 'Em trânsito',
    ENTREGUE: 'Entregue',
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

function mensagemDeErro(err) {
    const d = err.response?.data;
    return typeof d === 'string' ? d : (d?.message || err.message);
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
function VistoriaModal({ expedicao, tipoInicial, onClose, onChanged }) {
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [form, setForm] = useState({
        unidadeId: '',
        tipo: tipoInicial || 'ENTREGA',
        condicaoGeral: 'BOM',
        avariasExistentes: '',
        danosCausados: '',
        observacoes: '',
    });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);

    // Só lista as unidades que ainda NÃO têm vistoria desse tipo (a API recusa duplicada).
    const unidades = useMemo(() => {
        const itens = expedicao?.itens || [];
        const jaVistoriadas = new Set(
            (expedicao?.vistorias || [])
                .filter(v => v.tipo === (tipoInicial || 'ENTREGA'))
                .map(v => v.unidadeId)
        );
        const unique = [];
        const seen = new Set();
        itens.forEach(item => {
            if (item.unidadeId && !seen.has(item.unidadeId) && !jaVistoriadas.has(item.unidadeId)) {
                seen.add(item.unidadeId);
                unique.push({
                    id: item.unidadeId,
                    codigoPatrimonio: item.codigoPatrimonio || 'Sem patrimônio',
                    equipamentoNome: item.equipamentoNome || 'Equipamento',
                });
            }
        });
        return unique;
    }, [expedicao, tipoInicial]);

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

    function handleFiles(files) {
        setSelectedFiles(prev => [...prev, ...files]);
    }

    function removeFile(idx) {
        setSelectedFiles(prev => prev.filter((_, i) => i !== idx));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (!form.unidadeId) {
            setMessage({ type: 'error', text: 'Selecione a unidade para vistoriar.' });
            return;
        }
        const ehEntrega = form.tipo === 'ENTREGA';
        // A vistoria de pré-saída é a PROVA do estado em que o equipamento saiu.
        if (ehEntrega && selectedFiles.length === 0) {
            setMessage({ type: 'error', text: 'Anexe ao menos uma foto do estado do equipamento.' });
            return;
        }
        if (!ehEntrega && form.danosCausados.trim() && selectedFiles.length === 0) {
            setMessage({ type: 'error', text: 'Anexe ao menos uma foto dos danos registrados.' });
            return;
        }

        setSaving(true);
        setMessage({ type: 'info', text: 'Preparando fotos...' });

        try {
            // Foto de celular tem vários MB; a Vercel recusa envios > 4,5 MB.
            const fotos = await Promise.all(selectedFiles.map(f => comprimirImagem(f)));
            const total = fotos.reduce((soma, f) => soma + f.size, 0);
            if (total > TAMANHO_MAX_UPLOAD) {
                setMessage({
                    type: 'error',
                    text: `As fotos somam ${formatarTamanho(total)} e o limite por envio é ${formatarTamanho(TAMANHO_MAX_UPLOAD)}. Remova algumas fotos.`,
                });
                return;
            }

            setMessage({ type: 'info', text: 'Salvando vistoria...' });

            // Cada tipo de vistoria guarda só o seu campo de avarias.
            const vistoriaData = {
                unidadeId: parseInt(form.unidadeId, 10),
                tipo: form.tipo,
                condicaoGeral: form.condicaoGeral,
                observacoes: form.observacoes,
                ...(ehEntrega
                    ? { avariasExistentes: form.avariasExistentes }
                    : { danosCausados: form.danosCausados }),
            };

            const formData = new FormData();
            formData.append('vistoria', JSON.stringify(vistoriaData));
            fotos.forEach(file => formData.append('fotos', file));

            await api.post(`/api/expedicoes/${expedicao.id}/vistorias`, formData);

            setMessage({ type: 'success', text: 'Vistoria registrada com sucesso!' });
            setTimeout(() => {
                onChanged();
                onClose();
            }, 1200);
        } catch (err) {
            const msg = typeof err.response?.data === 'string'
                ? err.response.data
                : (err.response?.data?.message || err.response?.data || err.message);
            setMessage({ type: 'error', text: 'Erro ao salvar vistoria: ' + msg });
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="modalBackdrop modalBackdropAdmin">
            <div className="modalCard vistoriaModalCard">
                <div className="modalHeader">
                    <h3>
                        <FontAwesomeIcon icon={faClipboardCheck} />{' '}
                        {form.tipo === 'ENTREGA' ? 'Termo de Vistoria — Entrega' : 'Termo de Vistoria — Devolução'}
                    </h3>
                    <button type="button" className="btn btn-error" onClick={onClose}><FontAwesomeIcon icon={faXmark} /></button>
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
                                <option value="">{unidades.length === 0 ? 'Todas as unidades já foram vistoriadas' : 'Selecione...'}</option>
                                {unidades.map(u => (
                                    <option key={u.id} value={u.id}>{u.codigoPatrimonio} — {u.equipamentoNome}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="formGrid">
                        <div className="formField">
                            <label>Tipo de Vistoria</label>
                            {tipoInicial ? (
                                // Travado: essa modal já foi aberta a partir da seção certa
                                // (Entrega ou Devolução), então não deixamos trocar por engano.
                                <input
                                    className="equipInput"
                                    value={form.tipo === 'ENTREGA' ? 'Vistoria de Entrega' : 'Vistoria de Devolução'}
                                    readOnly
                                    disabled
                                />
                            ) : (
                                <select className="equipInput" name="tipo" value={form.tipo} onChange={handleChange}>
                                    <option value="ENTREGA">Vistoria de Entrega</option>
                                    <option value="DEVOLUCAO">Vistoria de Devolução</option>
                                </select>
                            )}
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

                    {form.condicaoGeral === 'RUIM' && form.tipo === 'ENTREGA' && (
                        <div className="messageBanner negative">
                            Condição RUIM reprova a unidade: a saída desta expedição fica bloqueada até a
                            vistoria ser refeita após o reparo, ou a expedição ser cancelada (a unidade
                            vai para a manutenção).
                        </div>
                    )}

                    {form.tipo === 'ENTREGA' ? (
                        <div className="formField">
                            <label>Avarias pré-existentes</label>
                            <textarea
                                className="equipTextarea"
                                name="avariasExistentes"
                                rows={2}
                                placeholder="Registre avarias que o equipamento já tem antes de sair (riscos, peças soltas...)"
                                value={form.avariasExistentes}
                                onChange={handleChange}
                            />
                        </div>
                    ) : (
                        <div className="formField">
                            <label>Danos causados pelo cliente</label>
                            <textarea
                                className="equipTextarea"
                                name="danosCausados"
                                rows={2}
                                placeholder="Registre danos causados durante o período de locação (exige foto)..."
                                value={form.danosCausados}
                                onChange={handleChange}
                            />
                        </div>
                    )}

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

                    {form.tipo === 'ENTREGA' && (
                        <div className="formField">
                            <label>Pessoas autorizadas a receber</label>
                            <input
                                className="equipInput"
                                value={(expedicao.nomesAutorizados || []).join(', ') || 'Nenhuma definida na expedição'}
                                readOnly
                                disabled
                            />
                            <p style={{ color: '#999', fontSize: '0.8rem', margin: '4px 0 0' }}>
                                Definidas na criação da expedição — a assinatura de quem recebeu é registrada no passo 3 (Confirmação de Entrega).
                            </p>
                        </div>
                    )}

                    <div className="formField">
                        <label>
                            <FontAwesomeIcon icon={faCamera} /> Fotos do estado do equipamento
                            {form.tipo === 'ENTREGA' ? ' (obrigatório — mínimo 1)' : ''}
                        </label>
                        <FileDropzone
                            accept="image/*"
                            multiple
                            label="Arraste as fotos aqui"
                            onFiles={handleFiles}
                        />
                        <ImagePreviewGrid files={selectedFiles} onRemove={removeFile} />
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
function NovaExpedicaoModal({ onClose, onChanged, pedidoOrigem, depositoOrigemId }) {
    const [clientes, setClientes] = useState([]);
    const [motoristas, setMotoristas] = useState([]);
    const [equipamentos, setEquipamentos] = useState([]);
    const [entregasParaColeta, setEntregasParaColeta] = useState([]);
    const [form, setForm] = useState({
        tipo: 'ENTREGA',
        clienteId: pedidoOrigem ? String(pedidoOrigem.clienteId || '') : '',
        motoristaId: '',
        entregaOrigemId: '',
        placaVeiculo: '',
        dataProgramada: pedidoOrigem?.dataInicio || new Date().toISOString().split('T')[0],
        horarioProgramado: '08:00',
        enderecoEntrega: pedidoOrigem?.enderecoEntrega
            ? {
                cep: pedidoOrigem.enderecoEntrega.cep || '',
                rua: pedidoOrigem.enderecoEntrega.rua || '',
                numero: pedidoOrigem.enderecoEntrega.numero || '',
                complemento: pedidoOrigem.enderecoEntrega.complemento || '',
                bairro: pedidoOrigem.enderecoEntrega.bairro || '',
                cidade: pedidoOrigem.enderecoEntrega.cidade || '',
                estado: pedidoOrigem.enderecoEntrega.estado || '',
            }
            : { cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '' },
        observacoes: '',
        nomeAutorizado1: '',
        nomeAutorizado2: '',
        nomeAutorizado3: '',
    });
    const [itens, setItens] = useState([]);
    const [novoItem, setNovoItem] = useState({ unidadeId: '', quantidade: 1, observacaoItem: '' });
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState(null);

    const ehColeta = form.tipo === 'COLETA';
    const entregaSelecionada = entregasParaColeta.find(e => String(e.id) === String(form.entregaOrigemId));
    // Quando o pedido foi desmembrado em mais de um depósito, essa expedição
    // cobre só os itens do depósito escolhido (ver Admin/pedidos.jsx, fila
    // do conferente) — o resto vira outra expedição depois.
    const itensDoDeposito = pedidoOrigem
        ? (pedidoOrigem.itens || []).filter(i => !depositoOrigemId || i.depositoId === depositoOrigemId)
        : [];

    useEffect(() => {
        Promise.all([
            api.get('/api/clientes?apenasAtivos=true'),
            api.get('/api/funcionarios?apenasAtivos=true'),
            api.get('/api/equipamentos?apenasAtivos=true'),
            api.get('/api/expedicoes/entregas-para-coleta'),
        ])
            .then(([cliRes, funcRes, eqRes, colRes]) => {
                setClientes(cliRes.data || []);
                setMotoristas((funcRes.data || []).filter(f => f.cargoNome === 'ENTREGADOR'));
                setEquipamentos(eqRes.data || []);
                setEntregasParaColeta(colRes.data || []);
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao carregar dados: ' + (err.response?.data || err.message) }));
    }, []);

    function handleChange(e) {
        const { name, value } = e.target;
        if (name === 'tipo') {
            // Trocar de tipo zera itens manuais (COLETA não usa essa lista,
            // os itens vêm da entrega selecionada) e a entrega/cliente escolhidos.
            // Expedição gerada a partir de um pedido é sempre ENTREGA — o select
            // fica desabilitado nesse caso, então isso nunca deve rodar ali.
            setItens([]);
            setForm(prev => ({ ...prev, tipo: value, entregaOrigemId: '', clienteId: pedidoOrigem ? prev.clienteId : '' }));
            return;
        }
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function handleSelectEntregaOrigem(e) {
        const entregaOrigemId = e.target.value;
        const entrega = entregasParaColeta.find(en => String(en.id) === String(entregaOrigemId));
        setForm(prev => ({
            ...prev,
            entregaOrigemId,
            // Pré-preenche com o endereço da entrega original — o conferente
            // pode editar se a coleta for buscar em outro lugar.
            enderecoEntrega: entrega?.enderecoEntrega
                ? {
                    cep: entrega.enderecoEntrega.cep || '',
                    rua: entrega.enderecoEntrega.rua || '',
                    numero: entrega.enderecoEntrega.numero || '',
                    complemento: entrega.enderecoEntrega.complemento || '',
                    bairro: entrega.enderecoEntrega.bairro || '',
                    cidade: entrega.enderecoEntrega.cidade || '',
                    estado: entrega.enderecoEntrega.estado || '',
                }
                : prev.enderecoEntrega,
        }));
    }

    function handleEnderecoChange(novoEndereco) {
        setForm(prev => ({ ...prev, enderecoEntrega: novoEndereco }));
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
                .filter(u => !pedidoOrigem || !depositoOrigemId || u.depositoId === depositoOrigemId)
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
            equipamentoId: unidade.equipamentoId,
            quantidade: 1, // cada item é UMA unidade física (patrimônio)
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
        if (ehColeta) {
            if (!form.entregaOrigemId) {
                setMessage({ type: 'error', text: 'Selecione qual entrega vai ser coletada.' });
                return;
            }
        } else {
            if (itens.length === 0) {
                setMessage({ type: 'error', text: 'Adicione pelo menos um item à expedição.' });
                return;
            }
            if (!pedidoOrigem && !form.clienteId) {
                setMessage({ type: 'error', text: 'Selecione o cliente da entrega.' });
                return;
            }
            if (!form.nomeAutorizado1.trim()) {
                setMessage({ type: 'error', text: 'Informe ao menos uma pessoa autorizada a receber o equipamento.' });
                return;
            }
            if (!pedidoConfere) {
                setMessage({ type: 'error', text: 'Os itens não conferem com o pedido: ' + pendenciasPedido.join(' · ') });
                return;
            }
        }
        if (!form.motoristaId) {
            setMessage({ type: 'error', text: 'Selecione o motorista (entregador).' });
            return;
        }
        if (!form.placaVeiculo.trim()) {
            setMessage({ type: 'error', text: 'Informe a placa do veículo.' });
            return;
        }
        setSaving(true);
        setMessage({ type: 'info', text: 'Criando expedição...' });

        const data = {
            tipo: form.tipo,
            clienteId: ehColeta ? null : (form.clienteId ? parseInt(form.clienteId, 10) : null),
            motoristaId: form.motoristaId ? parseInt(form.motoristaId, 10) : null,
            entregaOrigemId: ehColeta && form.entregaOrigemId ? parseInt(form.entregaOrigemId, 10) : null,
            pedidoId: pedidoOrigem ? pedidoOrigem.id : null,
            depositoId: pedidoOrigem ? depositoOrigemId : null,
            placaVeiculo: form.placaVeiculo,
            dataProgramada: form.dataProgramada,
            horarioProgramado: form.horarioProgramado,
            enderecoEntrega: form.enderecoEntrega,
            observacoes: form.observacoes,
            nomesAutorizados: ehColeta ? [] : [form.nomeAutorizado1, form.nomeAutorizado2, form.nomeAutorizado3]
                .map(n => (n || '').trim())
                .filter(n => n.length > 0),
            itens: ehColeta ? [] : itens.map(item => ({
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

    // Conferência com o pedido: mesmo equipamento e mesma quantidade que o cliente
    // pediu (neste depósito). A API também confere; aqui só avisamos antes.
    const esperadoPedido = {};
    itensDoDeposito.forEach(i => {
        const atual = esperadoPedido[i.equipamentoId] || { nome: i.equipamentoNome, qtd: 0 };
        esperadoPedido[i.equipamentoId] = { nome: atual.nome, qtd: atual.qtd + (i.quantidade || 0) };
    });
    const atualPedido = {};
    itens.forEach(i => {
        const atual = atualPedido[i.equipamentoId] || { nome: i.equipamentoNome, qtd: 0 };
        atualPedido[i.equipamentoId] = { nome: atual.nome, qtd: atual.qtd + 1 };
    });
    const pendenciasPedido = [];
    Object.entries(esperadoPedido).forEach(([id, e]) => {
        const qtd = atualPedido[id]?.qtd || 0;
        if (qtd !== e.qtd) pendenciasPedido.push(`${e.nome}: ${qtd} de ${e.qtd}`);
    });
    Object.entries(atualPedido).forEach(([id, a]) => {
        if (!esperadoPedido[id]) pendenciasPedido.push(`${a.nome} não faz parte do pedido`);
    });
    const pedidoConfere = !pedidoOrigem || pendenciasPedido.length === 0;

    return (
        <div className="modalBackdrop modalBackdropAdmin">
            <div className="modalCard novaExpedicaoModalCard">
                <div className="modalHeader">
                    <h3><FontAwesomeIcon icon={faTruck} /> {pedidoOrigem ? `Nova Expedição — Pedido ${pedidoOrigem.codigo}` : 'Nova Expedição'}</h3>
                    <button type="button" className="btn btn-error" onClick={onClose}><FontAwesomeIcon icon={faXmark} /></button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                {pedidoOrigem && (
                    <div className="specsContainer" style={{ marginBottom: '16px' }}>

                        <div className="tableWrapper">
                            <table className="usersTable">
                                <thead>
                                    <tr>
                                        <th>Equipamento</th>
                                        <th>Qtd. no pedido</th>
                                        <th>Obs. do cliente</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {itensDoDeposito.map(item => (
                                        <tr key={item.id} className="tableRow">
                                            <td>{item.equipamentoNome}</td>
                                            <td>{item.quantidade}</td>
                                            <td>{item.observacaoItem || '---'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="formGrid">
                        <div className="formField">
                            <label>Tipo</label>
                            <select className="equipInput" name="tipo" value={form.tipo} onChange={handleChange} disabled={!!pedidoOrigem}>
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
                        {ehColeta ? (
                            <div className="formField">
                                <label>Entrega a coletar</label>
                                <select className="equipInput" name="entregaOrigemId" value={form.entregaOrigemId} onChange={handleSelectEntregaOrigem} required>
                                    <option value="">Selecione a entrega já entregue...</option>
                                    {entregasParaColeta.map(en => (
                                        <option key={en.id} value={en.id}>
                                            {en.codigo} — {en.clienteNome || 'Sem cliente'} ({formatDateOnly(en.dataProgramada)})
                                        </option>
                                    ))}
                                </select>
                                {entregasParaColeta.length === 0 && (
                                    <p style={{ color: '#999', fontSize: '0.8rem', margin: '4px 0 0' }}>
                                        Nenhuma entrega aguardando coleta no momento.
                                    </p>
                                )}
                            </div>
                        ) : pedidoOrigem ? (
                            <div className="formField">
                                <label>Cliente</label>
                                <input className="equipInput" value={pedidoOrigem.clienteNome || '---'} readOnly disabled />
                            </div>
                        ) : (
                            <div className="formField">
                                <label>Cliente *</label>
                                <select className="equipInput" name="clienteId" value={form.clienteId} onChange={handleChange} required>
                                    <option value="">Selecione...</option>
                                    {clientes.map(c => (
                                        <option key={c.id} value={c.id}>{c.nome}</option>
                                    ))}
                                </select>
                            </div>
                        )}
                        <div className="formField">
                            <label>Motorista *</label>
                            <select className="equipInput" name="motoristaId" value={form.motoristaId} onChange={handleChange} required>
                                <option value="">Selecione...</option>
                                {motoristas.map(m => (
                                    <option key={m.id} value={m.id}>{m.nome}</option>
                                ))}
                            </select>
                        </div>
                        <div className="formField">
                            <label>Placa do Veículo *</label>
                            <input className="equipInput" name="placaVeiculo" placeholder="ABC-1234" value={form.placaVeiculo} onChange={handleChange} required />
                        </div>
                    </div>

                    {ehColeta && entregaSelecionada && (
                        <div className="formField">
                            <label>Cliente</label>
                            <input className="equipInput" value={entregaSelecionada.clienteNome || '---'} readOnly disabled />
                        </div>
                    )}

                    <div className="formField">
                        <label>Endereço de Entrega/Coleta</label>
                        {ehColeta && (
                            <p className="enderecoHintColeta">Pré-preenchido com o endereço da entrega original — edite se a coleta for buscar em outro lugar.</p>
                        )}
                        <EnderecoFields value={form.enderecoEntrega || {}} onChange={handleEnderecoChange} prefixo="expedicao" />
                    </div>

                    {ehColeta ? (
                        entregaSelecionada && (
                            <div className="formField">
                                <label>Pessoas autorizadas a receber (definidas na entrega)</label>
                                <input
                                    className="equipInput"
                                    value={(entregaSelecionada.nomesAutorizados || []).join(', ') || 'Nenhuma definida'}
                                    readOnly
                                    disabled
                                />
                            </div>
                        )
                    ) : (
                        <div className="formField">
                            <label>Pessoas autorizadas a receber (até 3 — pelo menos 1) *</label>
                            <div className="formGrid">
                                <input
                                    className="equipInput"
                                    name="nomeAutorizado1"
                                    placeholder="Nome 1"
                                    value={form.nomeAutorizado1}
                                    onChange={handleChange}
                                    required
                                />
                                <input
                                    className="equipInput"
                                    name="nomeAutorizado2"
                                    placeholder="Nome 2"
                                    value={form.nomeAutorizado2}
                                    onChange={handleChange}
                                />
                                <input
                                    className="equipInput"
                                    name="nomeAutorizado3"
                                    placeholder="Nome 3"
                                    value={form.nomeAutorizado3}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>
                    )}

                    <div className="formField">
                        <label>Observações</label>
                        <textarea className="equipTextarea" name="observacoes" rows={2} placeholder="Observações da expedição..." value={form.observacoes} onChange={handleChange} />
                    </div>

                    {/* ===== ITENS ===== */}
                    {ehColeta ? (
                        <div className="specsContainer">
                            <div className="specsHeader">
                                <label><FontAwesomeIcon icon={faTruckLoading} /> Itens a coletar</label>
                            </div>
                            {!entregaSelecionada ? (
                                <p style={{ color: '#999', fontSize: '0.85rem' }}>Selecione a entrega acima para ver os itens.</p>
                            ) : (
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
                                            {(entregaSelecionada.itens || []).map(item => (
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
                            )}
                            <p style={{ color: '#999', fontSize: '0.8rem', margin: '8px 0 0' }}>
                                Os itens da coleta são sempre os mesmos que saíram nessa entrega.
                            </p>
                        </div>
                    ) : (
                        <div className="specsContainer">
                            <div className="specsHeader">
                                <label><FontAwesomeIcon icon={faTruckLoading} /> Itens da Expedição</label>
                            </div>

                            <div className="unidadeFormRow">
                                <select className="equipInput" value={novoItem.unidadeId} onChange={handleSelectUnidade}>
                                    <option value="">Selecione</option>
                                    {unidades.map(u => (
                                        <option key={u.id} value={u.id}>
                                            {u.codigoPatrimonio || '---'} — {u.equipamentoNome}
                                        </option>
                                    ))}
                                </select>
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

                            {pedidoOrigem && (
                                <div className={`messageBanner ${pedidoConfere ? 'positive' : 'negative'}`} style={{ marginTop: 10 }}>
                                    {pedidoConfere
                                        ? 'Itens conferem com o pedido.'
                                        : 'Faltam ajustes para bater com o pedido: ' + pendenciasPedido.join(' · ')}
                                </div>
                            )}
                        </div>
                    )}

                    <div className="formFooter" style={{ justifyContent: 'flex-end' }}>
                        <button type="button" className="btn btn-secondary" onClick={onClose}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={saving || (!ehColeta && !pedidoConfere)}>
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

// Componente definido no nível do módulo (e não dentro de outro componente)
// para que sua identidade seja estável entre renders. Se fosse declarado dentro
// do modal, cada re-render criaria uma nova função e o React desmontaria/remontaria
// o <span> toda vez, o que causa o erro "insertBefore: ... is not a child of this node".
function StepBadge({ n, passoAtual, isFinalizada }) {
    const estado = n < passoAtual || isFinalizada ? 'done' : n === passoAtual ? 'current' : 'pending';
    return <span className={`stepBadge ${estado}`}>{estado === 'done' ? '✓' : n}</span>;
}

function ExpedicaoDetalheModal({ expedicao, onClose, onChanged }) {
    const { user } = useAuth();
    const [changingStatus, setChangingStatus] = useState(false);
    const [vistoriaModalTipo, setVistoriaModalTipo] = useState(null); // 'ENTREGA' | 'DEVOLUCAO' | null
    const [message, setMessage] = useState(null);
    // Prova da entrega (passo 3): quem assinou + documento + assinatura desenhada + foto.
    const [assinaturaEntrega, setAssinaturaEntrega] = useState('');
    const [documentoEntrega, setDocumentoEntrega] = useState('');
    const [observacaoEntrega, setObservacaoEntrega] = useState('');
    const [assinaturaDesenhada, setAssinaturaDesenhada] = useState(false);
    const assinaturaPadRef = useRef(null);
    const [fotoEntregaFile, setFotoEntregaFile] = useState(null);
    const [savingEntrega, setSavingEntrega] = useState(false);
    // Conferência do check-out: ids dos itens que o conferente já marcou.
    const [itensConferidos, setItensConferidos] = useState([]);

    const ehAdminOuGerente = user?.tipo === 'ADMIN' || user?.cargoFuncionario === 'GERENTE_OPERACOES';
    const ehEntregador = ehAdminOuGerente || user?.cargoFuncionario === 'ENTREGADOR';
    const ehConferente = ehAdminOuGerente || user?.cargoFuncionario === 'CONFERENTE';
    // Vistoria de entrega e de devolução são responsabilidade do técnico de
    // manutenção — é ele quem tem o olho técnico pra avaliar o equipamento.
    const ehTecnico = ehAdminOuGerente || user?.cargoFuncionario === 'TECNICO_MANUTENCAO';

    // Check-out (EM_TRANSITO, com a lista de itens conferidos) e check-in (CONCLUIDO).
    // Entrega no local e cancelamento têm rotas próprias (abaixo).
    function handleStatusChange(novoStatus, extras = {}) {
        setChangingStatus(true);
        setMessage(null);
        api.patch(`/api/expedicoes/${expedicao.id}/status`, { status: novoStatus, ...extras })
            .then(() => {
                setMessage({ type: 'success', text: 'Status atualizado!' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + mensagemDeErro(err) }))
            .finally(() => setChangingStatus(false));
    }

    function toggleItemConferido(itemId) {
        setItensConferidos(prev => prev.includes(itemId) ? prev.filter(id => id !== itemId) : [...prev, itemId]);
    }

    // Cancelar exige motivo (fica registrado na expedição). Depois que o
    // equipamento saiu do depósito, só o gerente cancela (o entregador tem o
    // botão "não consegui entregar").
    function handleCancelar() {
        const motivo = window.prompt('Motivo do cancelamento:');
        if (motivo === null) return;
        if (motivo.trim().length < 3) {
            setMessage({ type: 'error', text: 'Informe o motivo do cancelamento.' });
            return;
        }
        setChangingStatus(true);
        setMessage(null);
        api.post(`/api/expedicoes/${expedicao.id}/cancelar`, { motivo: motivo.trim() })
            .then(() => {
                setMessage({ type: 'success', text: 'Expedição cancelada.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + mensagemDeErro(err) }))
            .finally(() => setChangingStatus(false));
    }

    // O entregador chegou e não deu pra entregar/coletar (cliente ausente,
    // recusou, endereço não encontrado). Encerra a expedição com o motivo; na
    // entrega, as unidades voltam pro estoque e o pedido volta pra fila.
    function handleNaoRealizada() {
        const motivo = window.prompt(
            `Por que ${ehColetaDetalhe ? 'a coleta' : 'a entrega'} não foi realizada? (ex.: cliente ausente, recusou receber, endereço não encontrado)`
        );
        if (motivo === null) return;
        if (motivo.trim().length < 5) {
            setMessage({ type: 'error', text: 'Descreva o motivo (mínimo 5 caracteres).' });
            return;
        }
        setChangingStatus(true);
        setMessage(null);
        api.post(`/api/expedicoes/${expedicao.id}/nao-realizada`, { motivo: motivo.trim() })
            .then(() => {
                setMessage({ type: 'success', text: 'Ocorrência registrada. A expedição foi encerrada.' });
                onChanged();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro: ' + mensagemDeErro(err) }))
            .finally(() => setChangingStatus(false));
    }

    // Confirmação no local (passo 3). Prova: nome + documento de quem assinou,
    // assinatura DESENHADA e foto tirada na hora. A data/hora é a do servidor.
    // A foto é reduzida antes do envio (a Vercel recusa requisições > 4,5 MB).
    async function handleConfirmarEntrega() {
        if (!assinaturaEntrega.trim()) {
            setMessage({ type: 'error', text: 'Informe quem está assinando o recebimento.' });
            return;
        }
        if (documentoEntrega.trim().length < 5) {
            setMessage({ type: 'error', text: 'Informe o documento (CPF ou RG) de quem assinou.' });
            return;
        }
        if (!assinaturaPadRef.current || assinaturaPadRef.current.vazio()) {
            setMessage({ type: 'error', text: 'Peça para a pessoa assinar no quadro.' });
            return;
        }
        if (!fotoEntregaFile) {
            setMessage({ type: 'error', text: 'Anexe a foto tirada no local.' });
            return;
        }
        setSavingEntrega(true);
        setMessage(null);

        try {
            const [foto, assinaturaBlob] = await Promise.all([
                comprimirImagem(fotoEntregaFile),
                assinaturaPadRef.current.toBlob(),
            ]);

            const formData = new FormData();
            formData.append('assinatura', assinaturaEntrega.trim());
            formData.append('documento', documentoEntrega.trim());
            formData.append('assinaturaImagem', assinaturaBlob, 'assinatura.png');
            formData.append('foto', foto);
            if (observacaoEntrega.trim()) formData.append('observacao', observacaoEntrega.trim());

            await api.post(`/api/expedicoes/${expedicao.id}/confirmar-entrega`, formData);

            setMessage({ type: 'success', text: ehColetaDetalhe ? 'Coleta confirmada!' : 'Entrega confirmada!' });
            setAssinaturaEntrega('');
            setDocumentoEntrega('');
            setObservacaoEntrega('');
            setFotoEntregaFile(null);
            setAssinaturaDesenhada(false);
            onChanged();
        } catch (err) {
            setMessage({ type: 'error', text: 'Erro: ' + mensagemDeErro(err) });
        } finally {
            setSavingEntrega(false);
        }
    }

    const vistoriasEntrega = (expedicao.vistorias || []).filter(v => v.tipo === 'ENTREGA');
    const vistoriasDevolucao = (expedicao.vistorias || []).filter(v => v.tipo === 'DEVOLUCAO');

    // ------------------------------------------------------------------
    // Passo a passo do fluxo, na ordem em que acontece na vida real:
    //   1) Vistoria de Entrega (pré-saída)   → TÉCNICO DE MANUTENÇÃO, uma por unidade
    //   2) Check-out                         → CONFERENTE confere os itens e registra a saída
    //   3) Confirmação de Entrega (no local) → ENTREGADOR coleta nome, documento e
    //      assinatura desenhada de quem recebeu e fotografa a entrega. Data/hora
    //      sempre a do clique (o backend grava com LocalDateTime.now()).
    //   4) Check-in                          → CONFERENTE registra a volta
    // A partir do check-in, a unidade cai sozinha em "Aguardando Manutenção"
    // e é o TÉCNICO quem faz a revisão final (diagnóstico) na tela de Ordens
    // de Serviço — por isso esse fluxo aqui tem 4 passos, não 5: a etapa
    // do técnico acontece em outra tela, fora da expedição.
    // A Vistoria de Devolução continua existindo, mas como registro opcional
    // (não bloqueia nada), pra quem quiser documentar o estado na volta.
    //
    // IMPORTANTE: a ENTREGA só tem 3 passos (1 → 2 → 3). Ela termina na
    // confirmação de entrega no local (status ENTREGUE) porque o equipamento
    // fica com o cliente — não existe "check-in" pra ela. Quando chega a hora
    // de buscar o equipamento, cria-se uma expedição separada do tipo COLETA
    // (selecionando essa ENTREGA como origem), que aí sim segue os 4 passos
    // completos e termina em CONCLUIDO.
    // ------------------------------------------------------------------
    const ehColetaDetalhe = expedicao.tipo === 'COLETA';
    const isFinalizada = expedicao.status === 'CONCLUIDO' || expedicao.status === 'CANCELADO'
        || (!ehColetaDetalhe && expedicao.status === 'ENTREGUE');
    const podeRegistrarEntrega = expedicao.status === 'AGENDADO';

    // Vistoria por UNIDADE: cada patrimônio da entrega precisa da sua vistoria
    // e nenhuma pode estar reprovada (condição RUIM). A API confere o mesmo.
    const itensDaExpedicao = expedicao.itens || [];
    const vistoriaPorUnidade = {};
    vistoriasEntrega.forEach(v => { vistoriaPorUnidade[v.unidadeId] = v; });
    const rotuloItem = (i) => i.codigoPatrimonio || `#${i.unidadeId}`;
    const unidadesSemVistoria = itensDaExpedicao
        .filter(i => i.unidadeId && !vistoriaPorUnidade[i.unidadeId])
        .map(rotuloItem);
    const unidadesReprovadas = itensDaExpedicao
        .filter(i => i.unidadeId && vistoriaPorUnidade[i.unidadeId]?.condicaoGeral === 'RUIM')
        .map(rotuloItem);
    // COLETA não tem vistoria de saída — o equipamento ainda está com o cliente.
    const vistoriasOk = ehColetaDetalhe || (unidadesSemVistoria.length === 0 && unidadesReprovadas.length === 0);
    const conferenciaCompleta = itensDaExpedicao.length > 0
        && itensDaExpedicao.every(i => itensConferidos.includes(i.id));
    const podeFazerCheckout = expedicao.status === 'AGENDADO' && vistoriasOk;
    const entregaConfirmada = !!expedicao.entregaConfirmadaEm;
    const podeConfirmarEntrega = expedicao.status === 'EM_TRANSITO' && !entregaConfirmada;
    // Devolução: depois que a coleta saiu do depósito (em trânsito, confirmada no local ou concluída).
    const podeRegistrarDevolucao = ehColetaDetalhe
        && ['EM_TRANSITO', 'ENTREGUE', 'CONCLUIDO'].includes(expedicao.status);
    const podeFazerCheckin = ehColetaDetalhe && expedicao.status === 'ENTREGUE' && entregaConfirmada;

    // Numeração dos passos muda conforme o tipo: ENTREGA tem vistoria de
    // saída (o equipamento está no depósito, dá pra inspecionar antes de
    // sair); COLETA não — o primeiro passo dela já é o checkout.
    //   ENTREGA: 1 Vistoria de Saída → 2 Checkout → 3 Confirmação (fim, ENTREGUE)
    //   COLETA:  1 Checkout → 2 Confirmação → 3 Check-in (fim, CONCLUIDO)
    const stepCheckout = ehColetaDetalhe ? 1 : 2;
    const stepConfirmacao = ehColetaDetalhe ? 2 : 3;
    const stepCheckin = 3; // só existe pra COLETA

    const passoAtual = isFinalizada ? 4 :
        ehColetaDetalhe
            ? (expedicao.status === 'AGENDADO' ? 1 :
               expedicao.status === 'EM_TRANSITO' ? (entregaConfirmada ? 3 : 2) :
               expedicao.status === 'ENTREGUE' ? 3 : 4)
            : (expedicao.status === 'AGENDADO' ? (vistoriasOk ? 2 : 1) :
               expedicao.status === 'EM_TRANSITO' ? 3 : 4);

    return (
        <div className="modalBackdrop modalBackdropAdmin">
            <div className="modalCard detalheModalCard">
                <div className="modalHeader">
                    <h3>
                        <FontAwesomeIcon icon={faTruck} /> {expedicao.codigo}
                        <span className={`expeStatusTag ${expedicao.status?.toLowerCase()}`}>
                            {STATUS_EXPEDICAO_LABEL[expedicao.status] || expedicao.status}
                        </span>
                    </h3>
                    <button type="button" className="btn btn-error" onClick={onClose}><FontAwesomeIcon icon={faXmark} /></button>
                </div>

                {message && (
                    <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                )}

                {!isFinalizada && (
                    <p style={{ color: '#666', fontSize: '0.85rem', margin: '0 0 14px' }}>
                        Siga os passos <strong>1 → 2 → 3</strong> na ordem. Um botão só libera quando o passo anterior estiver feito.
                        {!ehColetaDetalhe && ' Essa entrega termina no passo 3 — a coleta do equipamento é feita depois, em outra expedição.'}
                    </p>
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
                        <strong>Endereço:</strong> {expedicao.enderecoEntrega?.formatado || '---'}
                    </div>
                    {ehColetaDetalhe && (
                        <div className="detalheInfoItem">
                            <strong>Entrega de origem:</strong> {expedicao.entregaOrigemCodigo || '---'}
                        </div>
                    )}
                    <div className="detalheInfoItem">
                        <strong>Observações:</strong> {expedicao.observacoes || '---'}
                    </div>
                    {expedicao.checkoutEm && (
                        <div className="detalheInfoItem">
                            <strong>Check-out:</strong> {formatDate(expedicao.checkoutEm)}
                        </div>
                    )}
                    {expedicao.entregaConfirmadaEm && (
                        <div className="detalheInfoItem">
                            <strong>Entrega confirmada:</strong> {formatDate(expedicao.entregaConfirmadaEm)}
                        </div>
                    )}
                    {expedicao.checkinEm && (
                        <div className="detalheInfoItem">
                            <strong>Check-in:</strong> {formatDate(expedicao.checkinEm)}
                        </div>
                    )}
                    {expedicao.motivoCancelamento && (
                        <div className="detalheInfoItem">
                            <strong>Motivo do cancelamento:</strong> {expedicao.motivoCancelamento}
                        </div>
                    )}
                </div>

                {/* ITENS (contexto, sem ação) */}
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

                {/* PASSO 1 (só ENTREGA) — VISTORIA DE SAÍDA. Não existe pra
                    COLETA: o equipamento ainda está com o cliente, não tem
                    o que inspecionar no depósito antes de sair pra buscar. */}
                {!ehColetaDetalhe && (
                <div className={`checkinSection stepSection ${podeRegistrarEntrega ? 'active' : ''}`}>
                    <h4>
                        <StepBadge n={1} passoAtual={passoAtual} isFinalizada={isFinalizada} /> Vistoria de Entrega ({vistoriasEntrega.length})
                        <span className="responsavelTag">Técnico de Manutenção</span>
                        <button
                            className="smallBtn success"
                            disabled={!podeRegistrarEntrega || !ehTecnico || unidadesSemVistoria.length === 0}
                            title={
                                !podeRegistrarEntrega ? 'Só é possível registrar antes do check-out, com a expedição Agendada.' :
                                unidadesSemVistoria.length === 0 ? 'Todas as unidades já foram vistoriadas.' :
                                !ehTecnico ? 'Somente o técnico de manutenção registra esta vistoria.' : ''
                            }
                            onClick={() => setVistoriaModalTipo('ENTREGA')}
                        >
                            <FontAwesomeIcon icon={faPen} /> Nova
                        </button>
                    </h4>
                    {podeRegistrarEntrega && unidadesSemVistoria.length > 0 && (
                        <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '0 0 8px' }}>
                            ⚠ Falta vistoriar: {unidadesSemVistoria.join(', ')} (cada unidade precisa da sua vistoria, com foto).
                        </p>
                    )}
                    {unidadesReprovadas.length > 0 && (
                        <div className="messageBanner negative">
                            Reprovada(s) na vistoria (condição RUIM): {unidadesReprovadas.join(', ')}. A saída está bloqueada:
                            cancele a expedição para encaminhar a unidade à manutenção e crie outra com um equipamento em bom estado.
                        </div>
                    )}
                    {vistoriasEntrega.length === 0 ? (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>
                            {podeRegistrarEntrega
                                ? 'O técnico de manutenção revisa o equipamento antes de sair do depósito.'
                                : 'Nenhuma vistoria registrada.'}
                        </p>
                    ) : (
                        <div className="vistoriaList">
                            {vistoriasEntrega.map(v => (
                                <VistoriaCard key={v.id} vistoria={v} />
                            ))}
                        </div>
                    )}
                </div>
                )}

                {/* PASSO — CHECK-OUT (CONFERENTE). Número muda conforme o
                    tipo (ver stepCheckout acima). */}
                <div className={`checkinSection stepSection ${expedicao.status === 'AGENDADO' ? 'active' : ''}`}>
                    <h4>
                        <StepBadge n={stepCheckout} passoAtual={passoAtual} isFinalizada={isFinalizada} /> Check-out (saída do depósito{ehColetaDetalhe ? ', indo buscar no cliente' : ''})
                        <span className="responsavelTag">Conferente</span>
                    </h4>
                    {expedicao.status === 'AGENDADO' && (
                        <>
                            <p style={{ color: '#666', fontSize: '0.85rem', margin: '0 0 6px' }}>
                                Confira cada item (patrimônio e equipamento) antes de liberar a saída:
                            </p>
                            <div className="conferenciaLista">
                                {itensDaExpedicao.map(item => (
                                    <label key={item.id} className="conferenciaItem">
                                        <input
                                            type="checkbox"
                                            checked={itensConferidos.includes(item.id)}
                                            disabled={!ehConferente || changingStatus}
                                            onChange={() => toggleItemConferido(item.id)}
                                        />
                                        <span>{item.codigoPatrimonio || '---'} — {item.equipamentoNome || '---'}</span>
                                    </label>
                                ))}
                            </div>
                            <button
                                className="smallBtn success"
                                disabled={changingStatus || !podeFazerCheckout || !conferenciaCompleta || !ehConferente}
                                title={
                                    !vistoriasOk ? 'Resolva as vistorias pendentes/reprovadas (passo 1) antes de sair.' :
                                    !conferenciaCompleta ? 'Marque todos os itens conferidos.' :
                                    !ehConferente ? 'Somente o conferente registra a saída.' : ''
                                }
                                onClick={() => handleStatusChange('EM_TRANSITO', { itensConferidos })}
                            >
                                <FontAwesomeIcon icon={faTruckLoading} /> Iniciar (Check-out)
                            </button>
                            {!vistoriasOk && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Bloqueado: {unidadesReprovadas.length > 0
                                        ? 'há unidade reprovada na vistoria (passo 1).'
                                        : 'faltam vistorias de entrega (passo 1).'}
                                </p>
                            )}
                            {vistoriasOk && !conferenciaCompleta && ehConferente && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Marque todos os itens conferidos para liberar a saída.
                                </p>
                            )}
                            {vistoriasOk && !ehConferente && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Aguarde até que um Conferente confirme sua saída.
                                </p>
                            )}
                        </>
                    )}
                    {expedicao.status !== 'AGENDADO' && (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>
                            {expedicao.checkoutEm ? `Check-out feito em ${formatDate(expedicao.checkoutEm)}.` : 'Ainda não chegou nesse passo.'}
                        </p>
                    )}
                </div>

                {/* PASSO 3 — CONFIRMAÇÃO DE ENTREGA NO LOCAL (ENTREGADOR).
                    Nome + documento de quem assina, assinatura desenhada e foto
                    (data/hora fica por conta do servidor). */}
                <div className={`checkinSection stepSection ${podeConfirmarEntrega ? 'active' : ''}`}>
                    <h4>
                        <StepBadge n={stepConfirmacao} passoAtual={passoAtual} isFinalizada={isFinalizada} /> {ehColetaDetalhe ? 'Confirmação de Coleta (no local do cliente)' : 'Confirmação de Entrega (no local do cliente)'}
                        <span className="responsavelTag">Entregador</span>
                    </h4>
                    {expedicao.status === 'EM_TRANSITO' && !entregaConfirmada && (
                        <>
                            <p style={{ color: '#999', fontSize: '0.85rem', margin: '0 0 10px' }}>
                                {ehColetaDetalhe
                                    ? 'O entregador confirma no local, com assinatura de quem entregou o equipamento de volta e uma foto do equipamento recolhido.'
                                    : 'O entregador confirma no local, com assinatura de quem recebeu e uma foto do equipamento entregue.'}
                                {' '}A data/hora é registrada automaticamente no momento do clique.
                            </p>

                            {!ehColetaDetalhe && (expedicao.nomesAutorizados || []).length > 0 ? (
                                <select
                                    className="equipInput"
                                    value={assinaturaEntrega}
                                    onChange={e => setAssinaturaEntrega(e.target.value)}
                                    disabled={!ehEntregador}
                                    style={{ marginBottom: '10px' }}
                                >
                                    <option value="">Quem está assinando? (pessoa autorizada)</option>
                                    {expedicao.nomesAutorizados.map((nome, idx) => (
                                        <option key={idx} value={nome}>{nome}</option>
                                    ))}
                                </select>
                            ) : (
                                <input
                                    className="equipInput"
                                    placeholder={ehColetaDetalhe ? 'Nome de quem devolveu' : 'Nome de quem recebeu'}
                                    value={assinaturaEntrega}
                                    onChange={e => setAssinaturaEntrega(e.target.value)}
                                    disabled={!ehEntregador}
                                    style={{ marginBottom: '10px' }}
                                />
                            )}

                            <input
                                className="equipInput"
                                placeholder="Documento de quem assina (CPF ou RG)"
                                value={documentoEntrega}
                                onChange={e => setDocumentoEntrega(e.target.value)}
                                disabled={!ehEntregador}
                                style={{ marginBottom: '10px' }}
                            />

                            <p style={{ margin: '0 0 4px', fontSize: '0.8rem' }}>
                                <FontAwesomeIcon icon={faSignature} /> <strong>Assinatura</strong> de quem {ehColetaDetalhe ? 'devolveu' : 'recebeu'}:
                            </p>
                            <AssinaturaPad
                                ref={assinaturaPadRef}
                                onChange={setAssinaturaDesenhada}
                                disabled={!ehEntregador}
                            />

                            <p style={{ margin: '10px 0 4px', fontSize: '0.8rem' }}>
                                <FontAwesomeIcon icon={faCamera} /> <strong>Foto</strong> tirada no local:
                            </p>
                            <div style={{ marginBottom: '10px' }}>
                                <FileDropzone
                                    compact
                                    accept="image/*"
                                    capture="environment"
                                    disabled={!ehEntregador}
                                    label={fotoEntregaFile ? fotoEntregaFile.name : 'Arraste a foto aqui'}
                                    hint={fotoEntregaFile ? 'clique ou arraste para trocar' : 'ou clique para tirar/selecionar'}
                                    onFiles={files => setFotoEntregaFile(files[0] || null)}
                                />
                            </div>

                            <textarea
                                className="equipTextarea"
                                rows={2}
                                placeholder="Observações do entregador (opcional)"
                                value={observacaoEntrega}
                                maxLength={1000}
                                onChange={e => setObservacaoEntrega(e.target.value)}
                                disabled={!ehEntregador}
                                style={{ marginBottom: '10px' }}
                            />

                            <div className="entregaAcoes">
                                <button
                                    className="smallBtn success"
                                    disabled={savingEntrega || changingStatus || !podeConfirmarEntrega || !ehEntregador || !assinaturaDesenhada}
                                    title={
                                        !ehEntregador ? 'Somente o entregador confirma a entrega no local.' :
                                        !assinaturaDesenhada ? 'Peça para a pessoa assinar no quadro.' : ''
                                    }
                                    onClick={handleConfirmarEntrega}
                                >
                                    <FontAwesomeIcon icon={faCamera} /> {savingEntrega ? 'Enviando...' : (ehColetaDetalhe ? 'Confirmar Coleta' : 'Confirmar Entrega')}
                                </button>
                                <button
                                    className="smallBtn delete"
                                    type="button"
                                    disabled={savingEntrega || changingStatus || !ehEntregador}
                                    onClick={handleNaoRealizada}
                                >
                                    <FontAwesomeIcon icon={faTimesCircle} /> {ehColetaDetalhe ? 'Não consegui coletar' : 'Não consegui entregar'}
                                </button>
                            </div>
                            {!ehEntregador && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Aguarde até que o Entregador confirme {ehColetaDetalhe ? 'a coleta' : 'a entrega'} no local.
                                </p>
                            )}
                        </>
                    )}
                    {expedicao.status !== 'EM_TRANSITO' && !entregaConfirmada && (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>Ainda não chegou nesse passo.</p>
                    )}
                    {entregaConfirmada && (
                        <div className="vistoriaList">
                            <p style={{ margin: '0 0 8px', fontSize: '0.85rem' }}>
                                {ehColetaDetalhe ? 'Coleta confirmada' : 'Entrega confirmada'} em {formatDate(expedicao.entregaConfirmadaEm)}.
                            </p>
                            {expedicao.assinaturaEntrega && (
                                <div className="assinaturaRegistrada" style={{ marginBottom: '8px' }}>
                                    <p style={{ margin: 0, fontSize: '0.8rem' }}><strong>Assinou:</strong></p>
                                    <p className="assinaturaText">{expedicao.assinaturaEntrega}</p>
                                    {expedicao.documentoRecebedor && (
                                        <p style={{ margin: '4px 0 0', fontSize: '0.8rem' }}>
                                            <strong>Documento:</strong> {expedicao.documentoRecebedor}
                                        </p>
                                    )}
                                </div>
                            )}
                            {expedicao.assinaturaEntregaImagem && (
                                <img
                                    src={imageUrl(expedicao.assinaturaEntregaImagem)}
                                    alt="Assinatura"
                                    className="assinaturaImagem"
                                    style={{ marginBottom: '8px' }}
                                />
                            )}
                            {expedicao.observacaoEntrega && (
                                <p style={{ margin: '0 0 8px', fontSize: '0.85rem' }}>
                                    <strong>Obs. do entregador:</strong> {expedicao.observacaoEntrega}
                                </p>
                            )}
                            {expedicao.fotoEntrega && (
                                <img
                                    src={imageUrl(expedicao.fotoEntrega)}
                                    alt="Foto da entrega"
                                    style={{ maxWidth: '220px', borderRadius: '8px', display: 'block' }}
                                />
                            )}
                        </div>
                    )}
                </div>

                {/* PASSO 3 — CHECK-IN (CONFERENTE) — só existe pra COLETA.
                    A ENTREGA termina no passo 3 (ENTREGUE); quem volta pro
                    depósito é a COLETA, então só ela tem check-in. */}
                {ehColetaDetalhe && (
                <div className={`checkinSection stepSection ${expedicao.status === 'ENTREGUE' ? 'active' : ''}`}>
                    <h4>
                        <StepBadge n={stepCheckin} passoAtual={passoAtual} isFinalizada={isFinalizada} /> Check-in (o conferente recebe de volta)
                        <span className="responsavelTag">Conferente</span>
                    </h4>
                    {expedicao.status === 'ENTREGUE' && (
                        <>
                            <button
                                className="smallBtn success"
                                disabled={changingStatus || !podeFazerCheckin || !ehConferente}
                                title={
                                    !entregaConfirmada ? 'Aguarde a confirmação de coleta no local (passo 2).' :
                                    !ehConferente ? 'Somente o conferente registra a entrada.' : ''
                                }
                                onClick={() => handleStatusChange('CONCLUIDO')}
                            >
                                <FontAwesomeIcon icon={faCheckCircle} /> Concluir (Check-in)
                            </button>
                            {!entregaConfirmada && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Bloqueado: aguarde o entregador confirmar a coleta no local (passo 2).
                                </p>
                            )}
                            {entregaConfirmada && !ehConferente && (
                                <p style={{ color: '#b45309', fontSize: '0.85rem', margin: '8px 0 0' }}>
                                    ⚠ Aguarde até que um Conferente confirme o recebimento.
                                </p>
                            )}
                            <p style={{ color: '#999', fontSize: '0.8rem', margin: '8px 0 0' }}>
                                Depois do check-in, o equipamento cai automaticamente em "Aguardando Manutenção" — é lá que o
                                <strong> técnico</strong> faz a revisão final e decide se precisa consertar algo (tela de Ordens de Serviço).
                            </p>
                        </>
                    )}
                    {expedicao.status !== 'ENTREGUE' && (
                        <p style={{ color: '#999', fontSize: '0.85rem' }}>
                            {expedicao.checkinEm ? `Check-in feito em ${formatDate(expedicao.checkinEm)}.` : 'Ainda não chegou nesse passo.'}
                        </p>
                    )}
                </div>
                )}

                {/* VISTORIA DE DEVOLUÇÃO — só faz sentido na COLETA (é quando o
                    equipamento realmente volta pro depósito). Registro opcional,
                    não bloqueia nada. A decisão de "precisa consertar?" acontece
                    na Ordem de Serviço, feita pelo técnico; isto aqui é só uma
                    foto/observação de apoio. */}
                {ehColetaDetalhe && (
                <div className="checkinSection">
                    <h4>
                        <FontAwesomeIcon icon={faClipboardCheck} /> Vistoria de Devolução ({vistoriasDevolucao.length})
                        <span className="responsavelTag optional">Técnico de Manutenção · Opcional</span>
                        <button
                            className="smallBtn success"
                            disabled={!podeRegistrarDevolucao || !ehTecnico}
                            title={
                                !podeRegistrarDevolucao ? 'Só é possível registrar depois que a coleta saiu do depósito (check-out).' :
                                !ehTecnico ? 'Somente o técnico de manutenção registra esta vistoria.' : ''
                            }
                            onClick={() => setVistoriaModalTipo('DEVOLUCAO')}
                        >
                            <FontAwesomeIcon icon={faPen} /> Nova
                        </button>
                    </h4>
                    <p style={{ color: '#999', fontSize: '0.8rem', marginBottom: vistoriasDevolucao.length ? '10px' : 0 }}>
                        Registro opcional de apoio (fotos/observações) do técnico de manutenção. Não é obrigatório
                        e não bloqueia o check-in — a decisão técnica é feita depois, na Ordem de Serviço.
                    </p>
                    {vistoriasDevolucao.length > 0 && (
                        <div className="vistoriaList">
                            {vistoriasDevolucao.map(v => (
                                <VistoriaCard key={v.id} vistoria={v} />
                            ))}
                        </div>
                    )}
                </div>
                )}

                {/* CANCELAR — com motivo. Conferente cancela só enquanto está Agendada;
                    depois do check-out (equipamento na estrada) só ADMIN/Gerente.
                    O entregador usa "não consegui entregar" no passo 3. */}
                {!isFinalizada && (ehAdminOuGerente || (user?.cargoFuncionario === 'CONFERENTE' && expedicao.status === 'AGENDADO')) && (
                    <div className="checkinSection">
                        <button
                            className="smallBtn delete"
                            disabled={changingStatus}
                            onClick={handleCancelar}
                        >
                            <FontAwesomeIcon icon={faTimesCircle} /> Cancelar Expedição
                        </button>
                        {!ehColetaDetalhe && (
                            <p style={{ color: '#999', fontSize: '0.8rem', margin: '8px 0 0' }}>
                                As unidades reservadas voltam para o estoque (as reprovadas na vistoria vão para a manutenção).
                            </p>
                        )}
                    </div>
                )}
            </div>

            {vistoriaModalTipo && (
                <VistoriaModal
                    expedicao={expedicao}
                    tipoInicial={vistoriaModalTipo}
                    onClose={() => setVistoriaModalTipo(null)}
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
    const location = useLocation();
    const navigate = useNavigate();
    const [expedicoes, setExpedicoes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [filtroData, setFiltroData] = useState('');
    const [filtroStatus, setFiltroStatus] = useState('');
    const [filtroTipo, setFiltroTipo] = useState('');
    const [showNova, setShowNova] = useState(false);
    const [pedidoOrigem, setPedidoOrigem] = useState(null);
    const [depositoOrigemId, setDepositoOrigemId] = useState(null);
    const [carregandoPedidoOrigem, setCarregandoPedidoOrigem] = useState(false);
    const [detalheId, setDetalheId] = useState(null);
    const [message, setMessage] = useState(null);

    // Excluir expedição é irreversível e mexe no histórico de locação —
    // restrito a quem gerencia a operação, espelhando o SecurityConfig
    // (DELETE /api/expedicoes/** só ADMIN/GERENTE_OPERACOES).
    const canManageExpedicao = user?.tipo === 'ADMIN' || user?.cargoFuncionario === 'GERENTE_OPERACOES';
    const canCreateExpedicao = user?.tipo === 'ADMIN' || user?.cargoFuncionario === 'GERENTE_OPERACOES' || user?.cargoFuncionario === 'CONFERENTE';

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

    // Chegou aqui pelo botão "Gerar expedição" na aba "Prontos p/ Expedição"
    // de Pedidos: busca o pedido e já abre o modal de Nova Expedição
    // pré-preenchido com os dados dele. Limpa o state da rota em seguida pra
    // um F5 na página não reabrir o modal sozinho.
    useEffect(() => {
        const pedidoOrigemId = location.state?.pedidoOrigemId;
        if (!pedidoOrigemId) return;
        setCarregandoPedidoOrigem(true);
        setDepositoOrigemId(location.state?.depositoOrigemId || null);
        api.get(`/api/pedidos/${pedidoOrigemId}`)
            .then(res => {
                setPedidoOrigem(res.data);
                setShowNova(true);
            })
            .catch(err => setMessage({ type: 'error', text: 'Não foi possível carregar o pedido: ' + (err.response?.data?.message || err.message) }))
            .finally(() => {
                setCarregandoPedidoOrigem(false);
                navigate(location.pathname, { replace: true, state: {} });
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.state?.pedidoOrigemId]);

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

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Painel de Expedição</h2>
                {canCreateExpedicao && (
                    <div className="headerRight">
                        <button className="addBtn" onClick={() => setShowNova(true)}>
                            <FontAwesomeIcon icon={faPlus} /> Nova Expedição
                        </button>
                    </div>
                )}
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
            )}

            {carregandoPedidoOrigem && (
                <div className="messageBanner">Carregando dados do pedido...</div>
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
                        <option value="ENTREGUE">Entregue</option>
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
                    <h3><FontAwesomeIcon icon={faList} /> {filtroData ? `Programação de ${formatDateOnly(filtroData)}` : 'Todas as expedições'}</h3>
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
                                        {canManageExpedicao && (
                                            <button className="actionBtn delete" onClick={() => handleDelete(e.id)} title="Excluir">
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        )}
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
                    pedidoOrigem={pedidoOrigem}
                    depositoOrigemId={depositoOrigemId}
                    onClose={() => { setShowNova(false); setPedidoOrigem(null); setDepositoOrigemId(null); }}
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