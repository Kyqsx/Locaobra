// 4.3 -> "4,3" (sempre 1 casa decimal, formato pt-BR)
export const formatarMedia = (media) =>
  Number(media || 0).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
