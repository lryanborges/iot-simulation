package com.ufersa.tcc.sistmonitoramento.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private String logFilePath;
    private final String originalFilePath;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Logger(String logFilePath) {
        this.logFilePath = logFilePath;
        this.originalFilePath = logFilePath;
    }

    private synchronized void log(String level, String message) {
        String time = LocalDateTime.now().format(dtf);
        String logLine = String.format("%s [%s] %s", time, level, message);

        String today = LocalDateTime.now().format(date);
        String parts[] = originalFilePath.split("\\.");
        String newPath = parts[0] + "-" + today + "." + parts[1];
        changePath(newPath);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath, true))) {
            bw.write(logLine);
            bw.newLine();
        } catch (IOException e) {
            // Se quiser, pode imprimir no console se falhar
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void changePath(String newPath) {
        this.logFilePath = newPath;
    }

    public String getCurrentFilePath(){
        return this.logFilePath;
    }

}
