import { useObjectUrl } from '../utils/useObjectUrl';
import './ImagePreviewGrid.css';

function Thumb({ file, onRemove, onOpen }) {
  const url = useObjectUrl(file);
  return (
    <div className="imagePreviewItem">
      {url && (
        <img
          src={url}
          alt={file.name}
          className={onOpen ? 'clickable' : undefined}
          onClick={onOpen ? () => onOpen(url, file) : undefined}
          title={onOpen ? 'Clique para ver a imagem inteira' : undefined}
        />
      )}
      {onRemove && (
        <button type="button" className="imagePreviewRemove" onClick={onRemove} title="Remover">✕</button>
      )}
    </div>
  );
}

/**
 * Grade de miniaturas quadradas para arquivos de imagem escolhidos.
 * Recebe File[] e cuida sozinha das object URLs (criação e liberação).
 *
 * Props:
 *  - files: File[]
 *  - onRemove(index)         mostra o botão ✕ em cada miniatura
 *  - onOpen(url, file)       torna a miniatura clicável (ex.: abrir no Lightbox)
 */
export default function ImagePreviewGrid({ files, onRemove, onOpen }) {
  if (!files || files.length === 0) return null;
  return (
    <div className="imagePreviewGridBox">
      {files.map((file, i) => (
        <Thumb
          key={`${file.name}-${file.size}-${file.lastModified}-${i}`}
          file={file}
          onOpen={onOpen}
          onRemove={onRemove ? () => onRemove(i) : undefined}
        />
      ))}
    </div>
  );
}
