import { useEffect, useMemo } from 'react';

// URL temporária (blob:) para pré-visualizar um File. Criada uma vez por arquivo
// e liberada quando o arquivo muda ou o componente desmonta — em vez de criar uma
// nova URL a cada render.
export function useObjectUrl(file) {
  const url = useMemo(() => (file ? URL.createObjectURL(file) : null), [file]);
  useEffect(() => () => { if (url) URL.revokeObjectURL(url); }, [url]);
  return url;
}
