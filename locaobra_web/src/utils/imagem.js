// Fotos tiradas no celular costumam ter 3–8 MB. A Vercel recusa requisições com
// mais de 4,5 MB (erro 413), então reduzimos a foto no navegador antes de enviar:
// lado maior de 1600 px em JPEG qualidade 0,8 — costuma cair para 300–600 KB e
// continua nítida o bastante para servir de prova de estado do equipamento.

// Limite seguro de bytes por envio (a Vercel corta em 4,5 MB, com folga para o multipart).
export const TAMANHO_MAX_UPLOAD = 4 * 1024 * 1024;

export async function comprimirImagem(arquivo, { maxLado = 1600, qualidade = 0.8 } = {}) {
  if (!arquivo || !arquivo.type || !arquivo.type.startsWith('image/')) return arquivo;
  if (arquivo.type === 'image/gif' || arquivo.type === 'image/svg+xml') return arquivo;
  if (arquivo.size <= 500 * 1024) return arquivo; // já é pequena

  try {
    const bitmap = await createImageBitmap(arquivo);
    const escala = Math.min(1, maxLado / Math.max(bitmap.width, bitmap.height));
    const largura = Math.round(bitmap.width * escala);
    const altura = Math.round(bitmap.height * escala);

    const canvas = document.createElement('canvas');
    canvas.width = largura;
    canvas.height = altura;
    canvas.getContext('2d').drawImage(bitmap, 0, 0, largura, altura);
    if (bitmap.close) bitmap.close();

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', qualidade));
    if (!blob || blob.size >= arquivo.size) return arquivo; // não ajudou: mantém a original

    const nome = arquivo.name.replace(/\.[^.]+$/, '') + '.jpg';
    return new File([blob], nome, { type: 'image/jpeg', lastModified: Date.now() });
  } catch {
    // Formato que o navegador não consegue decodificar: envia como está.
    return arquivo;
  }
}

export const formatarTamanho = (bytes) => `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
