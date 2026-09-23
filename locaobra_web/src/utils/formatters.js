// Formata 'YYYY-MM-DD' como 'DD/MM/YYYY'.
export const formatarData = (iso) => {
  if (!iso) return '—';
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
};

// Formata data/hora ISO no padrão pt-BR ('DD/MM/YYYY HH:MM').
export function formatDate(dateStr) {
  if (!dateStr) return '---';
  const d = new Date(dateStr);
  return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}
