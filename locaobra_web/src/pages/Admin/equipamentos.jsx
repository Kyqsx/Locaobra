import React, { useEffect, useState, useMemo, useRef } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../../service/api';
import './Equipamento.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faSearch, faPlus, faTrash, faTools, faFileImport,
    faList, faImage, faBoxesStacked, faTag, faEdit,
} from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../../utils/useAuth';
import { canAccessAdminRoute } from '../../utils/permissions';

const STATUS_UNIDADE_LABEL = {
    DISPONIVEL: 'Disponível',
    ALUGADO: 'Alugado',
    EM_MANUTENCAO: 'Em manutenção',
};

function imageUrl(path) {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${api.defaults.baseURL}${path}`;
}

function especificacoesParaArray(especificacoes) {
    return especificacoes ? Object.entries(especificacoes).map(([chave, valor]) => ({ chave, valor })) : [];
}

function especificacoesParaBackend(lista) {
    return lista.reduce((acc, s) => {
        const chave = (s.chave || '').trim();
        const valor = (s.valor || '').trim();
        if (chave && valor) acc[chave] = valor;
        return acc;
    }, {});
}

/* ============================================================
   IMAGE PICKER — seletor + crop 1:1, totalmente autocontido.
   Cada instância tem seu próprio estado; usar `key` diferente
   no componente pai força um remount limpo (sem vazar arquivo
   de uma sessão de upload pra outra).
   ============================================================ */
function ImagePicker({ onChange }) {
    const [readyFiles, setReadyFiles] = useState([]);
    const [cropQueue, setCropQueue] = useState([]);
    const [currentCropIndex, setCurrentCropIndex] = useState(0);
    const [cropZoom, setCropZoom] = useState(1);
    const [cropBox, setCropBox] = useState({ left: 0, top: 0, size: 200 });
    const [isDragging, setIsDragging] = useState(false);

    const canvasRef = useRef(null);
    const containerRef = useRef(null);
    const currentImageRef = useRef(null);
    const dragStartRef = useRef(null);

    useEffect(() => {
        onChange(readyFiles);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [readyFiles]);

    useEffect(() => {
        const file = cropQueue[currentCropIndex];
        const canvas = canvasRef.current;
        if (!canvas || !file) return;
        const ctx = canvas.getContext('2d');
        const img = new Image();
        const reader = new FileReader();
        reader.onload = () => { img.src = reader.result; };
        reader.readAsDataURL(file);
        img.onload = () => {
            const w = canvas.width = 400;
            const h = canvas.height = 400;
            ctx.clearRect(0, 0, w, h);

            const scale = Math.max(w / img.width, h / img.height);
            const drawW = img.width * scale;
            const drawH = img.height * scale;
            const drawX = Math.round((w - drawW) / 2);
            const drawY = Math.round((h - drawH) / 2);
            ctx.drawImage(img, 0, 0, img.width, img.height, drawX, drawY, drawW, drawH);

            currentImageRef.current = { img, drawX, drawY, drawW, drawH };

            const baseSize = Math.min(w, h);
            const size = Math.max(40, Math.round(baseSize / cropZoom));
            const left = Math.round((w - size) / 2);
            const top = Math.round((h - size) / 2);
            setCropBox({ left, top, size });
        };
    }, [cropQueue, currentCropIndex, cropZoom]);

    function handleFileInput(e) {
        const files = Array.from(e.target.files || []);
        if (files.length === 0) return;
        setCropQueue(files);
        setCurrentCropIndex(0);
        setCropZoom(1);
        e.target.value = ''; // permite selecionar o mesmo arquivo de novo depois
    }

    function onMouseDown(e) {
        if (!containerRef.current) return;
        e.preventDefault();
        setIsDragging(true);
        const rect = containerRef.current.getBoundingClientRect();
        dragStartRef.current = { x: e.clientX, y: e.clientY, rect, box: { ...cropBox } };
        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);
    }

    function onMouseMove(e) {
        const start = dragStartRef.current;
        if (!start) return;
        const dx = e.clientX - start.x;
        const dy = e.clientY - start.y;
        const newLeft = Math.min(Math.max(0, start.box.left + dx), start.rect.width - start.box.size);
        const newTop = Math.min(Math.max(0, start.box.top + dy), start.rect.height - start.box.size);
        setCropBox(prev => ({ ...prev, left: newLeft, top: newTop }));
    }

    function onMouseUp() {
        setIsDragging(false);
        dragStartRef.current = null;
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
    }

    async function applyCrop() {
        const file = cropQueue[currentCropIndex];
        const ref = currentImageRef.current;
        if (!file || !ref) return;
        const { img, drawX, drawY, drawW, drawH } = ref;
        const outSize = 800;
        const canvas = document.createElement('canvas');
        canvas.width = outSize;
        canvas.height = outSize;
        const ctx = canvas.getContext('2d');

        const { left, top, size } = cropBox;
        const relX = (left - drawX) / drawW;
        const relY = (top - drawY) / drawH;
        const relSize = size / drawW;

        const sx = Math.max(0, Math.round(relX * img.width));
        const sy = Math.max(0, Math.round(relY * img.height));
        const sSize = Math.max(1, Math.round(relSize * img.width));

        ctx.drawImage(img, sx, sy, sSize, sSize, 0, 0, outSize, outSize);

        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
        const croppedFile = new File([blob], file.name, { type: 'image/jpeg' });
        setReadyFiles(prev => [...prev, croppedFile]);
        advanceQueue();
    }

    function skipCrop() {
        advanceQueue();
    }

    function advanceQueue() {
        if (currentCropIndex + 1 >= cropQueue.length) {
            setCropQueue([]);
            setCurrentCropIndex(0);
        } else {
            setCurrentCropIndex(idx => idx + 1);
        }
    }

    function removeReady(idx) {
        setReadyFiles(prev => prev.filter((_, i) => i !== idx));
    }

    return (
        <div className="imagePickerWrapper">
            <div className="fileInputWrapper">
                <FontAwesomeIcon icon={faFileImport} />
                <input type="file" multiple onChange={handleFileInput} accept="image/*" />
            </div>

            {readyFiles.length > 0 && (
                <div className="selectedFilesList">
                    {readyFiles.map((file, i) => (
                        <span key={i} className="fileChip">
                            {file.name}
                            <button type="button" onClick={() => removeReady(i)} title="Remover">✕</button>
                        </span>
                    ))}
                </div>
            )}

            {cropQueue.length > 0 && currentCropIndex < cropQueue.length && (
                <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
                    <div className="modalCard" style={{ background: '#fff', padding: '18px', borderRadius: '8px', width: '760px', maxWidth: '95%' }}>
                        <h4>Crop 1:1 — arraste a caixa, ajuste o zoom e clique em "Aplicar"</h4>
                        <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
                            <div ref={containerRef} style={{ position: 'relative', width: '400px', height: '400px', border: '1px solid #ccc', background: '#222' }}>
                                <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />
                                <div
                                    onMouseDown={onMouseDown}
                                    style={{ position: 'absolute', left: cropBox.left, top: cropBox.top, width: cropBox.size, height: cropBox.size, border: '2px dashed #fff', boxSizing: 'border-box', cursor: 'move', background: 'rgba(255,255,255,0.06)' }}
                                />
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', minWidth: '200px' }}>
                                <label>Zoom</label>
                                <input type="range" min="1" max="3" step="0.01" value={cropZoom} onChange={e => setCropZoom(parseFloat(e.target.value))} />
                                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                                    <button type="button" className="smallBtn success" onClick={applyCrop}>Aplicar</button>
                                    <button type="button" className="smallBtn" onClick={skipCrop}>Pular</button>
                                    <button type="button" className="smallBtn delete" onClick={() => { setCropQueue([]); setCurrentCropIndex(0); }}>Fechar</button>
                                </div>
                                <div style={{ marginTop: '8px', color: '#666', fontSize: '0.9rem' }}>
                                    Imagem {currentCropIndex + 1} de {cropQueue.length}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

/* ============================================================
   MODAL DE CRIAÇÃO — fluxo dedicado para criar um novo modelo.
   ============================================================ */
function EquipamentoCreateModal({ onClose, onCreated }) {
    const [form, setForm] = useState({ nome: '', categoria: '', descricao: '', valorDiaria: '', especificacoes: [] });
    const [message, setMessage] = useState(null);
    const [novasImagens, setNovasImagens] = useState([]);
    const [imagePickerKey, setImagePickerKey] = useState(0);
    const [criando, setCriando] = useState(false);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function addSpecField() {
        setForm(prev => ({ ...prev, especificacoes: [...prev.especificacoes, { chave: '', valor: '' }] }));
    }

    function updateSpecAt(i, field, value) {
        setForm(prev => {
            const arr = [...prev.especificacoes];
            arr[i][field] = value;
            return { ...prev, especificacoes: arr };
        });
    }

    function removeSpecAt(i) {
        setForm(prev => ({ ...prev, especificacoes: prev.especificacoes.filter((_, idx) => idx !== i) }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setCriando(true);
        setMessage({ type: 'info', text: 'Enviando...' });

        const data = {
            nome: form.nome,
            descricao: form.descricao,
            categoria: form.categoria,
            valorDiaria: parseFloat(form.valorDiaria) || 0,
            especificacoes: especificacoesParaBackend(form.especificacoes),
        };

        const formData = new FormData();
        formData.append('equipamento', JSON.stringify(data));
        novasImagens.forEach(file => formData.append('imagens', file));

        api.post('/api/equipamentos', formData)
            .then(() => {
                setMessage({ type: 'success', text: 'Modelo cadastrado com sucesso!' });
                setForm({ nome: '', categoria: '', descricao: '', valorDiaria: '', especificacoes: [] });
                setNovasImagens([]);
                setImagePickerKey(k => k + 1);
                onCreated();
                onClose();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao criar: ' + (err.response?.data || err.message) }))
            .finally(() => setCriando(false));
    }

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>Novo modelo de equipamento</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                <form onSubmit={handleSubmit} className="equipForm">
                    {message && (
                        <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>{message.text}</div>
                    )}

                    <div className="formGrid">
                        <input className="equipInput" name="nome" placeholder="Nome do Equipamento" value={form.nome} onChange={handleChange} required />
                        <input className="equipInput" name="categoria" placeholder="Categoria" value={form.categoria} onChange={handleChange} required />
                        <input className="equipInput" name="valorDiaria" placeholder="Diária (0.00)" value={form.valorDiaria} onChange={handleChange} required />
                    </div>

                    <textarea className="equipTextarea" name="descricao" placeholder="Descrição / Especificações técnicas" value={form.descricao} onChange={handleChange} rows={2} />

                    <div className="specsContainer">
                        <div className="specsHeader">
                            <label><FontAwesomeIcon icon={faTools} /> Atributos</label>
                            <button type="button" onClick={addSpecField} className="smallBtn success">+ Adicionar</button>
                        </div>
                        {form.especificacoes.map((s, i) => (
                            <div key={i} className="specRow">
                                <input className="equipInput" placeholder="Chave" value={s.chave} onChange={e => updateSpecAt(i, 'chave', e.target.value)} />
                                <input className="equipInput" placeholder="Valor" value={s.valor} onChange={e => updateSpecAt(i, 'valor', e.target.value)} />
                                <button type="button" onClick={() => removeSpecAt(i)} className="actionBtn delete height100"><FontAwesomeIcon icon={faTrash} /></button>
                            </div>
                        ))}
                    </div>

                    <div className="formFooter">
                        <ImagePicker key={imagePickerKey} onChange={setNovasImagens} />
                        <button type="submit" className="addBtn" disabled={criando}>
                            {criando ? 'Salvando...' : 'Salvar Modelo'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ============================================================
   MODAL DE EDIÇÃO — um por equipamento, com estado 100% isolado.
   Busca os dados sozinho ao abrir; ao fechar, tudo é descartado.
   ============================================================ */
function EquipamentoEditModal({ equipamentoId, onClose, onChanged }) {
    const [loading, setLoading] = useState(true);
    const [eq, setEq] = useState(null);
    const [activeTab, setActiveTab] = useState('dados');

    const [form, setForm] = useState({ nome: '', categoria: '', descricao: '', valorDiaria: '', especificacoes: [] });
    const [dadosMessage, setDadosMessage] = useState(null);
    const [salvandoDados, setSalvandoDados] = useState(false);

    const [novasImagens, setNovasImagens] = useState([]);
    const [imagePickerKey, setImagePickerKey] = useState(0);
    const [imagensMessage, setImagensMessage] = useState(null);
    const [enviandoImagens, setEnviandoImagens] = useState(false);

    const [unidadeForm, setUnidadeForm] = useState({ codigoPatrimonio: '', numeroDeSerie: '', status: 'DISPONIVEL' });
    const [unidadeEditId, setUnidadeEditId] = useState(null);
    const [unidadeMessage, setUnidadeMessage] = useState(null);

    useEffect(() => {
        carregar();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [equipamentoId]);

    function carregar() {
        setLoading(true);
        api.get(`/api/equipamentos/${equipamentoId}`)
            .then(response => {
                const data = response.data;
                setEq(data);
                setForm({
                    nome: data.nome || '',
                    categoria: data.categoria || '',
                    descricao: data.descricao || '',
                    valorDiaria: data.valorDiaria?.toString() || '',
                    especificacoes: especificacoesParaArray(data.especificacoes),
                });
            })
            .catch(err => setDadosMessage({ type: 'error', text: 'Erro ao carregar: ' + (err.response?.data || err.message) }))
            .finally(() => setLoading(false));
    }

    function handleFormChange(e) {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    }

    function addSpecField() {
        setForm(prev => ({ ...prev, especificacoes: [...prev.especificacoes, { chave: '', valor: '' }] }));
    }

    function updateSpecAt(i, field, value) {
        setForm(prev => {
            const arr = [...prev.especificacoes];
            arr[i][field] = value;
            return { ...prev, especificacoes: arr };
        });
    }

    function removeSpecAt(i) {
        setForm(prev => ({ ...prev, especificacoes: prev.especificacoes.filter((_, idx) => idx !== i) }));
    }

    // Salva nome/categoria/valor/descrição/especificações — nunca mexe em imagens
    function handleSalvarDados(e) {
        e.preventDefault();
        setSalvandoDados(true);
        setDadosMessage({ type: 'info', text: 'Salvando...' });

        const data = {
            nome: form.nome,
            descricao: form.descricao,
            categoria: form.categoria,
            valorDiaria: parseFloat(form.valorDiaria) || 0,
            especificacoes: especificacoesParaBackend(form.especificacoes),
        };
        const formData = new FormData();
        formData.append('equipamento', JSON.stringify(data));

        api.put(`/api/equipamentos/${equipamentoId}`, formData)
            .then(() => {
                setDadosMessage({ type: 'success', text: 'Dados atualizados!' });
                carregar();
                onChanged();
            })
            .catch(err => setDadosMessage({ type: 'error', text: 'Erro ao salvar: ' + (err.response?.data || err.message) }))
            .finally(() => setSalvandoDados(false));
    }

    // Envia só as imagens novas — reaproveita os dados atuais do modelo
    function handleEnviarImagens() {
        if (novasImagens.length === 0) return;
        setEnviandoImagens(true);
        setImagensMessage({ type: 'info', text: 'Enviando...' });

        const data = {
            nome: form.nome,
            descricao: form.descricao,
            categoria: form.categoria,
            valorDiaria: parseFloat(form.valorDiaria) || 0,
            especificacoes: especificacoesParaBackend(form.especificacoes),
        };
        const formData = new FormData();
        formData.append('equipamento', JSON.stringify(data));
        novasImagens.forEach(file => formData.append('imagens', file));

        api.put(`/api/equipamentos/${equipamentoId}`, formData)
            .then(() => {
                setImagensMessage({ type: 'success', text: 'Imagens adicionadas!' });
                setNovasImagens([]);
                setImagePickerKey(k => k + 1); // remonta o ImagePicker limpo
                carregar();
                onChanged();
            })
            .catch(err => setImagensMessage({ type: 'error', text: 'Erro ao enviar: ' + (err.response?.data || err.message) }))
            .finally(() => setEnviandoImagens(false));
    }

    function handleDeleteImage(url) {
        if (!window.confirm('Remover esta imagem?')) return;
        api.delete(`/api/equipamentos/${equipamentoId}/imagens`, { params: { url } })
            .then(() => { carregar(); onChanged(); })
            .catch(err => setImagensMessage({ type: 'error', text: 'Erro ao remover: ' + (err.response?.data || err.message) }));
    }

    function moveImage(idx, direction) {
        if (!eq?.imagens) return;
        const arr = [...eq.imagens];
        const newIndex = idx + direction;
        if (newIndex < 0 || newIndex >= arr.length) return;
        [arr[idx], arr[newIndex]] = [arr[newIndex], arr[idx]];
        setEq(prev => ({ ...prev, imagens: arr })); // otimista
        api.post(`/api/equipamentos/${equipamentoId}/imagens/reorder`, arr)
            .catch(err => {
                setImagensMessage({ type: 'error', text: 'Erro ao reordenar: ' + (err.response?.data || err.message) });
                carregar(); // desfaz a alteração otimista em caso de erro
            });
    }

    function handleUnidadeChange(e) {
        const { name, value } = e.target;
        setUnidadeForm(prev => ({ ...prev, [name]: value }));
    }

    function handleUnidadeSubmit(e) {
        e.preventDefault();
        setUnidadeMessage({ type: 'info', text: unidadeEditId ? 'Atualizando...' : 'Adicionando...' });

        const data = {
            codigoPatrimonio: unidadeForm.codigoPatrimonio || null,
            numeroDeSerie: unidadeForm.numeroDeSerie || null,
            status: unidadeForm.status,
        };

        const request = unidadeEditId
            ? api.put(`/api/unidades/${unidadeEditId}`, data)
            : api.post(`/api/equipamentos/${equipamentoId}/unidades`, data);

        request
            .then(() => {
                setUnidadeMessage({ type: 'success', text: unidadeEditId ? 'Unidade atualizada!' : 'Unidade adicionada!' });
                setUnidadeForm({ codigoPatrimonio: '', numeroDeSerie: '', status: 'DISPONIVEL' });
                setUnidadeEditId(null);
                carregar();
                onChanged();
            })
            .catch(err => setUnidadeMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    function handleUnidadeStatusChange(unidadeId, status) {
        api.patch(`/api/unidades/${unidadeId}/status`, { status })
            .then(() => { carregar(); onChanged(); })
            .catch(err => setUnidadeMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    function handleEditUnidade(u) {
        setUnidadeForm({ codigoPatrimonio: u.codigoPatrimonio || '', numeroDeSerie: u.numeroDeSerie || '', status: u.status || 'DISPONIVEL' });
        setUnidadeEditId(u.id);
        setUnidadeMessage(null);
    }

    function handleCancelUnidadeEdit() {
        setUnidadeEditId(null);
        setUnidadeForm({ codigoPatrimonio: '', numeroDeSerie: '', status: 'DISPONIVEL' });
    }

    function handleDeleteUnidade(unidadeId) {
        if (!window.confirm('Remover esta unidade do patrimônio?')) return;
        api.delete(`/api/unidades/${unidadeId}`)
            .then(() => { carregar(); onChanged(); })
            .catch(err => setUnidadeMessage({ type: 'error', text: 'Erro: ' + (err.response?.data || err.message) }));
    }

    return (
        <div className="modalBackdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'flex-start', justifyContent: 'center', zIndex: 2000, overflowY: 'auto', padding: '30px 15px' }}>
            <div className="modalCard equipModalCard">
                <div className="modalHeader">
                    <h3>{eq ? eq.nome : 'Carregando...'}</h3>
                    <button type="button" className="closeBtn" onClick={onClose}>✕ Fechar</button>
                </div>

                {loading ? (
                    <div style={{ padding: '40px', textAlign: 'center', color: '#999' }}>Carregando...</div>
                ) : (
                    <>
                        <div className="modalTabs">
                            <button type="button" className={activeTab === 'dados' ? 'modalTabBtn active' : 'modalTabBtn'} onClick={() => setActiveTab('dados')}>
                                <FontAwesomeIcon icon={faTools} /> Dados
                            </button>
                            <button type="button" className={activeTab === 'imagens' ? 'modalTabBtn active' : 'modalTabBtn'} onClick={() => setActiveTab('imagens')}>
                                <FontAwesomeIcon icon={faImage} /> Imagens ({eq?.imagens?.length ?? 0})
                            </button>
                            <button type="button" className={activeTab === 'unidades' ? 'modalTabBtn active' : 'modalTabBtn'} onClick={() => setActiveTab('unidades')}>
                                <FontAwesomeIcon icon={faBoxesStacked} /> Unidades ({eq?.quantidadeTotal ?? 0})
                            </button>
                        </div>

                        {/* ===== ABA DADOS ===== */}
                        {activeTab === 'dados' && (
                            <form onSubmit={handleSalvarDados} className="equipForm">
                                {dadosMessage && (
                                    <div className={`messageBanner ${dadosMessage.type === 'error' ? 'negative' : 'positive'}`}>{dadosMessage.text}</div>
                                )}
                                <div className="formGrid">
                                    <input className="equipInput" name="nome" placeholder="Nome do Equipamento" value={form.nome} onChange={handleFormChange} required />
                                    <input className="equipInput" name="categoria" placeholder="Categoria" value={form.categoria} onChange={handleFormChange} required />
                                    <input className="equipInput" name="valorDiaria" placeholder="Diária (0.00)" value={form.valorDiaria} onChange={handleFormChange} required />
                                </div>
                                <textarea className="equipTextarea" name="descricao" placeholder="Descrição / Especificações técnicas" value={form.descricao} onChange={handleFormChange} rows={2} />

                                <div className="specsContainer">
                                    <div className="specsHeader">
                                        <label><FontAwesomeIcon icon={faTools} /> Atributos</label>
                                        <button type="button" onClick={addSpecField} className="smallBtn success">+ Adicionar</button>
                                    </div>
                                    {form.especificacoes.map((s, i) => (
                                        <div key={i} className="specRow">
                                            <input className="equipInput" placeholder="Chave" value={s.chave} onChange={e => updateSpecAt(i, 'chave', e.target.value)} />
                                            <input className="equipInput" placeholder="Valor" value={s.valor} onChange={e => updateSpecAt(i, 'valor', e.target.value)} />
                                            <button type="button" onClick={() => removeSpecAt(i)} className="actionBtn delete height100"><FontAwesomeIcon icon={faTrash} /></button>
                                        </div>
                                    ))}
                                    {form.especificacoes.length === 0 && <p style={{ color: '#999', fontSize: '0.85rem' }}>Nenhum atributo ainda.</p>}
                                </div>

                                <button type="submit" className="addBtn" disabled={salvandoDados}>
                                    {salvandoDados ? 'Salvando...' : 'Salvar Dados'}
                                </button>
                            </form>
                        )}

                        {/* ===== ABA IMAGENS ===== */}
                        {activeTab === 'imagens' && (
                            <div>
                                {imagensMessage && (
                                    <div className={`messageBanner ${imagensMessage.type === 'error' ? 'negative' : 'positive'}`}>{imagensMessage.text}</div>
                                )}

                                {eq?.imagens && eq.imagens.length > 0 ? (
                                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: '15px', marginBottom: '20px' }}>
                                        {eq.imagens.map((img, idx) => (
                                            <div key={img} style={{ textAlign: 'center', position: 'relative' }}>
                                                <img
                                                    src={imageUrl(img)}
                                                    alt={`${eq.nome} - ${idx + 1}`}
                                                    style={{ width: '100%', height: '120px', objectFit: 'cover', borderRadius: '8px', border: '1px solid #ddd' }}
                                                />
                                                <div style={{ position: 'absolute', top: '6px', right: '6px', display: 'flex', gap: '4px' }}>
                                                    <button type="button" className="smallBtn" onClick={() => moveImage(idx, -1)} title="Mover para trás" disabled={idx === 0}>↑</button>
                                                    <button type="button" className="smallBtn" onClick={() => moveImage(idx, 1)} title="Mover para frente" disabled={idx === eq.imagens.length - 1}>↓</button>
                                                    <button type="button" className="smallBtn delete" onClick={() => handleDeleteImage(img)} title="Remover">✕</button>
                                                </div>
                                                <p style={{ marginTop: '6px', fontSize: '11px', color: '#666' }}>#{idx + 1}</p>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p style={{ color: '#999', marginBottom: '20px' }}>Nenhuma imagem cadastrada ainda.</p>
                                )}

                                <div className="settingsCard" style={{ padding: '15px' }}>
                                    <strong>Adicionar novas imagens</strong>
                                    <ImagePicker key={imagePickerKey} onChange={setNovasImagens} />
                                    <button
                                        type="button"
                                        className="addBtn"
                                        disabled={novasImagens.length === 0 || enviandoImagens}
                                        onClick={handleEnviarImagens}
                                        style={{ marginTop: '10px' }}
                                    >
                                        {enviandoImagens ? 'Enviando...' : `Enviar ${novasImagens.length || ''} imagem(ns)`}
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* ===== ABA UNIDADES ===== */}
                        {activeTab === 'unidades' && (
                            <div>
                                {unidadeMessage && (
                                    <div className={`messageBanner ${unidadeMessage.type === 'error' ? 'negative' : 'positive'}`}>{unidadeMessage.text}</div>
                                )}

                                <form onSubmit={handleUnidadeSubmit} className="unidadeFormRow">
                                    <input className="equipInput" name="codigoPatrimonio" placeholder="Código de Patrimônio (ex: MAR-001)" value={unidadeForm.codigoPatrimonio} onChange={handleUnidadeChange} />
                                    <input className="equipInput" name="numeroDeSerie" placeholder="Nº de Série do Fabricante" value={unidadeForm.numeroDeSerie} onChange={handleUnidadeChange} />
                                    <select className="unidadeStatusSelect" name="status" value={unidadeForm.status} onChange={handleUnidadeChange}>
                                        <option value="DISPONIVEL">Disponível</option>
                                        <option value="EM_MANUTENCAO">Em manutenção</option>
                                        <option value="ALUGADO">Alugado</option>
                                    </select>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <button type="submit" className="addBtn">
                                            {unidadeEditId ? 'Salvar' : <><FontAwesomeIcon icon={faPlus} /> Adicionar</>}
                                        </button>
                                        {unidadeEditId && (
                                            <button type="button" className="smallBtn delete" onClick={handleCancelUnidadeEdit}>Cancelar</button>
                                        )}
                                    </div>
                                </form>

                                <div className="tableWrapper">
                                    <table className="usersTable">
                                        <thead>
                                            <tr>
                                                <th>Patrimônio</th>
                                                <th>Nº de Série</th>
                                                <th>Status</th>
                                                <th>Ações</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {eq?.unidades && eq.unidades.length > 0 ? (
                                                eq.unidades.map(u => (
                                                    <tr key={u.id} className="tableRow">
                                                        <td><FontAwesomeIcon icon={faTag} /> {u.codigoPatrimonio || '---'}</td>
                                                        <td className="dateCell">{u.numeroDeSerie || '---'}</td>
                                                        <td>
                                                            <select className="unidadeStatusSelect" value={u.status} onChange={(e) => handleUnidadeStatusChange(u.id, e.target.value)}>
                                                                {Object.entries(STATUS_UNIDADE_LABEL).map(([value, label]) => (
                                                                    <option key={value} value={value}>{label}</option>
                                                                ))}
                                                            </select>
                                                        </td>
                                                        <td className="actionsCell">
                                                            <button className="actionBtn edit" onClick={() => handleEditUnidade(u)} title="Editar"><FontAwesomeIcon icon={faEdit} /></button>
                                                            <button className="actionBtn delete" onClick={() => handleDeleteUnidade(u.id)} title="Remover"><FontAwesomeIcon icon={faTrash} /></button>
                                                        </td>
                                                    </tr>
                                                ))
                                            ) : (
                                                <tr className="tableRow">
                                                    <td colSpan="4" style={{ textAlign: 'center', color: '#999' }}>Nenhuma unidade cadastrada ainda</td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

/* ============================================================
   PÁGINA PRINCIPAL
   ============================================================ */
export default function Equipamento() {
    const { user } = useAuth();
    const [equipamentos, setEquipamentos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [editingId, setEditingId] = useState(null);
    const [createModalOpen, setCreateModalOpen] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        fetchList();
    }, []);

    function fetchList() {
        setLoading(true);
        api.get('/api/equipamentos')
            .then(response => setEquipamentos(response.data))
            .catch(err => setMessage({ type: 'error', text: 'Erro ao buscar: ' + (err.response?.data || err.message) }))
            .finally(() => setLoading(false));
    }

    function handleDelete(id) {
        if (!window.confirm('Tem certeza que deseja excluir este equipamento?')) return;
        api.delete(`/api/equipamentos/${id}`)
            .then(() => {
                setMessage({ type: 'success', text: 'Equipamento excluído com sucesso!' });
                if (editingId === id) setEditingId(null);
                fetchList();
            })
            .catch(err => setMessage({ type: 'error', text: 'Erro ao excluir: ' + (err.response?.data || err.message) }));
    }

    if (!canAccessAdminRoute(user, '/admin/equipamentos')) {
        return <Navigate to="/admin" replace />;
    }

    const filteredEquipamentos = useMemo(() => {
        return equipamentos.filter(eq =>
            eq.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
            eq.categoria.toLowerCase().includes(searchTerm.toLowerCase())
        );
    }, [equipamentos, searchTerm]);

    return (
        <div className="adminContent">
            <div className="viewHeader">
                <h2 className="pageTitle">Gestão de Equipamentos</h2>
                <div className="headerRight"></div>
            </div>

            {message && (
                <div className={`messageBanner ${message.type === 'error' ? 'negative' : 'positive'}`}>
                    {message.text}
                </div>
            )}

            <div className="settingsCard">
                <h3><FontAwesomeIcon icon={faPlus} /> Cadastrar Novo Modelo</h3>
                <p style={{ color: '#666', marginBottom: '12px' }}>Crie o modelo em uma janela dedicada, com upload de imagens e edição completa.</p>
                <div className="formFooter">
                    <button type="button" className="addBtn" onClick={() => setCreateModalOpen(true)}>
                        Novo Modelo
                    </button>
                </div>
            </div>

            <div className="recentUsersSection">
                <div className="sectionHeader">
                    <h3><FontAwesomeIcon icon={faList} /> Planilha de Cadastrados</h3>
                    <div className="headerRight">
                        {/*<button type="button" className="addBtn" onClick={() => setCreateModalOpen(true)}>
                            Novo Modelo
                        </button>*/}
                        <div className="searchBox">
                            <FontAwesomeIcon icon={faSearch} className="searchIcon" />
                            <input
                                className="searchInput"
                                placeholder="Pesquisar equipamento..."
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
                                <th>Categoria</th>
                                <th>Diária</th>
                                <th>Disponível / Total</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredEquipamentos.map(eq => (
                                <tr key={eq.id} className="tableRow">
                                    <td className="nameCell">
                                        <div className="userCell">
                                            <div className="userCellAvatar">{eq.nome.charAt(0)}</div>
                                            <div>
                                                <div className="userName">{eq.nome}</div>
                                                <div className="userRole">{eq.descricao?.substring(0, 25)}...</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td><span className="typeTag fornercedor">{eq.categoria}</span></td>
                                    <td className="userName">R$ {eq.valorDiaria?.toFixed(2)}</td>
                                    <td>
                                        <span className="quantidadeResumo">
                                            <strong>{eq.quantidadeDisponivel ?? 0}</strong> / {eq.quantidadeTotal ?? 0}
                                        </span>
                                    </td>
                                    <td className="actionsCell">
                                        <button className="actionBtn edit" onClick={() => setEditingId(eq.id)} title="Editar equipamento">
                                            <FontAwesomeIcon icon={faEdit} />
                                        </button>
                                        <button className="actionBtn delete" onClick={() => handleDelete(eq.id)} title="Excluir">
                                            <FontAwesomeIcon icon={faTrash} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!loading && filteredEquipamentos.length === 0 && (
                                <tr className="tableRow">
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#999' }}>Nenhum equipamento encontrado</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {createModalOpen && (
                <EquipamentoCreateModal
                    onClose={() => setCreateModalOpen(false)}
                    onCreated={fetchList}
                />
            )}

            {editingId && (
                <EquipamentoEditModal
                    key={editingId}
                    equipamentoId={editingId}
                    onClose={() => setEditingId(null)}
                    onChanged={fetchList}
                />
            )}
        </div>
    );
}
