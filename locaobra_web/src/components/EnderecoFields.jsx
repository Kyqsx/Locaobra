import React from 'react';
import './EnderecoFields.css';

// Grade de campos de endereço reutilizada em: cadastro/edição de cliente,
// depósito, e checkout (carrinho). `value` é sempre o formato "achatado"
// que o backend espera em EnderecoRequest: { apelido, cep, rua, numero,
// complemento, bairro, cidade, estado, principal }.
function EnderecoFields({ value, onChange, showApelido = false, showPrincipal = false, prefixo }) {
  const handleField = (campo) => (e) => {
    onChange({ ...value, [campo]: e.target.value });
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
        <input
          id={id('cep')}
          className="enderecoInput"
          value={value.cep || ''}
          onChange={handleField('cep')}
          placeholder="00000-000"
        />
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
