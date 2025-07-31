package com.ufersa.tcc.sistmonitoramento.server;

import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

public class ProxyListener extends Thread {
    private final ServerSocket serverSocket;

    private final int FILTER_PORT = 20002;

    public ProxyListener(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        System.out.println("📡 Aguardando conexões do Storage...");

        while (true) {
            try (Socket storageSocket = serverSocket.accept()) {
                System.out.println("🔗 Conexão recebida do Storage");

                // Conectar no cliente (proxy reverso -> cliente)
                try (Socket filtroSocket = new Socket(InetAddress.getLocalHost(), FILTER_PORT);
                     ObjectOutputStream filtroOut = new ObjectOutputStream(filtroSocket.getOutputStream());
                     BufferedReader in = new BufferedReader(new InputStreamReader(storageSocket.getInputStream()), 16384)) {

                    String linha;
                    while ((linha = in.readLine()) != null) {
                        System.out.println("📥 Recebido do Storage: " + linha);

                        Message<String> msgParaFiltro = new Message<>();
                        msgParaFiltro.setMessage(linha);
                        msgParaFiltro.setSourceIp(Env.localhost);
                        msgParaFiltro.setSourcePort(storageSocket.getPort());
                        msgParaFiltro.setDestinationIp(Env.localhost);
                        msgParaFiltro.setDestinationPort(FILTER_PORT); // talvez mudar aqui

                        filtroOut.writeObject(msgParaFiltro);
                        filtroOut.flush();
                    }

                    System.out.println("⚠️ Conexão do Storage encerrada.");
                } catch (IOException e) {
                    System.err.println("❌ Erro ao conectar/enviar para o cliente: " + e.getMessage());
                }

            } catch (IOException e) {
                System.err.println("❌ Erro ao aceitar conexão do Storage: " + e.getMessage());
            }
        }
    }
}
