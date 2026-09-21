import React, { useState } from 'react';
import './Estrelas.css';

// Estrelas somente-leitura. Aceita valores fracionados (ex.: 4.3): a camada
// "cheia" é cortada pela largura proporcional à nota.
export function Estrelas({ valor = 0, tamanho = 14 }) {
  const nota = Math.max(0, Math.min(5, Number(valor) || 0));
  const pct = (nota / 5) * 100;

  return (
    <span
      className="estrelas"
      style={{ fontSize: `${tamanho}px` }}
      role="img"
      aria-label={`Nota ${nota.toFixed(1).replace('.', ',')} de 5`}
    >
      <span className="estrelas-vazias" aria-hidden="true">★★★★★</span>
      <span className="estrelas-cheias" aria-hidden="true" style={{ width: `${pct}%` }}>★★★★★</span>
    </span>
  );
}

// Seletor de nota (1 a 5) para o formulário de avaliação.
export function EstrelasInput({ valor = 0, onChange, disabled = false, tamanho = 28 }) {
  const [hover, setHover] = useState(0);
  const ativo = hover || valor;

  return (
    <div
      className="estrelas-input"
      role="radiogroup"
      aria-label="Sua nota"
      onMouseLeave={() => setHover(0)}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={valor === n}
          aria-label={`${n} estrela${n > 1 ? 's' : ''}`}
          className={`estrela-btn ${n <= ativo ? 'ativa' : ''}`}
          style={{ fontSize: `${tamanho}px` }}
          disabled={disabled}
          onMouseEnter={() => setHover(n)}
          onFocus={() => setHover(n)}
          onBlur={() => setHover(0)}
          onClick={() => onChange && onChange(n)}
        >
          ★
        </button>
      ))}
    </div>
  );
}
