// Consulta o ViaCEP (https://viacep.com.br) para autocompletar rua, bairro,
// cidade e UF a partir do CEP digitado. É uma API pública, sem autenticação
// e com CORS liberado, então chamamos direto do browser (não passa pelo
// nosso backend).

/** Remove tudo que não é dígito e devolve só os números do CEP. */
export function apenasDigitosCep(cep) {
  return (cep || '').replace(/\D/g, '');
}

/** Formata os dígitos do CEP como "00000-000" enquanto o usuário digita. */
export function formatarCep(valor) {
  const digitos = apenasDigitosCep(valor).slice(0, 8);
  if (digitos.length <= 5) return digitos;
  return `${digitos.slice(0, 5)}-${digitos.slice(5)}`;
}

/**
 * Busca o endereço no ViaCEP. Devolve null se o CEP não tiver os 8 dígitos
 * ou se o ViaCEP não encontrar nada (`{ erro: true }`); lança erro só em
 * caso de falha de rede/resposta inesperada, pra quem chama poder avisar o
 * usuário sem confundir "não encontrado" com "sem internet".
 */
export async function buscarEnderecoPorCep(cep) {
  const digitos = apenasDigitosCep(cep);
  if (digitos.length !== 8) return null;

  const resposta = await fetch(`https://viacep.com.br/ws/${digitos}/json/`);
  if (!resposta.ok) {
    throw new Error(`ViaCEP respondeu status ${resposta.status}`);
  }
  const dados = await resposta.json();
  if (dados.erro) return null;

  return {
    rua: dados.logradouro || '',
    bairro: dados.bairro || '',
    cidade: dados.localidade || '',
    estado: dados.uf || '',
  };
}
