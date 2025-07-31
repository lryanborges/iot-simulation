package com.ufersa.tcc.sistmonitoramento.datagram;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ClimateLogger {
    private static final Random random = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Valores anteriores persistentes
    private static double temperatura = 20 + random.nextDouble() * 10; // Inicial de 20°C a 30°C
    private static double umidade = 50 + random.nextDouble() * 30;     // 50% a 80%
    private static double pressao = 990 + random.nextDouble() * 20;    // 990 hPa a 1010 hPa
    private static double vento = 2 + random.nextDouble() * 3;         // 2 a 5 m/s

    public static String generateClimateLog1() {
        LocalDateTime agora = LocalDateTime.now();

        // Define a variação máxima permitida para cada variável
        temperatura = variarLevemente(temperatura, 0.5, 15, 35);
        umidade = variarLevemente(umidade, 2.0, 30, 100);
        pressao = variarLevemente(pressao, 1.5, 980, 1020);
        vento = variarLevemente(vento, 0.7, 0, 15);

        return String.format("[%s] Temperatura: %.1f°C | Umidade: %.1f%% | Pressão: %.1f hPa | Vento: %.1f m/s",
                formatter.format(agora), temperatura, umidade, pressao, vento);
    }

    private static double variarLevemente(double valorAtual, double variacaoMax, double min, double max) {
        double delta = (random.nextDouble() * 2 - 1) * variacaoMax; // valor entre -variação e +variação
        double novoValor = valorAtual + delta;
        // Garante que o valor fique dentro do intervalo permitido
        return Math.max(min, Math.min(max, novoValor));
    }
}
