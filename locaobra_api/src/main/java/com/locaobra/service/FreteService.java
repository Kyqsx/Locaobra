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
//      300 kg/m³ (transporte de cargas em geral; mudanças usam 250). Esse
//      peso considerado gera DUAS coisas no valor final: o "frete peso"
//      (tarifa por kg) e o porte do veículo (que muda o R$/km e o pedágio).
//   2. Distância       -> km estimado entre a origem (depósito) e o destino
//      (endereço do cliente). Geocodifica os CEPs via BrasilAPI e calcula a
//      distância haversine (linha reta) corrigida pra km de rota. Sem CEP
//      nos dois lados, ou API fora do ar, cai numa heurística por cidade/UF.
//      Ver CepGeoService e FreteService.estimarDistanciaKm.
//   3. Ad valorem      -> "seguro" proporcional ao valor da nota (aqui, do
//      pedido de locação), cobre roubo/perda/avaria em trânsito.
//   4. Taxas           -> despacho (emissão do CTE/documentação, escalada
//      pelo porte do veículo — moto não emite CT-e, então paga bem menos que
//      um caminhão) e pedágio (estimado por eixo/km, variando com o porte do
//      veículo) + taxa de entrega difícil (TDE) pra locais sinalizados como
//      de difícil acesso.
//   5. Prazo           -> informativo: estimativa em dias úteis por faixa de
//      distância, sugerida ao cliente no carrinho.
//   6. Tipo de veículo -> sugerido pela carga total (peso cubado/real) e
//      determina o R$/km e o número de eixos usados no pedágio — equipamento
//      grande/pesado sai num veículo maior, que custa mais por km rodado.
//
// RETIRADA nunca passa por aqui — devolve frete zero direto no chamador.
@Service
public class FreteService {

    private final CepGeoService cepGeoService;

    public FreteService(CepGeoService cepGeoService) {
        this.cepGeoService = cepGeoService;
    }

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
    private static final BigDecimal AD_VALOREM_PCT = new BigDecimal("0.006"); // 0,6% do valor da locação
    private static final BigDecimal TARIFA_KG = new BigDecimal("0.35"); // R$ por kg considerado (frete peso)
    private static final BigDecimal PEDAGIO_POR_EIXO_KM = new BigDecimal("0.065"); // R$ por eixo por km
    private static final BigDecimal TAXA_ENTREGA_DIFICIL = new BigDecimal("120.00"); // TDE

    // R$/km, taxa de despacho e nº de eixos por porte de veículo — quanto
    // maior o veículo (exigido pelo peso/cubagem da carga), maior o custo
    // por km, mais burocracia de despacho e mais eixos pagam pedágio. É isso
    // que faz o peso/dimensão do equipamento pesar no valor final, e não só
    // no "veículo sugerido" informativo. A taxa de despacho da moto/carro de
    // passeio é bem menor porque essas entregas não emitem CT-e nem exigem a
    // documentação de carga fracionada de um caminhão — é uma corrida normal
    // de courier/motoboy.
    private static final BigDecimal PRECO_KM_MOTO = new BigDecimal("0.90");
    private static final BigDecimal PRECO_KM_UTILITARIO = new BigDecimal("1.40");
    private static final BigDecimal PRECO_KM_FURGAO = new BigDecimal("1.80");
    private static final BigDecimal PRECO_KM_TRUCK = new BigDecimal("2.60");
    private static final BigDecimal PRECO_KM_CARRETA = new BigDecimal("3.80");

    private static final BigDecimal TAXA_DESPACHO_MOTO = new BigDecimal("12.00");
    private static final BigDecimal TAXA_DESPACHO_UTILITARIO = new BigDecimal("25.00");
    private static final BigDecimal TAXA_DESPACHO_FURGAO = new BigDecimal("45.00");
    private static final BigDecimal TAXA_DESPACHO_TRUCK = new BigDecimal("70.00");
    private static final BigDecimal TAXA_DESPACHO_CARRETA = new BigDecimal("100.00");

    // Moto não paga pedágio de eixo (isenta ou tarifa simbólica na maioria das
    // praças) — o custo dela é só o R$/km e a taxa de despacho reduzida.
    private static final int EIXOS_MOTO = 0;
    private static final int EIXOS_UTILITARIO = 2;
    private static final int EIXOS_FURGAO = 2;
    private static final int EIXOS_TRUCK = 3;
    private static final int EIXOS_CARRETA = 5;

