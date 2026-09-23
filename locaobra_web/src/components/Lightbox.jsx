// Imagem ampliada em tela cheia. Estilos em styles/shared.css (.lightbox*).
export default function Lightbox({ src, onClose }) {
  if (!src) return null;
  return (
    <div className="lightboxBackdrop" onClick={onClose}>
      <img src={src} alt="Imagem ampliada" className="lightboxImage" onClick={e => e.stopPropagation()} />
      <button type="button" className="lightboxCloseBtn" onClick={onClose} title="Fechar">✕</button>
    </div>
  );
}
