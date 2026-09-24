package com.locaobra.service;

import com.locaobra.entity.Endereco;
import com.locaobra.enums.TipoVeiculo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

// Cálculo de frete das locações. Implementa os fatores clássicos do frete
// brasileiro, todos isolados em constantes pra ficar fácil de ajustar (ou
// trocar por uma integração real de roteirização/transportadora depois):
//
//   1. Peso x cubagem  -> cobra-se pelo MAIOR entre peso real bruto e o peso
//      cubado (espaço ocupado no veículo). Fator de cubagem padrão do mercado:
//      300 kg/m³ (transporte de cargas em geral; mudanças usam 250).
//   2. Distância       -> km estimado entre a origem (depósito) e o destino
//      (endereço do cliente). Aqui é uma HEURÍSTICA por cidade/UF — sem
//      chamada a API de mapas. Quando houver integração real (Google Distance
//      Matrix, OpenRouteService etc.), só este método muda.
//   3. Ad valorem      -> "seguro" proporcional ao valor da nota (aqui, do
//      pedido de locação), cobre roubo/perda/avaria em trânsito.
//   4. Taxas           -> despacho (emissão do CTE/documentação) e pedágio
//      (estimado por eixo/km) + taxa de entrega difícil (TDE) pra locais
//      sinalizados como de difícil acesso.
//   5. Prazo           -> informativo: estimativa em dias úteis por faixa de
//      distância, sugerida ao cliente no carrinho.
//   6. Tipo de veículo -> sugerido pela carga total (peso cubado/real) e
//      distância, apenas informativo pra logística.
//
// RETIRADA nunca passa por aqui — devolve frete zero direto no chamador.
@Service
public class FreteService {

    // ====================== PARÂMETROS DO CÁLCULO ======================

    // Fator de cubagem (kg por m³) — mercado brasileiro de cargas.
    private static final BigDecimal FATOR_CUBAGEM_KG_M3 = new BigDecimal("300");

    // Peso e dimensões padrão quando o equipamento não tem dados logísticos
    // cadastrados. 80 kg / 120x80x120 cm ≈ 1,15 m³ → cubado ≈ 345 kg.
    private static final BigDecimal PESO_PADRAO_KG = new BigDecimal("80");
    private static final BigDecimal COMPRIMENTO_PADRAO_CM = new BigDecimal("120");
    private static final BigDecimal LARGURA_PADRAO_CM = new BigDecimal("80");
    private static final BigDecimal ALTURA_PADRAO_CM = new BigDecimal("120");

    // Componentes do valor do frete (R$).
    private static final BigDecimal TAXA_DESPACHO = new BigDecimal("45.00");
    private static final BigDecimal AD_VALOREM_PCT = new BigDecimal("0.006"); // 0,6% do valor da locação
    private static final BigDecimal PRECO_KM = new BigDecimal("1.80"); // R$/km médio (diesel + manutenção)
    private static final BigDecimal PEDAGIO_POR_EIXO_KM = new BigDecimal("0.065"); // R$ por eixo por km
    private static final BigDecimal TAXA_ENTREGA_DIFICIL = new BigDecimal("120.00"); // TDE
    private static final int EIXOS_VEICULO_MEDIO = 4;

    // Limiar de km que ativa a taxa de dificuldade (rota fora da região).
    private static final int KM_LIMIAR_TDE = 150;
    // Distância usada quando não dá pra estimar a rota (origem desconhecida
    // em estimativa pré-pedido, endereço incompleto etc.).
    private static final int KM_ORIGEM_DESCONHECIDA = 25;

    // ====================== PESO / CUBAGEM ======================

    // Fator de cubagem público, pra exibir a regra na tela se precisar.
    public BigDecimal fatorCubagem() {
        return FATOR_CUBAGEM_KG_M3;
    }

    // Volume (m³) ocupado por um item da locação, a partir das dimensões em cm
    // cadastradas no equipamento. Usa o valor padrão quando o campo está vazio
    // e multiplica pela quantidade.
    public BigDecimal volumeM3(BigDecimal comprimentoCm, BigDecimal larguraCm, BigDecimal alturaCm, Integer quantidade) {
        int qtd = quantidade != null && quantidade > 0 ? quantidade : 1;
        BigDecimal c = positivoOuPadrao(comprimentoCm, COMPRIMENTO_PADRAO_CM);
        BigDecimal l = positivoOuPadrao(larguraCm, LARGURA_PADRAO_CM);
        BigDecimal a = positivoOuPadrao(alturaCm, ALTURA_PADRAO_CM);
        // cm³ -> m³
        return c.multiply(l).multiply(a).multiply(BigDecimal.valueOf(qtd))
                .divide(BigDecimal.valueOf(1_000_000L), 4, RoundingMode.HALF_UP);
    }

