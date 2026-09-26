import React, { useRef, useState } from 'react';
import './EnderecoFields.css';
import { buscarEnderecoPorCep, formatarCep } from '../utils/viacep';

// Grade de campos de endereço reutilizada em: cadastro/edição de cliente,
// depósito, e checkout (carrinho). `value` é sempre o formato "achatado"
// que o backend espera em EnderecoRequest: { apelido, cep, rua, numero,
// complemento, bairro, cidade, estado, principal }.
function EnderecoFields({ value, onChange, showApelido = false, showPrincipal = false, prefixo }) {
  const [buscandoCep, setBuscandoCep] = useState(false);
  const [cepNaoEncontrado, setCepNaoEncontrado] = useState(false);
  const ultimoCepBuscado = useRef(null);

  const handleField = (campo) => (e) => {
    onChange({ ...value, [campo]: e.target.value });
  };

  const handleCepChange = (e) => {
    const cepFormatado = formatarCep(e.target.value);
    setCepNaoEncontrado(false);
    onChange({ ...value, cep: cepFormatado });

    const digitos = cepFormatado.replace(/\D/g, '');
    if (digitos.length !== 8) return;

    ultimoCepBuscado.current = digitos;
    setBuscandoCep(true);
    buscarEnderecoPorCep(digitos)
      .then((endereco) => {
        // Se o usuário já mudou o CEP de novo enquanto essa busca ainda
        // estava no ar, a resposta é descartada pra não sobrescrever o
        // campo com um endereço de um CEP antigo.
        if (ultimoCepBuscado.current !== digitos) return;
        if (!endereco) {
          setCepNaoEncontrado(true);
          return;
        }
        onChange({
          ...value,
          cep: cepFormatado,
          rua: endereco.rua || value.rua || '',
          bairro: endereco.bairro || value.bairro || '',
          cidade: endereco.cidade || value.cidade || '',
          estado: endereco.estado || value.estado || '',
        });
      })
      .catch(() => {
        // Falha de rede na consulta não deve travar o formulário — o
        // usuário sempre pode preencher o endereço manualmente.
        if (ultimoCepBuscado.current === digitos) setCepNaoEncontrado(false);
      })
      .finally(() => {
        if (ultimoCepBuscado.current === digitos) setBuscandoCep(false);
      });
  };

  const id = (campo) => (prefixo ? `${prefixo}-${campo}` : campo);

  return (
    <div className="enderecoFieldsGrid">
      {showApelido && (
        <div className="enderecoField enderecoFieldFull">
          <label htmlFor={id('apelido')}>Apelido (ex: Casa, Obra Centro)</label>
          <input
            id={id('apelido')}
            className="enderecoInput"
            value={value.apelido || ''}
            onChange={handleField('apelido')}
            placeholder="Como você quer chamar esse endereço"
          />
        </div>
      )}

      <div className="enderecoField">
        <label htmlFor={id('cep')}>CEP</label>
        <div className="enderecoCepWrapper">
          <input
            id={id('cep')}
            className="enderecoInput"
            value={value.cep || ''}
            onChange={handleCepChange}
            placeholder="00000-000"
            inputMode="numeric"
            maxLength={9}
          />
          {buscandoCep && <span className="enderecoCepSpinner" aria-label="Buscando CEP" />}
        </div>
        {cepNaoEncontrado && (
          <span className="enderecoCepAviso">CEP não encontrado, preencha manualmente.</span>
        )}
      </div>

      <div className="enderecoField enderecoFieldWide">
        <label htmlFor={id('rua')}>Rua *</label>
        <input
          id={id('rua')}
          className="enderecoInput"
          value={value.rua || ''}
          onChange={handleField('rua')}
          placeholder="Nome da rua"
          required
        />
      </div>

      <div className="enderecoField">
        <label htmlFor={id('numero')}>Número</label>
        <input
          id={id('numero')}
          className="enderecoInput"
          value={value.numero || ''}
          onChange={handleField('numero')}
          placeholder="Nº"
        />
      </div>

      <div className="enderecoField">
        <label htmlFor={id('complemento')}>Complemento</label>
        <input
          id={id('complemento')}
          className="enderecoInput"
          value={value.complemento || ''}
          onChange={handleField('complemento')}
          placeholder="Apto, bloco..."
        />
      </div>

      <div className="enderecoField">
        <label htmlFor={id('bairro')}>Bairro</label>
        <input
          id={id('bairro')}
          className="enderecoInput"
          value={value.bairro || ''}
          onChange={handleField('bairro')}
        />
      </div>

      <div className="enderecoField enderecoFieldWide">
        <label htmlFor={id('cidade')}>Cidade *</label>
        <input
          id={id('cidade')}
          className="enderecoInput"
          value={value.cidade || ''}
          onChange={handleField('cidade')}
          required
        />
      </div>

      <div className="enderecoField">
        <label htmlFor={id('estado')}>UF *</label>
        <input
          id={id('estado')}
          className="enderecoInput"
          value={value.estado || ''}
          onChange={(e) => onChange({ ...value, estado: e.target.value.toUpperCase().slice(0, 2) })}
          placeholder="SP"
          maxLength={2}
          required
        />
      </div>

      {showPrincipal && (
        <label className="enderecoPrincipalCheck enderecoFieldFull">
          <input
            type="checkbox"
            checked={Boolean(value.principal)}
            onChange={(e) => onChange({ ...value, principal: e.target.checked })}
          />
          Definir como endereço principal
        </label>
      )}
    </div>
  );
}

export default EnderecoFields;
