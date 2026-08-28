import React, { useEffect, useState } from 'react';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import EnderecoFields from '../../components/EnderecoFields';
import './MeusEnderecos.css';

const enderecoVazio = { apelido: '', cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '', principal: false };

function MeusEnderecos() {
  const { recarregarEnderecos } = useAuth();
  const [enderecos, setEnderecos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [mensagem, setMensagem] = useState(null);

  const [formAberto, setFormAberto] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(enderecoVazio);
  const [salvando, setSalvando] = useState(false);

  const carregar = () => {
    setLoading(true);
    setErro(null);
    api.get('/api/clientes/meus-enderecos')
      .then((res) => setEnderecos(res.data))
      .catch(() => setErro('Não foi possível carregar seus endereços.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    carregar();
  }, []);

  const abrirNovo = () => {
    setEditandoId(null);
    setForm({ ...enderecoVazio, principal: enderecos.length === 0 });
    setFormAberto(true);
  };

  const abrirEdicao = (endereco) => {
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
  };

  const fecharForm = () => {
    setFormAberto(false);
    setEditandoId(null);
    setForm(enderecoVazio);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSalvando(true);
    setMensagem(null);
    try {
      if (editandoId) {
        await api.put(`/api/clientes/meus-enderecos/${editandoId}`, form);
      } else {
        await api.post('/api/clientes/meus-enderecos', form);
      }
      setMensagem({ tipo: 'sucesso', texto: 'Endereço salvo com sucesso!' });
      fecharForm();
      carregar();
      recarregarEnderecos();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err?.response?.data?.message || 'Não foi possível salvar o endereço.' });
    } finally {
      setSalvando(false);
    }
  };

  const handleRemover = async (endereco) => {
    if (!window.confirm(`Remover o endereço "${endereco.apelido || endereco.formatado}"?`)) return;
    setMensagem(null);
    try {
      await api.delete(`/api/clientes/meus-enderecos/${endereco.id}`);
      carregar();
      recarregarEnderecos();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err?.response?.data?.message || 'Não foi possível remover o endereço.' });
    }
  };

  const handleDefinirPrincipal = async (endereco) => {
    setMensagem(null);
    try {
      await api.patch(`/api/clientes/meus-enderecos/${endereco.id}/principal`);
      carregar();
      recarregarEnderecos();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err?.response?.data?.message || 'Não foi possível definir como principal.' });
    }
  };

  return (
    <div className="meusEnderecosContainer">
      <div className="meusEnderecosHeader">
        <h1>Meus Endereços</h1>
        <p>Endereços salvos pra agilizar o checkout — pode ter mais de um, ex: casa e obra.</p>
      </div>

      {mensagem && (
        <div className={`messageBanner ${mensagem.tipo === 'erro' ? 'negative' : 'positive'}`}>{mensagem.texto}</div>
      )}

      {loading ? (
        <div className="loading-container">Carregando endereços...</div>
      ) : erro ? (
        <div className="error-container">{erro}</div>
      ) : (
        <>
          <div className="enderecosLista">
            {enderecos.length === 0 && !formAberto && (
              <div className="enderecosVazio">Você ainda não tem nenhum endereço salvo.</div>
            )}
            {enderecos.map((endereco) => (
              <div key={endereco.id} className="enderecoCard">
                <div className="enderecoCardHeader">
                  <span className="enderecoApelido">{endereco.apelido || 'Endereço'}</span>
                  {endereco.principal && <span className="enderecoBadgePrincipal">Principal</span>}
                </div>
                <p className="enderecoFormatado">{endereco.formatado}</p>
                <div className="enderecoCardAcoes">
                  {!endereco.principal && (
                    <button type="button" className="btnLink" onClick={() => handleDefinirPrincipal(endereco)}>
                      Definir como principal
                    </button>
                  )}
                  <button type="button" className="btnLink" onClick={() => abrirEdicao(endereco)}>Editar</button>
                  <button type="button" className="btnLink btnLinkDanger" onClick={() => handleRemover(endereco)}>Remover</button>
                </div>
              </div>
            ))}
          </div>

          {!formAberto && (
            <button type="button" className="btnPrimary" onClick={abrirNovo}>+ Adicionar endereço</button>
          )}

          {formAberto && (
            <form className="enderecoFormCard" onSubmit={handleSubmit}>
              <h3>{editandoId ? 'Editar endereço' : 'Novo endereço'}</h3>
              <EnderecoFields value={form} onChange={setForm} showApelido showPrincipal />
              <div className="enderecoFormAcoes">
                <button type="button" className="btnSecondary" onClick={fecharForm} disabled={salvando}>Cancelar</button>
                <button type="submit" className="btnPrimary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar endereço'}
                </button>
              </div>
            </form>
          )}
        </>
      )}
    </div>
  );
}

export default MeusEnderecos;
