package com.locaobra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Geocodificação de CEP (BrasilAPI) — resolve um CEP em latitude/longitude
// real. Usado pelo FreteService pra calcular a distância de rota de verdade
// (haversine) em vez da heurística por cidade/UF.
//
// Timeouts curtos e falha SEMPRE silenciosa (retorna null): se a BrasilAPI
// estiver fora do ar, o CEP não tiver coordenadas cadastradas, ou não tiver
// rede, quem chama (FreteService) cai de volta na heurística — a estimativa
// de frete nunca quebra por causa disso.
@Service
public class CepGeoService {

    private static final String BRASIL_API_URL = "https://brasilapi.com.br/api/cep/v2/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // Cache simples em memória do processo: o mesmo CEP (ex.: o de um
    // depósito) é consultado de novo em toda estimativa/confirmação — evita
    // bater na API repetidas vezes pro mesmo endereço.
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    // Coordenadas [latitude, longitude] do CEP, ou null se não foi possível
    // resolver (CEP inválido/vazio, API fora do ar, sem coordenadas etc.).
    public double[] buscarCoordenadas(String cepBruto) {
        String cep = normalizarCep(cepBruto);
        if (cep == null) {
            return null;
        }

        double[] emCache = cache.get(cep);
        if (emCache != null) {
            return emCache;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BRASIL_API_URL + cep))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode coordinates = mapper.readTree(response.body()).path("location").path("coordinates");
            double lat = coordinates.path("latitude").asDouble(Double.NaN);
            double lon = coordinates.path("longitude").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon) || (lat == 0 && lon == 0)) {
                return null;
            }

            double[] coords = {lat, lon};
            cache.put(cep, coords);
            return coords;
        } catch (Exception e) {
            // Rede fora do ar, timeout, JSON inesperado etc. — nunca propaga.
            return null;
        }
    }

    private static String normalizarCep(String cepBruto) {
        if (cepBruto == null) {
            return null;
        }
        String cep = cepBruto.replaceAll("\\D", "");
        return cep.length() == 8 ? cep : null;
    }
}
