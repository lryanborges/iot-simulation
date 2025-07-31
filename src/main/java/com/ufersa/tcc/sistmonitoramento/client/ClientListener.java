package com.ufersa.tcc.sistmonitoramento.client;

import com.ufersa.tcc.sistmonitoramento.datagram.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener extends Thread {
    private ServerSocket serverSocket;

    public ClientListener(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        System.out.println("🛡️ ClientReceiver aguardando conexões na porta " + serverSocket.getLocalPort());
        while (true) {
            try (Socket socket = serverSocket.accept();
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

                Message<String> msg = (Message<String>) input.readObject();
                System.out.println("📥 Log recebido: " + msg.getMessage());

            } catch (Exception e) {
                System.err.println("❌ Erro no ClientListener: " + e.getMessage());
            }
        }
    }

}
