import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMapMarkerAlt, faPlus, faEdit, faTrash, faStar } from '@fortawesome/free-solid-svg-icons';
import './meusEnderecos.css';

const enderecoVazio = () => ({
  apelido: '',
  cep: '',
  rua: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  estado: '',
});

// Formata em uma linha única — espelha o `formatado` que o backend já manda no
// EnderecoResponse (usado só como fallback pros dados ainda não persistidos).
function formatar(a) {
  if (!a) return '';
  const partes = [];
  const rua = (a.rua || '').trim();
  const numero = (a.numero || '').trim();
  const complemento = (a.complemento || '').trim();
  const bairro = (a.bairro || '').trim();
  const cidade = (a.cidade || '').trim();
  const estado = (a.estado || '').trim();
  const cep = (a.cep || '').trim();
  if (rua) partes.push(numero ? `${rua}, ${numero}` : rua);
  if (complemento) partes.push(complemento);
  if (bairro) partes.push(bairro);
  if (cidade) partes.push(estado ? `${cidade}/${estado}` : cidade);
  if (cep) partes.push(`CEP ${cep}`);
  return partes.join(' - ');
}

function MeusEnderecos() {
  const [enderecos, setEnderecos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  // Modal de cadastro/edição
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(enderecoVazio());
  const [saving, setSaving] = useState(false);

  // Botão em progresso por id (marcar principal / excluir)
  const [processandoId, setProcessandoId] = useState(null);

  const carregar = () => {
    setLoading(true);
    setMessage(null);
    api.get('/api/clientes/meus-enderecos')
      .then((res) => setEnderecos(res.data || []))
      .catch(() => setMessage({ type: 'error', text: 'Não foi possível carregar seus endereços. Tente novamente.' }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    carregar();
  }, []);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  }

  function openNovo() {
    setForm(enderecoVazio());
    setEditingId(null);
    setModalOpen(true);
  }

  function openEditar(end) {
    setForm({
      apelido: end.apelido || '',
      cep: end.cep || '',
      rua: end.rua || '',
      numero: end.numero || '',
      complemento: end.complemento || '',
      bairro: end.bairro || '',
      cidade: end.cidade || '',
      estado: end.estado || '',
    });
    setEditingId(end.id);
    setModalOpen(true);
  }

  function fecharModal() {
    setModalOpen(false);
    setEditingId(null);
    setForm(enderecoVazio());
  }
function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setMessage(null);

    const payload = {
      apelido: form.apelido.trim() || null,
      cep: form.cep.trim() || null,
      rua: form.rua.trim() || null,
      numero: form.numero.trim() || null,
      complemento: form.complemento.trim() || null,
      bairro: form.bairro.trim() || null,
      cidade: form.cidade.trim() || null,
      estado: form.estado.trim() || null,
    };

    const request = editingId
      ? api.put(`/api/clientes/meus-enderecos/${editingId}`, payload)
      : api.post('/api/clientes/meus-enderecos', payload);

    request
      .then(() => {
        setMessage({ type: 'success', text: editingId ? 'Endereço atualizado!' : 'Endereço adicionado!' });
        fecharModal();
        carregar();
      })
      .catch(err => {
        const msg = err?.response?.data?.message || err?.response?.data || err.message;
        setMessage({ type: 'error', text: 'Erro ao salvar endereço: ' + msg });
      })
      .finally(() => setSaving(false));
  }

  function handleDefinirPrincipal(id) {
    if (!window.confirm('Definir este endereço como o principal?')) return;
    setProcessandoId(id);
    api.patch(`/api/clientes/meus-enderecos/${id}/principal`)
      .then(() => carregar())
      .catch(err => {
        const msg = err?.response?.data?.message || err?.response?.data || err.message;
        setMessage({ type: 'error', text: 'Erro: ' + msg });
      })
      .finally(() => setProcessandoId(null));
  }

  function handleDelete(id) {
    if (!window.confirm('Excluir este endereço?')) return;
    setProcessandoId(id);
    api.delete(`/api/clientes/meus-enderecos/${id}`)
      .then(() => carregar())
      .catch(err => {
        const msg = err?.response?.data?.message || err?.response?.data || err.message;
        setMessage({ type: 'error', text: 'Erro ao excluir: ' + msg });
      })
      .finally(() => setProcessandoId(null));
  }
return (
    <div className="meusEnderecos-container">
      <div className="meusEnderecos-header">
        <h1>Meus Endereços</h1>
        <p className="meusEnderecos-subtitle">Gerencie os endereços de entrega dos seus aluguéis.</p>
      </div>

      {message && (
        <div className={`meusEnderecos-msg ${message.type === 'error' ? 'meusEnderecos-msg-error' : 'meusEnderecos-msg-success'}`}>
          {message.text}
        </div>
      )}

      <div className="meusEnderecos-toolbar">
        <button type="button" className="btnPrimary meusEnderecos-novo" onClick={openNovo}>
          <FontAwesomeIcon icon={faPlus} /> Novo endereço
        </button>
        <Link to="/carrinho" className="btnSecondary">Voltar ao checkout</Link>
      </div>

      {loading ? (
        <div className="loading-container">Carregando endereços...</div>
      ) : enderecos.length === 0 ? (
        <div className="meusEnderecos-vazio">
          <FontAwesomeIcon icon={faMapMarkerAlt} />
          <p>Você ainda não cadastrou nenhum endereço.</p>
          <p className="meusEnderecos-vazio-sub">
            Cadastre endereços para agilizar o fechamento dos seus pedidos no carrinho.
          </p>
        </div>
      ) : (
        <div className="meusEnderecos-lista">
          {enderecos.map((end) => (
            <div key={end.id} className={`endereco-card ${end.principal ? 'endereco-card-principal' : ''}`}>
              <div className="endereco-card-topo">
                <span className="endereco-apelido">
                  <FontAwesomeIcon icon={faMapMarkerAlt} /> {end.apelido || 'Sem apelido'}
                </span>
                {end.principal && <span className="endereco-principal-badge">principal</span>}
              </div>
              <p className="endereco-linha">{end.formatado || formatar(end)}</p>
              <div className="endereco-card-acoes">
                {!end.principal && (
                  <button
                    type="button"
                    className="endereco-acao endereco-acao-principal"
                    onClick={() => handleDefinirPrincipal(end.id)}
                    disabled={processandoId === end.id}
                  >
                    <FontAwesomeIcon icon={faStar} /> Definir principal
                  </button>
                )}
                <button type="button" className="endereco-acao" onClick={() => openEditar(end)}>
                  <FontAwesomeIcon icon={faEdit} /> Editar
                </button>
                <button type="button" className="endereco-acao endereco-acao-delete" onClick={() => handleDelete(end.id)} disabled={processandoId === end.id}>
                  <FontAwesomeIcon icon={faTrash} /> Excluir
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="checkoutBackdrop" onClick={fecharModal}>
          <div className="checkoutModalCard enderecoModal" onClick={(e) => e.stopPropagation()}>
            <div className="checkoutModalHeader">
              <h3>{editingId ? 'Editar endereço' : 'Novo endereço'}</h3>
              <button type="button" className="checkoutCloseBtn" onClick={fecharModal}>✕</button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="enderecoFormGrid">
                <input className="checkoutInput" name="apelido" placeholder="Apelido (ex: Casa, Obra Centro)" value={form.apelido} onChange={handleChange} />
                <input className="checkoutInput" name="cep" placeholder="CEP" value={form.cep} onChange={handleChange} />
                <input className="checkoutInput meusEnderecos-full" name="rua" placeholder="Rua / Logradouro *" value={form.rua} onChange={handleChange} required />
                <input className="checkoutInput" name="numero" placeholder="Número" value={form.numero} onChange={handleChange} />
                <input className="checkoutInput" name="complemento" placeholder="Complemento" value={form.complemento} onChange={handleChange} />
                <input className="checkoutInput" name="bairro" placeholder="Bairro" value={form.bairro} onChange={handleChange} />
                <input className="checkoutInput" name="cidade" placeholder="Cidade *" value={form.cidade} onChange={handleChange} required />
                <input className="checkoutInput" name="estado" placeholder="UF *" maxLength={2} value={form.estado} onChange={handleChange} required />
              </div>

              <div className="checkoutModalFooter">
                <button type="button" className="btnSecondary" onClick={fecharModal}>Cancelar</button>
                <button type="submit" className="btnPrimary" disabled={saving}>
                  {saving ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default MeusEnderecos;