package com.locaobra.enums;

// Porte de veículo sugerido pelo FreteService a partir da carga (peso
// considerado). Define o R$/km e os eixos usados no cálculo do frete — não é
// só informativo, equipamento pesado/volumoso sai mais caro por causa disso.
public enum TipoVeiculo {
    MOTO,        // até ~30 kg
    UTILITARIO,  // 30–300 kg (carro pequeno / kombi)
    FURGAO,      // 300–800 kg (furgão / HR / 3/4)
    TRUCK,       // 800–2.500 kg
    CARRETA      // acima de 2.500 kg
}