    // Limiar de km que ativa a taxa de dificuldade (rota fora da região).
    private static final int KM_LIMIAR_TDE = 150;
    // Distância usada quando não dá pra estimar a rota (origem desconhecida
    // em estimativa pré-pedido, endereço incompleto etc.).
    private static final int KM_ORIGEM_DESCONHECIDA = 25;

    // Raio médio da Terra (km) — usado no cálculo de distância em linha reta
    // (haversine) entre as coordenadas dos dois CEPs.
    private static final double RAIO_TERRA_KM = 6371.0;

    // Rota real (ruas, avenidas) é sempre mais longa que a linha reta entre
    // dois pontos. Fator de correção padrão de mercado pra estimar km
    // rodado a partir da distância geodésica.
    private static final double FATOR_ROTA_SOBRE_LINHA_RETA = 1.3;

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
    public BigDecimal pesoConsideradoKg(BigDecimal pesoUnitarioKg, Integer quantidade, BigDecimal volumeTotalM3) {
        int qtd = quantidade != null && quantidade > 0 ? quantidade : 1;
        BigDecimal pesoReal = positivoOuPadrao(pesoUnitarioKg, PESO_PADRAO_KG).multiply(BigDecimal.valueOf(qtd));
        BigDecimal pesoCubado = volumeTotalM3.multiply(FATOR_CUBAGEM_KG_M3);
        return pesoReal.max(pesoCubado).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positivoOuPadrao(BigDecimal valor, BigDecimal padrao) {
        return (valor != null && valor.signum() > 0) ? valor : padrao;
    }

    // ====================== VALOR DO FRETE ======================

    // Frete total (R$) = despacho (varia com o porte do veículo) +
    // deslocamento (R$/km do veículo exigido) + pedágio (por eixo do
    // veículo/km) + frete peso (tarifa x kg considerado) + ad valorem (0,6%
    // do valor da locação) + taxa de entrega difícil (explícita, ou rota
    // acima do limiar de km). O peso/cubagem do equipamento entra em TRÊS
    // pontos: define o porte do veículo (logo, o R$/km, a taxa de despacho
    // e os eixos do pedágio) e é cobrado direto via frete peso — por isso um
    // item pesado/volumoso sempre sai mais caro que um leve/pequeno na mesma
    // rota, e uma entrega de moto não paga a burocracia de um caminhão.
    public BigDecimal calcularFrete(
            BigDecimal pesoTotalKg,
            Integer distanciaKm,
            BigDecimal valorMercadoria,
            Boolean entregaDificil) {
        int km = (distanciaKm != null && distanciaKm > 0) ? distanciaKm : KM_ORIGEM_DESCONHECIDA;
        BigDecimal valorLocacao = (valorMercadoria != null && valorMercadoria.signum() > 0)
                ? valorMercadoria : BigDecimal.ZERO;
        BigDecimal peso = (pesoTotalKg != null && pesoTotalKg.signum() > 0) ? pesoTotalKg : PESO_PADRAO_KG;

        TipoVeiculo veiculo = sugerirVeiculo(peso);
        BigDecimal precoKm = precoKmPorVeiculo(veiculo);
        BigDecimal taxaDespacho = taxaDespachoPorVeiculo(veiculo);
        int eixos = eixosPorVeiculo(veiculo);

        BigDecimal deslocamento = precoKm.multiply(BigDecimal.valueOf(km));
        BigDecimal pedagio = PEDAGIO_POR_EIXO_KM
                .multiply(BigDecimal.valueOf(eixos))
                .multiply(BigDecimal.valueOf(km));
        BigDecimal fretePeso = TARIFA_KG.multiply(peso);
        BigDecimal adValorem = valorLocacao.multiply(AD_VALOREM_PCT);
        boolean dificil = Boolean.TRUE.equals(entregaDificil) || km > KM_LIMIAR_TDE;
        BigDecimal tde = dificil ? TAXA_ENTREGA_DIFICIL : BigDecimal.ZERO;

        return taxaDespacho
                .add(deslocamento)
                .add(pedagio)
                .add(fretePeso)
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

    // Distância estimada (km) entre a origem (depósito) e o destino (cliente).
    // Duas camadas:
    //   1. CEP real -> geocodifica os dois endereços (BrasilAPI) e calcula a
    //      distância em linha reta (haversine), com um fator de correção pra
    //      aproximar de km rodado. Só usa quando os DOIS CEPs resolvem.
    //   2. Sem CEP, ou API fora do ar -> heurística por cidade/UF (fallback,
    //      mantém o sistema funcionando mesmo sem rede/CEP cadastrado):
    //        mesma cidade           -> 25 km
    //        mesma UF, outra cidade -> 120 km
    //        UFs diferentes         -> 450–650 km (varia pelo nome da cidade,
    //                                  só pra dar granularidade sem depender
    //                                  de serviço externo)
    public int estimarDistanciaKm(Endereco origem, Endereco destino) {
        if (origem == null || destino == null
                || isBlank(origem.getCidade()) || isBlank(destino.getCidade())) {
            return KM_ORIGEM_DESCONHECIDA;
        }

        Integer kmReal = estimarDistanciaKmPorCep(origem.getCep(), destino.getCep());
        if (kmReal != null) {
            return kmReal;
        }

        String cidadeOrigem = normalizar(origem.getCidade());
        String ufOrigem = normalizar(origem.getEstado());
        String cidadeDestino = normalizar(destino.getCidade());
        String ufDestino = normalizar(destino.getEstado());

        if (cidadeOrigem.equals(cidadeDestino)) return 25;
        if (!ufOrigem.isEmpty() && ufOrigem.equals(ufDestino)) return 120;
        return 450 + Math.abs(cidadeOrigem.hashCode()) % 200;
    }

    // Distância real via CEP, ou null se não deu pra calcular (CEP ausente/
    // inválido em algum dos dois lados, ou a API não respondeu) — nesse caso
    // quem chama cai na heurística por cidade/UF.
    private Integer estimarDistanciaKmPorCep(String cepOrigem, String cepDestino) {
        double[] coordOrigem = cepGeoService.buscarCoordenadas(cepOrigem);
        if (coordOrigem == null) {
            return null;
        }
        double[] coordDestino = cepGeoService.buscarCoordenadas(cepDestino);
        if (coordDestino == null) {
            return null;
        }

        double linhaRetaKm = distanciaHaversineKm(
                coordOrigem[0], coordOrigem[1], coordDestino[0], coordDestino[1]);
        double kmEstrada = linhaRetaKm * FATOR_ROTA_SOBRE_LINHA_RETA;
        return Math.max(1, (int) Math.round(kmEstrada));
    }

    // Distância em linha reta (km) entre duas coordenadas — fórmula de
    // haversine, considerando a Terra como uma esfera de raio RAIO_TERRA_KM.
    private static double distanciaHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_KM * c;
    }

    // ====================== TIPO DE VEÍCULO ======================

    // Sugestão de porte do veículo pela carga total (peso considerado) —
    // determina o R$/km e os eixos usados em calcularFrete, além de ser
    // exibido ao cliente/logística.
    public TipoVeiculo sugerirVeiculo(BigDecimal pesoTotalKg) {
        BigDecimal kg = pesoTotalKg != null ? pesoTotalKg : BigDecimal.ZERO;
        if (kg.compareTo(new BigDecimal("30")) <= 0) return TipoVeiculo.MOTO;
        if (kg.compareTo(new BigDecimal("300")) <= 0) return TipoVeiculo.UTILITARIO;
        if (kg.compareTo(new BigDecimal("800")) <= 0) return TipoVeiculo.FURGAO;
        if (kg.compareTo(new BigDecimal("2500")) <= 0) return TipoVeiculo.TRUCK;
        return TipoVeiculo.CARRETA;
    }

    private BigDecimal precoKmPorVeiculo(TipoVeiculo veiculo) {
        return switch (veiculo) {
            case MOTO -> PRECO_KM_MOTO;
            case UTILITARIO -> PRECO_KM_UTILITARIO;
            case FURGAO -> PRECO_KM_FURGAO;
            case TRUCK -> PRECO_KM_TRUCK;
            case CARRETA -> PRECO_KM_CARRETA;
        };
    }

    private BigDecimal taxaDespachoPorVeiculo(TipoVeiculo veiculo) {
        return switch (veiculo) {
            case MOTO -> TAXA_DESPACHO_MOTO;
            case UTILITARIO -> TAXA_DESPACHO_UTILITARIO;
            case FURGAO -> TAXA_DESPACHO_FURGAO;
            case TRUCK -> TAXA_DESPACHO_TRUCK;
            case CARRETA -> TAXA_DESPACHO_CARRETA;
        };
    }

    private int eixosPorVeiculo(TipoVeiculo veiculo) {
        return switch (veiculo) {
            case MOTO -> EIXOS_MOTO;
            case UTILITARIO -> EIXOS_UTILITARIO;
            case FURGAO -> EIXOS_FURGAO;
            case TRUCK -> EIXOS_TRUCK;
            case CARRETA -> EIXOS_CARRETA;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}