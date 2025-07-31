package com.ufersa.tcc.sistmonitoramento.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogNormalizer {

    public static String normalize(String log) {

        int deviceType = detectFormat(log);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        double temperatura = 0, umidade = 0, pressao = 0, vento = 0;

        try {
            switch (deviceType) {
                case 1: // "[data] Temperatura: X°C | Umidade: Y% | Pressão: Z hPa | Vento: W m/s"
                    String content1 = log.replaceFirst("^\\[[^\\]]+\\]\\s*", ""); // remove [timestamp]
                    String[] parts1 = content1.split("\\|");
                    temperatura = parseValue(parts1[0], "Temperatura");
                    umidade = parseValue(parts1[1], "Umidade");
                    pressao = parseValue(parts1[2], "Pressão");
                    vento = parseValue(parts1[3], "Vento");
                    break;

                case 2: // formato: "[data] X°C | Y% | Z hPa | W m/s"
                    try {
                        // Remove timestamp entre colchetes, ex: [2025-07-16 14:28:30]
                        String content = log.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
                        String[] parts2 = content.split("\\|");

                        temperatura = Double.parseDouble(parts2[0].replaceAll("[^\\d.,]+", "").replace(",", "."));
                        umidade = Double.parseDouble(parts2[1].replaceAll("[^\\d.,]+", "").replace(",", "."));
                        pressao = Double.parseDouble(parts2[2].replaceAll("[^\\d.,]+", "").replace(",", "."));
                        vento = Double.parseDouble(parts2[3].replaceAll("[^\\d.,]+", "").replace(",", "."));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Erro ao interpretar log do tipo 2: " + log, e);
                    }
                    break;

                case 3: // "[data] Temperatura: X°C ; Umidade: Y% ; Pressão: Z hPa ; Vento: W m/s"
                    String content3 = log.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
                    String[] parts3 = content3.split(";");
                    temperatura = parseValue(parts3[0], "Temperatura");
                    umidade = parseValue(parts3[1], "Umidade");
                    pressao = parseValue(parts3[2], "Pressão");
                    vento = parseValue(parts3[3], "Vento");
                    break;

                case 4:
                    try {
                        String fixedLog = log.replaceAll("(\\d+),(\\d+)", "$1.$2"); // 16,1 -> 16.1
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(fixedLog);
                        temperatura = jsonNode.get("temperatura").asDouble();
                        umidade = jsonNode.get("umidade").asDouble();
                        pressao = jsonNode.get("pressao").asDouble();
                        vento = jsonNode.get("vento").asDouble();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Erro ao interpretar log JSON: " + log, e);
                    }
                    break;

                case 5: // CSV: X,Y,Z,W com vírgulas decimais (ex: 27,4,42,3,983,4,3,2)
                    String[] parts5 = log.split(",");
                    if (parts5.length != 8) {
                        throw new IllegalArgumentException("CSV mal formatado: " + log);
                    }

                    temperatura = Double.parseDouble(parts5[0] + "." + parts5[1]);
                    umidade     = Double.parseDouble(parts5[2] + "." + parts5[3]);
                    pressao     = Double.parseDouble(parts5[4] + "." + parts5[5]);
                    vento       = Double.parseDouble(parts5[6] + "." + parts5[7]);
                    break;
            }

            return String.format("[%s] Temp: %.1f°C | Umid: %.1f%% | Press: %.1f hPa | Vento: %.1f m/s",
                    timestamp, temperatura, umidade, pressao, vento);

        } catch (Exception e) {
            return String.format("[%s] Erro ao normalizar log: %s", timestamp, e.getMessage());
        }
    }

    public static int detectFormat(String log) {
        log = log.trim();

        if (log.startsWith("{") && log.endsWith("}")) {
            return 4; // JSON
        } else if (log.contains(";")) {
            return 3; // Semicolon-separated
        } else if (log.matches("^\\[.*\\]\\s*Temperatura:.*\\|.*")) {
            return 1; // Formato com labels
        } else if (log.matches("^\\[.*\\]\\s*\\d+.*\\|.*")) {
            return 2; // Formato sem labels
        } else if (log.matches("^\\d+(,\\d+){7}$")) {
            return 5; // CSV com vírgula decimal (8 partes)
        } else {
            throw new IllegalArgumentException("Formato de log desconhecido: " + log);
        }
    }

    private static double parseValue(String input, String label) {
        String[] split = input.split(":");
        if (split.length < 2) return 0.0;
        String numeric = split[1].replaceAll("[^\\d.,]+", "").replace(",", ".");
        return Double.parseDouble(numeric);
    }



}