    // Peso (kg) considerado pra um item: o MAIOR entre o peso real bruto
    // (peso unitário x quantidade) e o peso cubado (volume total x fator). É a
    // regra "a transportadora cobra pelo maior valor entre os dois".
    public BigDecimal pesoConsideradoKg(BigDecimal pesoUnitarioKg, BigDecimal volumeTotalM3) {
        BigDecimal pesoReal = positivoOuPadrao(pesoUnitarioKg, PESO_PADRAO_KG);
        BigDecimal pesoCubado = volumeTotalM3.multiply(FATOR_CUBAGEM_KG_M3);
        return pesoReal.max(pesoCubado).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positivoOuPadrao(BigDecimal valor, BigDecimal padrao) {
        return (valor != null && valor.signum() > 0) ? valor : padrao;
    }

    // ====================== VALOR DO FRETE ======================

    // Frete total (R$) = despacho + deslocamento (R$/km) + pedágio (por eixo/km)
    // + ad valorem (0,6% do valor da locação) + taxa de entrega difícil
    // (explícita, ou rota acima do limiar de km). O peso influi no porte do
    // veículo sugerido; o R$/km já embute o porte médio da frota.
    public BigDecimal calcularFrete(
            BigDecimal pesoTotalKg,
            Integer distanciaKm,
            BigDecimal valorMercadoria,
            Boolean entregaDificil) {
        int km = (distanciaKm != null && distanciaKm > 0) ? distanciaKm : KM_ORIGEM_DESCONHECIDA;
        BigDecimal valorLocacao = (valorMercadoria != null && valorMercadoria.signum() > 0)
                ? valorMercadoria : BigDecimal.ZERO;

        BigDecimal deslocamento = PRECO_KM.multiply(BigDecimal.valueOf(km));
        BigDecimal pedagio = PEDAGIO_POR_EIXO_KM
                .multiply(BigDecimal.valueOf(EIXOS_VEICULO_MEDIO))
                .multiply(BigDecimal.valueOf(km));
        BigDecimal adValorem = valorLocacao.multiply(AD_VALOREM_PCT);
        boolean dificil = Boolean.TRUE.equals(entregaDificil) || km > KM_LIMIAR_TDE;
        BigDecimal tde = dificil ? TAXA_ENTREGA_DIFICIL : BigDecimal.ZERO;

        return TAXA_DESPACHO
                .add(deslocamento)
                .add(pedagio)
                .add(adValorem)
                .add(tde)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Prazo estimado (dias úteis) por faixa de distância — informativo, exibido
    // como "prazo estimado de entrega" no carrinho.
    public int prazoEstimadoDias(Integer distanciaKm) {
        int km = (distanciaKm != null && distanciaKm > 0) ? distanciaKm : KM_ORIGEM_DESCONHECIDA;
        if (km <= 30) return 1;
        if (km <= 100) return 2;
        if (km <= 300) return 3;
        if (km <= 700) return 5;
        return 7;
    }

    // ====================== DISTÂNCIA (HEURÍSTICA) ======================

    // Distância estimada (km) entre a origem (depósito) e o destino (cliente),
    // por cidade/UF — SEM API de mapas. Faixas:
    //   mesma cidade           -> 25 km
    //   mesma UF, outra cidade -> 120 km
    //   UFs diferentes         -> 450–650 km (varia pelo nome da cidade, só pra
    //                              dar granularidade sem depender de serviço externo)
    // Quando houver integração real de roteirização (Google Distance Matrix,
    // OpenRouteService etc.), substituir APENAS este método — o resto do
    // cálculo permanece intacto.
    public int estimarDistanciaKm(Endereco origem, Endereco destino) {
        if (origem == null || destino == null
                || isBlank(origem.getCidade()) || isBlank(destino.getCidade())) {
            return KM_ORIGEM_DESCONHECIDA;
        }
        String cidadeOrigem = normalizar(origem.getCidade());
        String ufOrigem = normalizar(origem.getEstado());
        String cidadeDestino = normalizar(destino.getCidade());
        String ufDestino = normalizar(destino.getEstado());

        if (cidadeOrigem.equals(cidadeDestino)) return 25;
        if (!ufOrigem.isEmpty() && ufOrigem.equals(ufDestino)) return 120;
        return 450 + Math.abs(cidadeOrigem.hashCode()) % 200;
    }

    // ====================== TIPO DE VEÍCULO (informativo) ======================

    // Sugestão de porte do veículo pela carga total (peso considerado) —
    // informativo pra logística, hoje não altera o valor do frete.
    public TipoVeiculo sugerirVeiculo(BigDecimal pesoTotalKg) {
        BigDecimal kg = pesoTotalKg != null ? pesoTotalKg : BigDecimal.ZERO;
        if (kg.compareTo(new BigDecimal("30")) <= 0) return TipoVeiculo.MOTO;
        if (kg.compareTo(new BigDecimal("300")) <= 0) return TipoVeiculo.UTILITARIO;
        if (kg.compareTo(new BigDecimal("800")) <= 0) return TipoVeiculo.FURGAO;
        if (kg.compareTo(new BigDecimal("2500")) <= 0) return TipoVeiculo.TRUCK;
        return TipoVeiculo.CARRETA;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}