package com.ufersa.tcc.sistmonitoramento.server.microservices;

import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;
import com.ufersa.tcc.sistmonitoramento.server.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Storage implements Runnable {

    boolean connexion = true;

    ServerSocket serverSocket;
    ObjectInputStream input;
    private static final int firstIotPort = 6001;
    private static final int lastIotPort = 6100;

    private final Logger logger = new Logger(Env.logsDefaultFilePath);

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(9002);
            Socket connectedSocket = serverSocket.accept();
            input = new ObjectInputStream(connectedSocket.getInputStream());

            while (connexion) {
                Message<String> receivedMsg = (Message<String>) input.readObject();
                String msg = receivedMsg.getMessage();

                System.out.println("Recebida: " + msg);
                int sourcePort = receivedMsg.getSourcePort();

                if(sourcePort >= firstIotPort && sourcePort <= lastIotPort) { // se está no intervalo de dispositivos iot
                    logger.info(receivedMsg.getMessage());
                } else {
                    List<String> response = findLogsByDate(msg);
                    sendResponse(response);
                }

                if(msg.equals("disconnect")) {
                    connexion = false;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                input.close();
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private List<String> findLogsByDate(String mensagem) {
        List<String> resultado = new ArrayList<>();
        Pattern padraoData = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
        Matcher matcher = padraoData.matcher(mensagem);

        while (matcher.find()) {
            String dataStr = matcher.group();
            try {
                LocalDate data = LocalDate.parse(dataStr);
                String nomeArquivo = String.format("storage-%s.log", data);
                Path caminhoLog = Paths.get("logs", nomeArquivo); // ajuste conforme o seu caminho real

                if (Files.exists(caminhoLog)) {
                    resultado.add("📅 Logs de " + dataStr + ":");
                    resultado.addAll(Files.readAllLines(caminhoLog));
                } else {
                    resultado.add("⚠️ Nenhum log encontrado para " + dataStr);
                }
            } catch (Exception e) {
                resultado.add("❌ Erro ao processar data " + dataStr + ": " + e.getMessage());
            }
        }

        if (resultado.isEmpty()) {
            resultado.add("⚠️ Nenhuma data reconhecida na mensagem.");
        }

        return resultado;
    }

    private void sendResponse(List<String> linhas) {
        try (Socket socket = new Socket(Env.proxyHost, 10000);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 16384)) {

            for (String linha : linhas) {
                out.write(linha);
                out.newLine(); // adiciona quebra de linha
            }

            out.flush(); // envia tudo que estiver no buffer

        } catch (IOException e) {
            System.err.println("❌ Falha ao enviar resposta ao proxy: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        try {
            new Thread(new Storage()).start();

        } catch (Exception e) {
            System.out.println("Erro no main:");
            e.printStackTrace();
        }

    }

}
