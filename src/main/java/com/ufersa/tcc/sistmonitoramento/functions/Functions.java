package com.ufersa.tcc.sistmonitoramento.functions;

import com.ufersa.tcc.sistmonitoramento.Env;

import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Functions {

    private static Instant executionStart;

    private static final Random random = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getIP(InetAddress address) {
        byte[] ip = address.getAddress();
        StringBuilder ipString = new StringBuilder();

        for (int i = 0; i < ip.length; i++) {
            ipString.append(ip[i] & 0xFF);
            if (i < ip.length - 1) {
                ipString.append(".");
            }
        }

        return ipString.toString();
    }

    public static void getFileInfo() {
        String today = LocalDateTime.now().format(date);
        String parts[] = Env.logsDefaultFilePath.split("\\.");
        String path = parts[0] + "-" + today + "." + parts[1];

        File file = new File(path);

        if (file.exists()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("📄 Nome: " + file.getName());
            System.out.println("📁 Caminho: " + file.getAbsolutePath());
            System.out.println("📏 Tamanho: " + file.length() + " bytes");
            System.out.println("📅 Última modificação: " + sdf.format(file.lastModified()));
        } else {
            System.out.println("Arquivo não encontrado.");
        }
    }

    public static void initExecutionTimer() {
        executionStart = Instant.now();
    }

    public static String getElapsedTime() {
        Duration elapsed = Duration.between(executionStart, Instant.now());
        long hours = elapsed.toHours();
        long minutes = elapsed.toMinutes() % 60;
        long seconds = elapsed.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static boolean turnOffProxy() {
        Duration elapsed = Duration.between(executionStart, Instant.now());
        Duration target = Duration.ofHours(2).plusMinutes(2);

        return elapsed.compareTo(target) >= 0;
    }

}
