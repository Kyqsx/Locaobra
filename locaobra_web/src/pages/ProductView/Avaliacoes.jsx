import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import { useAuth } from '../../utils/useAuth';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { Estrelas, EstrelasInput } from '../../components/Estrelas';
import { formatarMedia } from '../../utils/avaliacoes';
import './Avaliacoes.css';

const LIMITE_COMENTARIO = 1000;

const formatarData = (iso) => {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('pt-BR');
};

const mensagemDeErro = (err, padrao) => {
  const data = err?.response?.data;
  if (data?.errors) return Object.values(data.errors).join(' ');
  return data?.message || padrao;
};

function Avaliacoes({ equipamentoId, onAtualizado }) {
  const { user, isCliente, isAdmin } = useAuth();
  const podeModerar = isAdmin || user?.cargoFuncionario === 'GERENTE_OPERACOES';

  const [dados, setDados] = useState(null);   // { media, total, distribuicao, avaliacoes }
  const [minha, setMinha] = useState(null);   // { podeAvaliar, motivo, avaliacao }
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);

  const [editando, setEditando] = useState(false);
  const [nota, setNota] = useState(0);
  const [comentario, setComentario] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erroForm, setErroForm] = useState(null);

  const carregar = useCallback(async () => {
    try {
      const [lista, situacao] = await Promise.all([
        api.get(`/api/avaliacoes/equipamento/${equipamentoId}`),
        isCliente
          ? api.get(`/api/avaliacoes/equipamento/${equipamentoId}/minha`)
          : Promise.resolve(null),
      ]);
      setDados(lista.data);
      setMinha(situacao ? situacao.data : null);
      setErro(null);
    } catch {
      setErro('Não foi possível carregar as avaliações.');
    } finally {
      setLoading(false);
    }
  }, [equipamentoId, isCliente]);

  useEffect(() => {
    setLoading(true);
    setEditando(false);
    setNota(0);
    setComentario('');
    carregar();
  }, [carregar]);

  const abrirEdicao = () => {
    setNota(minha.avaliacao.nota);
    setComentario(minha.avaliacao.comentario || '');
    setErroForm(null);
    setEditando(true);
  };

  const cancelarEdicao = () => {
    setEditando(false);
    setNota(0);
    setComentario('');
    setErroForm(null);
  };

  const enviar = async (e) => {
    e.preventDefault();
    if (nota < 1) {
      setErroForm('Escolha uma nota de 1 a 5 estrelas.');
      return;
    }
    setEnviando(true);
    setErroForm(null);
    try {
      if (editando && minha?.avaliacao) {
        await api.put(`/api/avaliacoes/${minha.avaliacao.id}`, { nota, comentario });
      } else {
        await api.post('/api/avaliacoes', { equipamentoId: Number(equipamentoId), nota, comentario });
      }
      cancelarEdicao();
      await carregar();
      if (onAtualizado) onAtualizado();
    } catch (err) {
      setErroForm(mensagemDeErro(err, 'Não foi possível salvar sua avaliação.'));
    } finally {
      setEnviando(false);
    }
  };

  const excluir = async (avaliacao, propria) => {
    const texto = propria
      ? 'Excluir a sua avaliação?'
      : `Excluir a avaliação de ${avaliacao.autor}? Essa ação não pode ser desfeita.`;
    if (!window.confirm(texto)) return;
    try {
      await api.delete(`/api/avaliacoes/${avaliacao.id}`);
      await carregar();
      if (onAtualizado) onAtualizado();
    } catch (err) {
      alert(mensagemDeErro(err, 'Não foi possível excluir a avaliação.'));
    }
  };

  if (loading) {
    return <div className="av-secao"><p className="av-mensagem">Carregando avaliações...</p></div>;
  }
  if (erro) {
    return <div className="av-secao"><p className="av-mensagem av-erro">{erro}</p></div>;
  }

  const total = dados?.total || 0;
  const minhaId = minha?.avaliacao?.id;
  const mostrarForm = isCliente && minha?.podeAvaliar && (!minha.avaliacao || editando);

  return (
    <div className="av-secao">
      <h2 className="av-titulo">Avaliações dos clientes</h2>

      {/* Resumo */}
      {total > 0 ? (
        <div className="av-resumo">
          <div className="av-media">
            <span className="av-media-valor">{formatarMedia(dados.media)}</span>
            <Estrelas valor={dados.media} tamanho={18} />
            <span className="av-media-total">{total} avaliaç{total === 1 ? 'ão' : 'ões'}</span>
          </div>
          <div className="av-distribuicao">
            {[5, 4, 3, 2, 1].map((n) => {
              const qtd = dados.distribuicao?.[n] || 0;
              const pct = total ? (qtd / total) * 100 : 0;
              return (
                <div key={n} className="av-barra-linha">
                  <span className="av-barra-nota">{n} ★</span>
                  <div className="av-barra"><div className="av-barra-preenchida" style={{ width: `${pct}%` }} /></div>
                  <span className="av-barra-qtd">{qtd}</span>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <p className="av-mensagem">Este equipamento ainda não tem avaliações.</p>
      )}

      {/* Área do cliente: avaliar / editar / motivo */}
      {!user && (
        <div className="av-aviso">
          <Link to="/login">Faça login</Link> como cliente para avaliar este equipamento depois de alugá-lo.
        </div>
      )}

      {isCliente && minha && !minha.podeAvaliar && (
        <div className="av-aviso">{minha.motivo}</div>
      )}

      {isCliente && minha?.avaliacao && !editando && (
        <div className="av-aviso av-aviso-ok">
          Você já avaliou este equipamento.{' '}
          <button type="button" className="av-link" onClick={abrirEdicao}>Editar avaliação</button>
        </div>
      )}

      {mostrarForm && (
        <form className="av-form" onSubmit={enviar}>
          <h3 className="av-form-titulo">{editando ? 'Editar sua avaliação' : 'Avalie este equipamento'}</h3>
          <EstrelasInput valor={nota} onChange={setNota} disabled={enviando} />
          <textarea
            className="av-textarea"
            placeholder="Conte como foi sua experiência (opcional)"
            value={comentario}
            maxLength={LIMITE_COMENTARIO}
            onChange={(e) => setComentario(e.target.value)}
            disabled={enviando}
            rows={4}
          />
          <div className="av-contador">{comentario.length}/{LIMITE_COMENTARIO}</div>
          {erroForm && <p className="av-mensagem av-erro">{erroForm}</p>}
          <div className="av-form-acoes">
            <button type="submit" className="btn-main av-btn" disabled={enviando}>
              {enviando ? 'Enviando...' : editando ? 'Salvar alterações' : 'Publicar avaliação'}
            </button>
            {editando && (
              <button type="button" className="btn-secondary" onClick={cancelarEdicao} disabled={enviando}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      )}

      {/* Lista */}
      {total > 0 && (
        <ul className="av-lista">
          {dados.avaliacoes.map((a) => {
            const propria = a.id === minhaId;
            return (
              <li key={a.id} className="av-item">
                <div className="av-item-topo">
                  <Estrelas valor={a.nota} tamanho={13} />
                  <span className="av-autor">{a.autor}</span>
                  {propria && <span className="av-badge">Sua avaliação</span>}
                  <span className="av-data">{formatarData(a.criadoEm)}</span>
                  {(propria || podeModerar) && (
                    <button type="button" className="av-link av-link-perigo" onClick={() => excluir(a, propria)}>
                      <FontAwesomeIcon icon={faTrash} />
                    </button>
                  )}
                </div>
                {a.comentario && <p className="av-comentario">{a.comentario}</p>}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

export default Avaliacoes;
