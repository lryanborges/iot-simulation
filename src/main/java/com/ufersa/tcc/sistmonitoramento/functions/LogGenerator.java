package com.ufersa.tcc.sistmonitoramento.functions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogGenerator {

    private static final Random random = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private double temperatura;
    private double umidade;
    private double pressao;
    private double vento;

    public LogGenerator() {
        temperatura = 26 + random.nextDouble() * 4;    // 26°C a 30°C (limite 22 a 35)
        umidade = 50 + random.nextDouble() * 20;       // 50% a 70% (limite 40 a 80)
        pressao = 1005 + random.nextDouble() * 10;     // 1005 hPa a 1015 hPa (limite 1000 a 1020)
        vento = 5 + random.nextDouble() * 3;           // 5 a 8 m/s (limite 2 a 10)
    }

    public String generateClimateLog1(LocalDateTime now) {
        return String.format("[%s] Temperatura: %.1f°C | Umidade: %.1f%% | Pressão: %.1f hPa | Vento: %.1f m/s",
                formatter.format(now), temperatura, umidade, pressao, vento);
    }

    // Dispositivo IoT2: usa " | " mas sem rótulos
    private String generateClimateLog2(LocalDateTime now) {
        return String.format("[%s] %.1f°C | %.1f%% | %.1f hPa | %.1f m/s",
                formatter.format(now), temperatura, umidade, pressao, vento);
    }

    // Dispositivo IoT3: usa " ; " como separador
    private String generateClimateLog3(LocalDateTime now) {
        return String.format("[%s] Temperatura: %.1f°C ; Umidade: %.1f%% ; Pressão: %.1f hPa ; Vento: %.1f m/s",
                formatter.format(now), temperatura, umidade, pressao, vento);
    }

    // Dispositivo IoT4: JSON formatado
    private String generateClimateLog4(LocalDateTime now) {
        return String.format("{ \"timestamp\": \"%s\", \"temperatura\": %.1f, \"umidade\": %.1f, \"pressao\": %.1f, \"vento\": %.1f }",
                formatter.format(now), temperatura, umidade, pressao, vento);
    }

    // Dispositivo IoT5: CSV puro (sem timestamp)
    private String generateClimateLog5(LocalDateTime now) {
        return String.format("%.1f,%.1f,%.1f,%.1f", temperatura, umidade, pressao, vento);
    }

    public String generateClimateLog(int type) {
        LocalDateTime now = LocalDateTime.now();

        temperatura = varyInfo(temperatura, 0.1, 17, 38);
        umidade = varyInfo(umidade, 0.5, 20, 95);
        pressao = varyInfo(pressao, 0.2, 995, 1025);
        vento = varyInfo(vento, 0.1, 0.5, 15);

        switch (type) {
            case 1: return generateClimateLog1(now);
            case 2: return generateClimateLog2(now);
            case 3: return generateClimateLog3(now);
            case 4: return generateClimateLog4(now);
            case 5: return generateClimateLog5(now);
            default: return "Log inválido.";
        }
    }

    private static double varyInfo(double valorAtual, double variacaoMax, double min, double max) {
        double delta = (random.nextDouble() * 2 - 1) * variacaoMax; // entre -variacao e +variacao
        double attempt = valorAtual + delta;

        // Se ultrapassou o limite superior, inverte a direção da variação
        if (attempt > max) {
            attempt = valorAtual - Math.abs(delta);
        }

        // Se ultrapassou o limite inferior, inverte a direção da variação
        if (attempt < min) {
            attempt = valorAtual + Math.abs(delta);
        }

        // Ainda garante que está dentro dos limites (por segurança)
        return Math.max(min, Math.min(max, attempt));
    }


}
