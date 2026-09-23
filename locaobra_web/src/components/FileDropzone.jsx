import { useRef, useState, useEffect } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCloudArrowUp } from '@fortawesome/free-solid-svg-icons';
import './FileDropzone.css';

// Confere o arquivo contra o atributo `accept` do input ("image/*", ".pdf", "image/png").
// O <input> só filtra pelo seletor do sistema; no drag-and-drop a checagem é nossa.
function aceita(file, accept) {
  if (!accept) return true;
  const nome = file.name.toLowerCase();
  const tipo = (file.type || '').toLowerCase();
  return accept
    .split(',')
    .map(r => r.trim().toLowerCase())
    .filter(Boolean)
    .some(r => {
      if (r.startsWith('.')) return nome.endsWith(r);
      if (r.endsWith('/*')) return tipo.startsWith(r.slice(0, -1));
      return tipo === r;
    });
}

/**
 * Área de upload com clique, teclado e arrastar-e-soltar.
 *
 * Props:
 *  - onFiles(files: File[])  chamado só com os arquivos aceitos (1 item se !multiple)
 *  - accept, multiple, capture, disabled  repassados ao <input type="file">
 *  - label / hint   textos da área (têm padrão em pt-BR)
 *  - icon           ícone FontAwesome (padrão: nuvem de upload)
 *  - busy           mostra "Processando..." e bloqueia novas seleções
 *  - compact        versão em linha, para formulários apertados
 *
 * Estilos em FileDropzone.css.
 */
export default function FileDropzone({
  onFiles,
  accept,
  multiple = false,
  capture,
  disabled = false,
  busy = false,
  compact = false,
  label,
  hint,
  icon = faCloudArrowUp,
  className = '',
}) {
  const inputRef = useRef(null);
  const timerRef = useRef(null);
  const [arrastando, setArrastando] = useState(false);
  const [erro, setErro] = useState(null);
  const bloqueado = disabled || busy;

  useEffect(() => () => clearTimeout(timerRef.current), []);

  function mostrarErro(msg) {
    setErro(msg);
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => setErro(null), 4000);
  }

  function receber(lista) {
    const todos = Array.from(lista || []);
    if (todos.length === 0) return;
    const validos = todos.filter(f => aceita(f, accept));
    if (validos.length < todos.length) {
      mostrarErro(
        validos.length === 0
          ? 'Tipo de arquivo não aceito.'
          : 'Alguns arquivos foram ignorados (tipo não aceito).'
      );
    } else {
      setErro(null);
    }
    if (validos.length === 0) return;
    onFiles(multiple ? validos : validos.slice(0, 1));
  }

  function abrirSeletor() {
    if (!bloqueado) inputRef.current?.click();
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      abrirSeletor();
    }
  }

  function handleDragOver(e) {
    e.preventDefault(); // sem isso o navegador não aceita o drop
    if (bloqueado) {
      e.dataTransfer.dropEffect = 'none';
      return;
    }
    e.dataTransfer.dropEffect = 'copy';
    setArrastando(true);
  }

  function handleDragLeave(e) {
    // dragleave também dispara ao passar por elementos filhos — só desliga ao sair de verdade
    if (!e.currentTarget.contains(e.relatedTarget)) setArrastando(false);
  }

  function handleDrop(e) {
    e.preventDefault();
    setArrastando(false);
    if (bloqueado) return;
    receber(e.dataTransfer.files);
  }

  function handleInputChange(e) {
    receber(e.target.files);
    e.target.value = ''; // permite escolher o mesmo arquivo de novo
  }

  const titulo = busy
    ? 'Processando...'
    : label || (multiple ? 'Arraste os arquivos aqui' : 'Arraste o arquivo aqui');
  const sub = busy ? null : hint === undefined ? 'ou clique para selecionar' : hint;

  const classes = [
    'fileDropzone',
    compact && 'compact',
    arrastando && 'dragging',
    bloqueado && 'disabled',
    className,
  ].filter(Boolean).join(' ');

  return (
    <div className="fileDropzoneWrapper">
      <div
        className={classes}
        role="button"
        tabIndex={bloqueado ? -1 : 0}
        aria-disabled={bloqueado}
        onClick={abrirSeletor}
        onKeyDown={handleKeyDown}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <FontAwesomeIcon icon={icon} className="fileDropzoneIcon" />
        <div className="fileDropzoneText">
          <span className="fileDropzoneTitle">{arrastando ? 'Solte para adicionar' : titulo}</span>
          {sub && !arrastando && <span className="fileDropzoneHint">{sub}</span>}
        </div>
        <input
          ref={inputRef}
          type="file"
          hidden
          accept={accept}
          multiple={multiple}
          capture={capture}
          disabled={disabled}
          onChange={handleInputChange}
        />
      </div>
      {erro && <p className="fileDropzoneError" role="alert">{erro}</p>}
    </div>
  );
}
